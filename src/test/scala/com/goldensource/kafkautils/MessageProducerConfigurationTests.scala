package com.goldensource.kafkautils

import java.io.File

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.util.Random

class MessageProducerConfigurationTests extends FlatSpec with Matchers with BeforeAndAfterAll {
  "apply method" should "return a properly initialized MessageSenderConfiguration instance when provided a " +
    "specific configuration" in {
    // given
    val bootstrapServers = ConfigValueFactory.fromAnyRef(Random.nextString(20))
    val configuration    = ConfigFactory.empty.withValue("message-broker.producer.bootstrap-servers", bootstrapServers)

    // when
    val subject = MessageProducerConfiguration(configuration)

    // then
    subject.bootstrapServers shouldBe bootstrapServers.unwrapped
  }

  it should "return a properly initialized MessageSenderConfiguration instance when using the " +
    "application configuration" in {
    // when
    val subject = MessageProducerConfiguration()

    // then
    subject.bootstrapServers shouldBe "localhost:9092" // scalastyle:ignore
  }

  it should "load default values for empty application configuration files" in {
    // given
    val configuration = ConfigFactory.parseFile(new File("./src/test/resources/empty-application.conf"))

    // when
    val subject = MessageProducerConfiguration(configuration)

    // then
    subject.bootstrapServers shouldBe "localhost:9092"
  }
}
