package com.goldensource.kafkautils

import java.util.Properties

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.testkit.scaladsl.{EmbeddedKafkaLike, ScalatestKafkaSpec}
import akka.stream.ActorMaterializer
import MessageConsumerFactoryTests.MessageHandler
import com.goldensource.kafkautils.MessageConsumerFactory.ConsumeMessage
import com.goldensource.kafkautils.MessageProducerTests.TestMessage
import com.typesafe.config.{Config, ConfigFactory}
import net.manub.embeddedkafka.EmbeddedKafka
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Minute, Seconds, Span}
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}

import scala.concurrent.Future
import scala.util.Random

class MessageConsumerFactoryTests
    extends ScalatestKafkaSpec(9092)
    with EmbeddedKafkaLike
    with WordSpecLike
    with Matchers
    with Eventually
    with IntegrationPatience
    with ScalaFutures
    with MockitoSugar
    with BeforeAndAfter {
  private var topics: Set[String]   = _
  private val groupId: String       = "message-consumers"
  private val configuration: Config = ConfigFactory.defaultApplication().resolve()
  private val testActorSystem       = ActorSystem("TestMessageConsumerFactoryTests", configuration)
  private val testMaterializer      = ActorMaterializer()(testActorSystem)

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(1, Minute)), interval = Span(1, Seconds))

  private val producer: KafkaProducer[String, Array[Byte]] = {
    val properties = new Properties

    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getCanonicalName)
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[ByteArraySerializer].getCanonicalName)

    new KafkaProducer[String, Array[Byte]](properties)
  }

  private var messageConsumerConfiguration: MessageConsumerConfiguration = _

  private def newMessage: TestMessage = TestMessage(message = Random.nextString(20))

  before {
    topics = (1 to Random.nextInt(5)).map(_ => s"topic-${Random.nextLong()}").toSet
    messageConsumerConfiguration = {
      MessageConsumerConfiguration(topics = topics, groupId = groupId)(configuration)
    }
  }

  override protected def afterAll(): Unit = {
    testMaterializer.shutdown()
    shutdown(testActorSystem)
    super.afterAll()
  }

  "createConsumerFor" when {
    "the broker is properly working" should { // scalastyle:ignore
      "receive and process all messages" in { // scalastyle:ignore
        val handler                             = mock[MessageHandler]
        val handle: ConsumeMessage[TestMessage] = handler.handle
        when(handler.handle(any[TestMessage])).thenReturn(Future.successful(Done))

        // some messages are created
        val messages = (1 to Random.nextInt(3)).map(_ => newMessage)
        // when, sent all the messages through all the topics
        messages foreach { message =>
          val topic  = Random.shuffle(topics).head
          val record = new ProducerRecord[String, Array[Byte]](topic, message.message.toString.getBytes())
          producer.send(record)
        }

        // and there is instantiated the consumer
        val control =
          MessageConsumerFactory.createConsumerFor(messageConsumerConfiguration, handle)(testActorSystem,
                                                                                         testMaterializer,
                                                                                         convertToTestMessage)

        eventually {
          messages.foreach(verify(handler, times(1)).handle(_))
        }

        whenReady(control.get.stop.map(_ => control.get.shutdown)) { _ =>
          succeed
        }
      }
    }

    "kafka broker is failing" should {
      "reconnect and continue receiving messages" in {
        val handler                             = mock[MessageHandler]
        val handle: ConsumeMessage[TestMessage] = handler.handle
        when(handler.handle(any[TestMessage])).thenReturn(Future.successful(Done))

        // some messages are created
        val messages = (1 to Random.nextInt(3)).map(_ => newMessage)
        // when, sent all the messages through all the topics
        messages foreach { message =>
          val topic  = Random.shuffle(topics).head
          val record = new ProducerRecord[String, Array[Byte]](topic, message.message.toString.getBytes())
          producer.send(record)
        }

        val control =
          MessageConsumerFactory.createConsumerFor(messageConsumerConfiguration, handle)(testActorSystem,
                                                                                         testMaterializer,
                                                                                         convertToTestMessage)

        eventually {
          messages.foreach(verify(handler, times(1)).handle(_))
        }

        EmbeddedKafka.stop()
        EmbeddedKafka.start()(embeddedKafkaConfig)

        // and then some messages are started
        val messages2 = (1 to Random.nextInt(5)).map(_ => newMessage)
        // when, sent all the messages through all the topics
        messages2 foreach { message =>
          val topic  = Random.shuffle(topics).head
          val record = new ProducerRecord[String, Array[Byte]](topic, message.message.toString.getBytes())
          producer.send(record)
        }

        eventually {
          messages2.foreach(verify(handler, times(1)).handle(_))
        }

        whenReady(control.get.stop.map(_ => control.get.shutdown)) { _ =>
          succeed
        }
      }
    }
  }

  implicit private def convertToTestMessage(bytes: Array[Byte]): TestMessage = TestMessage(message = new String(bytes))
}

object MessageConsumerFactoryTests {

  class MessageHandler {
    def handle(message: TestMessage): Future[Done] = Future.successful(Done)
  }
}
