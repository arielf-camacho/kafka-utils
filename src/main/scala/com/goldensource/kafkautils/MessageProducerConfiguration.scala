package com.goldensource.kafkautils

import com.typesafe.config.{Config, ConfigFactory}

/**
  * Represents the configuration of the Message Sender actor.
  *
  * @param bootstrapServers Comma separated Set of strings being each of the address of a Kafka broker.
  */
class MessageProducerConfiguration(val bootstrapServers: String)

/**
  * Contains factory methods to easily build instances of the
  * [[com.goldensource.kafkautils.MessageProducer]]  actor using default or provided configuration.
  */
object MessageProducerConfiguration extends BrokerClient {

  /**
    * Constructs a new instance of [[com.goldensource.kafkautils.MessageProducerConfiguration]]
    * class using the  provided client identification.
    *
    * @param configuration Configuration from which to extract the values to initialize properly the
    *                      [[com.goldensource.kafkautils.MessageProducerConfiguration]] to return.
    * @return A new instance of [[com.goldensource.kafkautils.MessageProducerConfiguration]] with
    *         the provided client identifier and also the other parameters extracted from the given configuration.
    */
  def apply(configuration: Config = ConfigFactory.load): MessageProducerConfiguration =
    new MessageProducerConfiguration(
      bootstrapServers = safeGet(_.getString(getSetting("bootstrap-servers")), "localhost:9092")(configuration)
    )

  override protected val CLIENT_SECTION: Option[String] = Some("producer")
}
