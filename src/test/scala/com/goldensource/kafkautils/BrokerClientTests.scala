package com.goldensource.kafkautils

import org.scalatest.{Matchers, WordSpec}

import scala.util.Random

class BrokerClientTests extends WordSpec with Matchers {
  import BrokerClientTests._

  private val clientSection = Random.nextString(20)

  "setting method" when {
    "section is defined" should {
      "return the correct construction of a specific broker setting path including the section" in {
        // given
        val setting = Random.nextString(20)
        val subject = new BrokerClientStub(Some(clientSection))

        // when
        val formedSetting = subject.getSetting(setting)

        // then
        formedSetting shouldBe s"message-broker.$clientSection.$setting"
      }
    }

    "section is undefined" should {
      "return the correct construction of a specific broker setting path without section" in {
        // given
        val setting = Random.nextString(20)
        val subject = new BrokerClientStub(None)

        // when
        val formedSetting = subject.getSetting(setting)

        // then
        formedSetting shouldBe s"message-broker.$setting"
      }
    }
  }
}

object BrokerClientTests {

  class BrokerClientStub(clientSection: Option[String]) extends BrokerClient {
    override protected val CLIENT_SECTION: Option[String] = clientSection

    override def getSetting(setting: String): String = super.getSetting(setting)
  }
}
