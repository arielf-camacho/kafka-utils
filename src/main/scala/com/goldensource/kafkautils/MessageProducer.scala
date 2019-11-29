package com.goldensource.kafkautils

import java.util.Properties

import akka.Done
import org.apache.kafka.clients.producer._
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

import scala.concurrent.{Future, Promise}

/**
  * Client that is to be used when desired to send messages through Apache Kafka.
  *
  * @param configuration    A [[MessageProducerConfiguration]] instance to
  *                         instantiate the connection to kafka.
  */
class MessageProducer(configuration: MessageProducerConfiguration) {

  private[kafkautils] val producer = {
    val props               = new Properties
    val stringSerializer    = classOf[StringSerializer].getCanonicalName
    val byteArraySerializer = classOf[ByteArraySerializer].getCanonicalName

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, configuration.bootstrapServers)
    props.put(ProducerConfig.CLIENT_ID_CONFIG, Unique.cleanUniqueId)
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, stringSerializer)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, byteArraySerializer)

    new KafkaProducer[String, Array[Byte]](props)
  }

  /**
    * Send a message to Apache Kafka asynchronously.
    *
    * @param message  An object to serialize and send to Apache Kafka.
    * @param topic    The Apache Kafka's topic to send the message.
    * @param maybeKey An optional key to use to keep message order in Apache Kafka.
    * @param convertToBytes A function to convert the message to a bytes array.
    * @tparam T The message type.
    * @note The message must be serializable to json.
    * @return A [[scala.concurrent.Future]] to indicate when the action to send the message has ended.
    */
  def send[T](
      message: T,
      topic: String,
      maybeKey: Option[String] = None
    )(implicit convertToBytes: T => Array[Byte]
    ): Future[Done] = {
    val serializedMessage = convertToBytes(message)
    val record            = new ProducerRecord[String, Array[Byte]](topic, maybeKey.orNull, serializedMessage)
    val promise           = Promise[Done]

    // noinspection ConvertExpressionToSAM
    producer.send(
      record,
      new Callback {
        override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit =
          Option(exception).map(promise.failure).getOrElse(promise.success(Done))
      }
    )

    promise.future
  }

  /**
    * Closes the connection to the message broker.
    */
  def close(): Unit = producer.close()
}
