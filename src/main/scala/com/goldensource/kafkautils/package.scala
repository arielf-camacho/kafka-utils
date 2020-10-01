package com.goldensource

import akka.kafka.ConsumerMessage.CommittableMessage
import com.typesafe.config.Config

import scala.util.{Failure, Success, Try}

package object kafkautils {

  /**
    * Aliases the type of object received when pre-processing a message from a Kafka topic.
    */
  type StreamMessage = CommittableMessage[String, Array[Byte]]

  /**
    * Allows to safely obtain a setting from a configuration file and put a default value if for some reason a failure
    * preventing the setting value from being obtained.
    *
    * @param getter A function that given the [[Config]] object it returns the wanted setting value.
    * @param default Default value in case the setting does not exist or an error prevented its initialization.
    * @param configuration The action [[Config]] object from which to extract the settings values.
    * @tparam T Type of the desired value.
    * @return The value of type [[T]] that was gotten from the setting or a default value instead.
    */
  def safeGet[T](getter: Config => T, default: T)(implicit configuration: Config): T =
    Try(getter(configuration)) match {
      case Failure(_)     => default
      case Success(value) => value
    }
}
