package com.goldensource.kafkautils

import akka.kafka.testkit.scaladsl.{EmbeddedKafkaLike, ScalatestKafkaSpec}
import com.typesafe.config.ConfigFactory
import net.manub.embeddedkafka.EmbeddedKafka
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.scalatest._
import org.scalatest.concurrent.{Eventually, IntegrationPatience, ScalaFutures}
import org.scalatest.time.{Milliseconds, Seconds, Span}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.Random

class MessageProducerTests
    extends ScalatestKafkaSpec(9092)
    with EmbeddedKafkaLike
    with FlatSpecLike
    with Matchers
    with Eventually
    with IntegrationPatience
    with ScalaFutures
    with BeforeAndAfterAll {

  import MessageProducerTests._

  private var subject: MessageProducer                                   = _
  private val topic: String                                              = s"topic-${Random.nextLong()}"
  implicit private val deserializer: ByteArrayDeserializer               = new ByteArrayDeserializer
  implicit private val convertMessageToBytes: TestMessage => Array[Byte] = _.message.toString.getBytes()
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(5, Seconds)), interval = scaled(Span(500, Milliseconds)))

  private def createSubject(): Unit = {
    val messageProducerConfiguration = MessageProducerConfiguration(ConfigFactory.defaultApplication().resolve())

    subject = new MessageProducer(messageProducerConfiguration)
  }

  private def newMessage: TestMessage =
    TestMessage(message = Random.nextString(20))

  it should "send reliably all messages to Kafka, in any order because is not using key" in {
    // given
    createSubject()
    val messages = (1 to Random.nextInt(5)).map(_ => newMessage)

    // when
    val messageDeliveries = Future.sequence(messages.map(subject.send(_, topic)))

    // then
    whenReady(messageDeliveries) { _ =>
      val receivedMessages = EmbeddedKafka
        .consumeNumberMessagesFrom[Array[Byte]](topic, messages.length, autoCommit = true)
        .map(bytes => TestMessage(message = new String(bytes)))

      receivedMessages should contain allElementsOf messages
    }
  }

  it should "send reliably all messages to Kafka, maintaining the order, using key" in {
    // given
    createSubject()
    val messages = (1 to Random.nextInt(5)).map(_ => newMessage)

    // when
    messages foreach (message => Await.ready(subject.send(message, topic, Some("1")), 10 seconds))

    // then
    val receivedMessages = EmbeddedKafka
      .consumeNumberMessagesFrom(topic, messages.length, autoCommit = true)
      .map(bytes => TestMessage(message = new String(bytes)))

    receivedMessages shouldBe messages
  }

  override protected def afterAll(): Unit = {
    subject.close()
    super.afterAll()
  }
}

object MessageProducerTests {
  case class TestMessage(message: String)
}
