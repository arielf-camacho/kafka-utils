package com.goldensource.kafkautils

import java.time.{Duration => JavaDuration}
import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._

/**
  * Represents the configuration of the Message Receiver actor.
  *
  * @param minBackoff           Minimum time to wait for the first restart of the internal Kafka consumer.
  * @param maxBackoff           Minimum time to wait for the subsequent restarts of the internal Kafka consumer.
  * @param topics               Set of topics names to get messages from.
  * @param bootstrapServers     String being the comma separated list of each Kafka broker addresses.
  * @param groupId              The name of the Group the message receiver will be associated to.
  * @param pollInterval         time to wait during a message polling.
  * @param pollTimeout          time to wait during to declare a poll as hung.
  * @param messageCommitTimeout The time to wait during an message processed acknowledgement.
  * @param sessionTimeout       The timeout to consider a consumer as dead, and trigger a re-balance by the consumer
  *                             group coordinator in milliseconds.
  * @param parallelism          States how many messages will be processed in parallel from the batch brought from
  *                             Kafka in a single poll.
  * @param dispatcher           The name of the dispatcher is which will run the actors that are involved in the
  *                             messages consumption.
  * @param configuration        The configuration from which this settings were extracted, just in case there are
  *                             needed other settings not contained in this class.
  */
class MessageConsumerConfiguration(
    val minBackoff: FiniteDuration,
    val maxBackoff: FiniteDuration,
    val topics: Set[String],
    val bootstrapServers: String,
    val groupId: String,
    val pollInterval: JavaDuration,
    val pollTimeout: JavaDuration,
    val messageCommitTimeout: JavaDuration,
    val sessionTimeout: Int = 3000,
    val parallelism: Int = 1,
    val commitBatchSize: Long = 1,
    val dispatcher: String = "akka.actor.default-dispatcher", // scalastyle:ignore
    val configuration: Config)

/**
  * Contains a factory method that allows to build a
  * [[com.goldensource.kafkautils.MessageConsumerConfiguration]] instance from a topics set and
  * with a group id to identifier the consumers.
  */
object MessageConsumerConfiguration extends BrokerClient {

  /**
    * Creates a new instance of [[com.goldensource.kafkautils.MessageConsumerConfiguration]] given
    * the topics and a group identifier.
    *
    * @param topics        Set with the topics to poll messages from.
    * @param groupId       Identifier of the group to put the consumer in.
    * @param configuration Remaining configuration settings just in case more is needed from it.
    * @return A new instance of [[com.goldensource.kafkautils.MessageConsumerConfiguration]] with
    *         the explicitly provided values and with the other that were not automatically getting their values from
    *         predefined regions of the provided configuration.
    */
  def apply(
      topics: Set[String],
      groupId: String
    )(implicit configuration: Config = ConfigFactory.load.resolve
    ): MessageConsumerConfiguration =
    new MessageConsumerConfiguration(
      topics = topics,
      groupId = groupId,
      minBackoff = safeGet(_.getDuration(getSetting("min-backoff")), 1 second),
      maxBackoff = safeGet(_.getDuration(getSetting("max-backoff")), 8 seconds),
      bootstrapServers = safeGet(_.getString(getSetting("bootstrap-servers")), "localhost:9092"),
      pollInterval = safeGet(_.getDuration(getSetting("poll-interval")), 250 milliseconds),
      pollTimeout = safeGet(_.getDuration(getSetting("poll-timeout")), 300 milliseconds),
      messageCommitTimeout = safeGet(_.getDuration(getSetting("message-commit-timeout")), 1 second),
      sessionTimeout =
        safeGet(_.getDuration(getSetting("session-timeout-ms")), java.time.Duration.ofMillis(10000)).toMillis.toInt,
      parallelism = safeGet(_.getInt(getSetting("parallelism")), 5),
      commitBatchSize = safeGet(_.getLong(getSetting("commit-batch-size")), 1),
      dispatcher = safeGet(_.getString(getSetting("dispatcher")), "akka.actor.default-dispatcher"),
      configuration = configuration
    )

  override protected val CLIENT_SECTION: Option[String] = Some("consumer")

  implicit def convertToFiniteDuration(duration: java.time.Duration): FiniteDuration =
    FiniteDuration(duration.toMillis, TimeUnit.MILLISECONDS)

  implicit def convertToDuration(duration: FiniteDuration): java.time.Duration =
    java.time.Duration.ofNanos(duration.toNanos)
}
