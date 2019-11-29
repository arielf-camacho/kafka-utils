package com.goldensource.kafkautils

import java.util.concurrent.atomic.AtomicReference

import akka.actor.ActorSystem
import akka.kafka._
import akka.kafka.scaladsl.Consumer.{Control, DrainingControl, NoopControl}
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.stream.Materializer
import akka.stream.scaladsl.{Keep, RestartSource, Sink, Source}
import akka.{Done, NotUsed}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.collection.immutable
import scala.concurrent.Future

/**
  * Trait that abstracts the actual factory used to create Apache Kafka messages consumption streams.
  */
trait MessageConsumerFactory {
  import MessageConsumerFactory._

  /**
    * Creates a Consumption stream from a kafka topic. It uses the configuration and all the provided parameters to get
    * messages as byte arrays from a kafka topic, converts them using a specified conversion method and then passes it
    * to another method that is given for further processing.
    *
    * @param configuration Configuration that allows to setup the stream. There is included where the brokers are, the
    *                      topics to consume and timeouts.
    * @param consumeMessage Function the client of the current method provides so that the received message can be
    *                       further processed.
    * @param system An [[akka.actor.ActorSystem]] to host the actors created to support the stream.
    * @param materializer An [[akka.stream.Materializer]] which will materialize the stream when its ready to be
    *                     started.
    * @param convertToType A function that transforms a byte array corresponding to a message to expected message type.
    * @tparam T The type that is expected the messages to have.
    * @return A [[java.util.concurrent.atomic.AtomicReference]] containing a [[akka.kafka.scaladsl.Consumer.Control]]
    *         that can be used to control when the consumption stream should be stopped.
    */
  def createConsumerFor[T](
      configuration: MessageConsumerConfiguration,
      consumeMessage: ConsumeMessage[T]
    )(implicit system: ActorSystem,
      materializer: Materializer,
      convertToType: Array[Byte] => T
    ): AtomicReference[Control]
}

/**
  * Factory class to create a kafka consuming stream.
  */
object MessageConsumerFactory extends MessageConsumerFactory {

  /**
    * Function to handle a received message object through a consumption stream.
    *
    * @tparam T The type of the received message to process.
    */
  abstract class ConsumeMessage[T] extends (T => Future[Done])

  override def createConsumerFor[T](
      configuration: MessageConsumerConfiguration,
      consumeMessage: ConsumeMessage[T]
    )(implicit system: ActorSystem,
      materializer: Materializer,
      convertToType: Array[Byte] => T
    ): AtomicReference[Control] = {
    val committerSettings = CommitterSettings(system).withMaxBatch(configuration.commitBatchSize)
    val subscription      = Subscriptions.topics(configuration.topics)
    val control           = new AtomicReference[Control](NoopControl)

    val finalSource = createRestartSource(configuration)(
      () => createConsumer[T](committerSettings, subscription, control, configuration, consumeMessage)
    )

    val f: Future[Seq[Done]] = materializeSource(finalSource)
    val drainingControl      = DrainingControl((control.get, f))
    control.set(drainingControl)

    control
  }

  private def materializeSource[T](
      finalSource: Source[Done, NotUsed]
    )(implicit materializer: Materializer
    ): Future[immutable.Seq[Done]] = finalSource.toMat(Sink.seq)(Keep.both).run()._2

  private def createRestartSource[T](configuration: MessageConsumerConfiguration) =
    RestartSource
      .onFailuresWithBackoff[T](
        minBackoff = configuration.minBackoff,
        maxBackoff = configuration.maxBackoff,
        randomFactor = 0.2
      ) _

  private def createConsumer[T](
      committerSettings: CommitterSettings,
      subscription: Subscription,
      control: AtomicReference[Control],
      configuration: MessageConsumerConfiguration,
      consumeMessage: T => Future[Done]
    )(implicit system: ActorSystem,
      convertToType: Array[Byte] => T
    ): Source[Done, Unit] = {
    val dispatcher       = system.dispatchers.lookup(configuration.dispatcher)
    val consumerSettings = createConsumerSettings(configuration, system)

    Consumer
      .committableSource(consumerSettings, subscription)
      .mapMaterializedValue(control.set)
      .mapAsync(configuration.parallelism) { message =>
        val convertedMessage = convertToType(message.record.value)
        consumeMessage(convertedMessage).map(_ => message.committableOffset)(dispatcher)
      }
      .via(Committer.flow(committerSettings.withMaxBatch(1)))
  }

  private def createConsumerSettings[T](
      configuration: MessageConsumerConfiguration,
      system: ActorSystem
    ): ConsumerSettings[String, Array[Byte]] =
    ConsumerSettings(system, new StringDeserializer, new ByteArrayDeserializer)
      .withBootstrapServers(configuration.bootstrapServers)
      .withGroupId(configuration.groupId)
      .withPollInterval(configuration.pollInterval)
      .withPollTimeout(configuration.pollTimeout)
      .withCommitTimeout(configuration.messageCommitTimeout)
      .withDispatcher(configuration.dispatcher)
      .withProperty(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, configuration.sessionTimeout.toString)
      .withProperty(ConsumerConfig.CLIENT_ID_CONFIG, Unique.cleanUniqueId)
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
}
