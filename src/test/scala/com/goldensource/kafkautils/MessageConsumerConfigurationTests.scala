package com.goldensource.kafkautils

import java.time.temporal.ChronoUnit
import java.time.{Duration => JDuration}

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.duration._
import scala.util.Random

class MessageConsumerConfigurationTests extends FlatSpec with Matchers with BeforeAndAfterAll {
  private val consumerSection = "message-broker.consumer"
  private val secondsName     = "seconds"

  "apply method" should "return a properly initialized MessageSenderConfiguration instance when provided a " +
    "specific configuration" in {
    // given
    val minBackoff           = ConfigValueFactory.fromAnyRef(s"${Random.nextInt(20)} $secondsName")
    val maxBackoff           = ConfigValueFactory.fromAnyRef(s"${Random.nextInt(20)} $secondsName")
    val pollInterval         = ConfigValueFactory.fromAnyRef(s"${Random.nextInt(20)} $secondsName")
    val pollTimeout          = ConfigValueFactory.fromAnyRef(s"${Random.nextInt(20)} $secondsName")
    val parallelism          = ConfigValueFactory.fromAnyRef(Random.nextInt())
    val messageCommitTimeout = ConfigValueFactory.fromAnyRef(s"${Random.nextInt()} $secondsName")
    val sessionTimeout       = ConfigValueFactory.fromAnyRef(s"${Random.nextInt()} $secondsName")
    val commitBatchSize      = ConfigValueFactory.fromAnyRef(Random.nextLong())
    val topics               = (1 to Random.nextInt(5)).map(_ => Random.nextString(20)).toSet
    val bootstrapServers     = ConfigValueFactory.fromAnyRef(Random.nextString(20))
    val groupId              = Random.nextString(20)
    val dispatcher           = ConfigValueFactory.fromAnyRef(Random.nextString(20))
    val configuration = ConfigFactory.empty
      .withValue(s"$consumerSection.bootstrap-servers", bootstrapServers)
      .withValue(s"$consumerSection.min-backoff", minBackoff)
      .withValue(s"$consumerSection.max-backoff", maxBackoff)
      .withValue(s"$consumerSection.poll-interval", pollInterval)
      .withValue(s"$consumerSection.poll-timeout", pollTimeout)
      .withValue(s"$consumerSection.parallelism", parallelism)
      .withValue(s"$consumerSection.message-commit-timeout", messageCommitTimeout)
      .withValue(s"$consumerSection.session-timeout-ms", sessionTimeout)
      .withValue(s"$consumerSection.commit-batch-size", commitBatchSize)
      .withValue(s"$consumerSection.dispatcher", dispatcher)

    // when
    val subject = MessageConsumerConfiguration(topics, groupId)(configuration)

    // then
    subject.minBackoff should be(Duration(minBackoff.unwrapped.toString))
    subject.maxBackoff should be(Duration(maxBackoff.unwrapped.toString))
    subject.pollInterval should be(toJDuration(pollInterval.unwrapped.toString))
    subject.pollTimeout should be(toJDuration(pollTimeout.unwrapped.toString))
    subject.parallelism should be(parallelism.unwrapped)
    subject.messageCommitTimeout should be(toJDuration(messageCommitTimeout.unwrapped.toString))
    subject.sessionTimeout should be(Duration(sessionTimeout.unwrapped.toString).toMillis.toInt)
    subject.commitBatchSize shouldBe commitBatchSize.unwrapped()
    subject.topics shouldBe topics
    subject.groupId shouldBe groupId
    subject.dispatcher shouldBe dispatcher.unwrapped
    subject.bootstrapServers shouldBe bootstrapServers.unwrapped
  }

  it should "return a properly initialized MessageSenderConfiguration instance when using the " +
    "application configuration" in {
    // given
    val topics  = (1 to Random.nextInt(5)).map(_ => Random.nextString(20)).toSet
    val groupId = Random.nextString(10)

    // when
    val subject = MessageConsumerConfiguration(topics, groupId)

    // then
    subject.bootstrapServers shouldBe "localhost:9092"
    subject.minBackoff shouldBe (1 second)
    subject.maxBackoff shouldBe (2 seconds)
    subject.pollInterval shouldBe JDuration.of(250, ChronoUnit.MILLIS)
    subject.pollTimeout shouldBe JDuration.of(300, ChronoUnit.MILLIS)
    subject.messageCommitTimeout shouldBe JDuration.of(1, ChronoUnit.SECONDS)
    subject.sessionTimeout shouldBe 10000
    subject.parallelism shouldBe 5
    subject.dispatcher shouldBe "dispatchers.consumer"
    subject.groupId shouldBe groupId
    subject.topics shouldBe topics
  }

  private def toJDuration(string: String) = JDuration.of(Duration(string).toSeconds, ChronoUnit.SECONDS)
}
