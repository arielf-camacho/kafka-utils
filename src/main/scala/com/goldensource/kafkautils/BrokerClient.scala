package com.goldensource.kafkautils

/**
  * Serves as base trait for the Kafka client configuration creator objects.
  */
trait BrokerClient {

  /**
    * States the section in the application configuration where the settings for the Kafka clients should be placed.
    */
  protected val BROKER_SECTION: String = "message-broker"

  /**
    * Represents the specific section where are placed the settings for the kafka client being initialized.
    */
  protected val CLIENT_SECTION: Option[String] = None

  /**
    * Helper method that allows to easily build a setting name using the Broker's section, inside an specific client
    * section and named as stated.
    *
    * @param setting The path of the setting to get, relative to the sections specified in the class.
    * @return A name for a setting, example: "Broker section"."client section"."setting".
    */
  protected def getSetting(setting: String): String =
    s"$BROKER_SECTION.${CLIENT_SECTION.map(clientSection => s"$clientSection.").getOrElse("")}$setting"
}
