package com.goldensource.kafkautils

import scala.collection.JavaConverters._
import com.dimafeng.testcontainers.{ForAllTestContainer, KafkaContainer, MultipleContainers}
import org.scalatest.TestSuite

trait Containers extends ForAllTestContainer { this: TestSuite =>
  protected val kafkaContainer: KafkaContainer = KafkaContainer("5.1.2")
  kafkaContainer.container.setPortBindings(List("19092:9092").asJava)

  override val container: MultipleContainers = MultipleContainers(kafkaContainer)
}
