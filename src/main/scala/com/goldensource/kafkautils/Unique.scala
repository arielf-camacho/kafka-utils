package com.goldensource.kafkautils

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

/**
  * Trait that contains the concept of the a collision-free identifier.
  */
trait Unique {

  /**
    * Unique identifier that differentiates the current instance of others.
    *
    * @return
    */
  def id: String
}

/**
  * Contains the method that is used to generate unique ids.
  */
object Unique {

  /**
    * Gets a unique identifier.
    *
    * @return A String representing a unique identifier.
    */
  def uniqueId: String = s"${UUID.randomUUID}|${LocalDateTime.now.toEpochSecond(ZoneOffset.UTC)}"

  /**
    * Gets a unique identifier with only alphanumeric characters.
    *
    * @return A String representing a unique identifier.
    */
  def cleanUniqueId: String = s"${UUID.randomUUID}${LocalDateTime.now.toEpochSecond(ZoneOffset.UTC)}".replace("-", "")
}
