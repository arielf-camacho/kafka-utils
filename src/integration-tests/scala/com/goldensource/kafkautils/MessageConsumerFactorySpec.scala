package com.goldensource.kafkautils

import akka.Done
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.goldensource.kafkautils.MessageConsumerFactory.ConsumeMessage
import com.goldensource.kafkautils.MessageConsumerFactorySpec.TestMessage
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, MustMatchers, WordSpec}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class MessageConsumerFactorySpec
    extends WordSpec
    with MustMatchers
    with Containers
    with BeforeAndAfterAll
    with BeforeAndAfter
    with Eventually
    with ScalaFutures
    with MockitoSugar {
  private var topics: Set[String]   = _
  private val groupId: String       = "message-consumers"
  private val configuration: Config = ConfigFactory.defaultApplication().resolve()

  // noinspection TypeAnnotation
  implicit private val testActorSystem = ActorSystem("TestMessageConsumerFactoryTests", configuration)
  // noinspection TypeAnnotation
  implicit private val testMaterializer = ActorMaterializer()(testActorSystem)
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(30, Seconds)),
    interval = Span(2, Seconds)
  )
  implicit private def preProcess(commitableMessage: StreamMessage): TestMessage =
    TestMessage(message = new String(commitableMessage.record.value))
  implicit private def convertToBytes(message: TestMessage): Array[Byte] = message.message.getBytes

  private var messageConsumerConfiguration: MessageConsumerConfiguration = _
  private var messageProducerConfiguration: MessageProducerConfiguration = _

  private def newProducer: MessageProducer = new MessageProducer(messageProducerConfiguration)
  private def newMessage: TestMessage      = TestMessage(message = Random.alphanumeric.take(20).mkString)

  import testActorSystem.dispatcher

  before {
    topics = (1 to 3).map(_ => s"topic-${Math.abs(Random.nextLong())}").toSet
    val bootstrapServers = ConfigValueFactory.fromAnyRef(kafkaContainer.bootstrapServers)
    val updatedConfiguration = configuration
      .withValue("message-broker.producer.bootstrap-servers", bootstrapServers)
      .withValue("message-broker.consumer.bootstrap-servers", bootstrapServers)

    messageProducerConfiguration = MessageProducerConfiguration(updatedConfiguration)
    messageConsumerConfiguration =
      MessageConsumerConfiguration(topics = topics, groupId = groupId)(updatedConfiguration)
  }

  override protected def beforeAll(): Unit = container.start()

  override protected def afterAll(): Unit = {
    container.stop()
    testMaterializer.shutdown()
    Await.ready(testActorSystem.terminate(), 20 seconds)
  }

  "createConsumerFor" when {
    "the broker is properly working" should {
      "receive and process all messages" in {
        val producer = newProducer

        val consumedMessages = ListBuffer[TestMessage]()
        val handle: ConsumeMessage[TestMessage] = message =>
          Future {
            consumedMessages += message
            Done
        }

        // some messages are created and sent through all the topics randomly
        val messages = (1 to 3)
          .map(_ => newMessage)
          .map { message =>
            val topic = Random.shuffle(topics).head
            producer.send(message, topic)
            message
          }

        // and there is instantiated the consumer
        val control = MessageConsumerFactory.createConsumerFor(messageConsumerConfiguration, handle)

        eventually {
          consumedMessages must contain allElementsOf messages
        }

        eventually {
          control.get.drainAndShutdown(Future.successful(Done))
          producer.close()
        }
      }
    }
  }
}

object MessageConsumerFactorySpec {
  case class TestMessage(message: String)

  class MessageHandler {
    // noinspection ScalaUnusedSymbol
    def handle(notUsed: TestMessage): Future[Done.type] = Future.successful(Done)
  }
}
