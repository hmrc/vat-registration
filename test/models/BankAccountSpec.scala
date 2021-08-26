/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package models

import auth.CryptoSCRS
import com.typesafe.config.ConfigFactory
import helpers.VatRegSpec
import models.api.{BankAccount, BankAccountDetails, BankAccountMongoFormat, BankAccountOverseasDetails, BeingSetup}
import play.api.Configuration
import play.api.libs.json._
import org.mockito.Mockito._

class BankAccountSpec extends VatRegSpec with JsonFormatValidation {

  val fullBankAccountModel: BankAccount = BankAccount(
    isProvided = true,
    details = Some(BankAccountDetails(
      name = "Test Account name",
      sortCode = "00-99-22",
      number = "12345678"
    )),
    overseasDetails = None,
    reason = None
  )
  val fullBankAccountOverseasModel: BankAccount = BankAccount(
    isProvided = true,
    details = None,
    overseasDetails = Some(BankAccountOverseasDetails(
      name = "Test Account name",
      bic = "3",
      iban = "3333333"
    )),
    reason = None
  )
  val fullBankAccountJson: JsValue = Json.parse(
    s"""
       |{
       |  "isProvided":true,
       |  "details":{
       |    "name":"Test Account name",
       |    "sortCode":"00-99-22",
       |    "number":"12345678"
       |  }
       |}
        """.stripMargin)

  val fullBankAccountOverseasJson: JsValue = Json.parse(
    s"""
       |{
       |  "isProvided":true,
       |  "overseasDetails":{
       |    "name":"Test Account name",
       |    "bic":"3",
       |    "iban":"3333333"
       |  }
       |}
        """.stripMargin)



  val noDetailsBankAccountModel: BankAccount = BankAccount(isProvided = false, None,None, Some(BeingSetup))
  val noDetailsBankAccountJson: JsValue = Json.parse(
    s"""
       |{
       |  "isProvided":false,
       |  "reason":"BeingSetup"
       |}
        """.stripMargin)


  "Creating a BankAccount model from Json" should {
    implicit val format: Format[BankAccount] = BankAccount.format
    "complete successfully" when {
      "from full Json" in {
        Json.fromJson[BankAccount](fullBankAccountJson) mustBe JsSuccess(fullBankAccountModel)
      }
      "from full overseas Json" in {
        Json.fromJson[BankAccount](fullBankAccountOverseasJson) mustBe JsSuccess(fullBankAccountOverseasModel)
      }
      "from full Json without details" in {
        Json.fromJson[BankAccount](noDetailsBankAccountJson) mustBe JsSuccess(noDetailsBankAccountModel)
      }
    }

    "fail" when {
      "from Json with missing isProvided" in {
        val json = Json.parse(
          s"""
             {
             |  "details":{
             |    "name":"Test Account name",
             |    "sortCode":"00-99-22",
             |    "number":"12345678"
             |  }
             |}
           """.stripMargin
        )
        val result = Json.fromJson[BankAccount](json)
        result shouldHaveErrors (__ \ "isProvided" -> JsonValidationError("error.path.missing"))
      }

      "from Json with missing name" in {
        val json = Json.parse(
          s"""
             |{
             |  "isProvided":true,
             |  "details":{
             |    "sortCode":"00-99-22",
             |    "number":"12345678"
             |  }
             |}
        """.stripMargin)

        val result = Json.fromJson[BankAccount](json)
        result shouldHaveErrors (__ \ "details" \ "name" -> JsonValidationError("error.path.missing"))
      }

      "from Json with missing number" in {
        val json = Json.parse(
          s"""
             |{
             |  "isProvided":true,
             |  "details":{
             |    "name":"Test Account name",
             |    "sortCode":"00-99-22"
             |  }
             |}
        """.stripMargin)

        val result = Json.fromJson[BankAccount](json)
        result shouldHaveErrors (__ \ "details" \ "number" -> JsonValidationError("error.path.missing"))
      }

      "from Json with missing sort code" in {
        val json = Json.parse(
          s"""
             |{
             |  "isProvided":true,
             |  "details":{
             |    "name":"Test Account name",
             |    "number":"12345678"
             |  }
             |}
        """.stripMargin)

        val result = Json.fromJson[BankAccount](json)
        result shouldHaveErrors (__ \ "details" \ "sortCode" -> JsonValidationError("error.path.missing"))
      }
    }
  }

  "Creating Json from a BankAccount model" should {
    "succeed" when {
      "full model is given" in {
        Json.toJson[BankAccount](fullBankAccountModel) mustBe fullBankAccountJson
      }
      "full overseas model is given" in {
        Json.toJson[BankAccount](fullBankAccountOverseasModel) mustBe fullBankAccountOverseasJson
      }
      "full model without details is given" in {
        Json.toJson[BankAccount](noDetailsBankAccountModel) mustBe noDetailsBankAccountJson
      }
    }
  }

  "The BankAccount encryption formatter" should {

    val testEncryptionKey = "YWJjZGVmZ2hpamtsbW5vcA=="

    val mockConfig = mock[Configuration]
    val crypto = new CryptoSCRS(mockConfig)
    when(mockConfig.underlying).thenReturn(ConfigFactory.parseString(
      s"""
         |json {
         |  encryption.key:"$testEncryptionKey"
          }
        """.stripMargin))

    val encryptionFormat: OFormat[BankAccount] = BankAccountMongoFormat.encryptedFormat(crypto)

    val bankAccount = BankAccount(
      isProvided = true,
      Some(BankAccountDetails(
        name = "Test Account name",
        sortCode = "00-99-22",
        number = "12345678"
      )),
      overseasDetails = None,
      reason = None
    )

    val bankAccountOverseas = BankAccount(
      isProvided = true,
      details = None,
      overseasDetails = Some(BankAccountOverseasDetails(
        name = "Test Account name",
        bic = "3",
        iban = "3333333"
      )),
      reason = None
    )

    val encryptedJson = Json.parse(
      """
        |{
        | "isProvided":true,
        | "details":{
        |   "name":"Test Account name",
        |   "sortCode":"00-99-22",
        |   "number":"V3BrR3VxdHB2YzBYb1BrbHk3UGJzdz09"
        | }
        |}
      """.stripMargin)

    val encryptedOverseasJson = Json.parse(
      """
        |{
        | "isProvided":true,
        | "overseasDetails":{
        |   "name":"Test Account name",
        |   "bic":"dDZDN0dJdzRHakhLeTVBVldDalZvdz09",
        |   "iban":"QkFYUWZhbk5CZ2FPREVZRWs3NHUwZz09"
        | }
        |}
      """.stripMargin)

    "write from a BankAccount case class to a correct Json representation with an encrypted account number" in {
      val writeResult = Json.toJson(bankAccount)(encryptionFormat)
      writeResult mustBe encryptedJson
    }
    "write from a BankAccount case class to a correct Json representation with an encrypted bic and iban" in {
      val writeResult = Json.toJson(bankAccountOverseas)(encryptionFormat)
      writeResult mustBe encryptedOverseasJson
    }
    "read from a Json object with an encrypted account number to a correct BankAccount case class" in {
      val readResult = Json.fromJson(encryptedJson)(encryptionFormat).get
      readResult mustBe bankAccount
    }
    "read from a Json object with an encrypted bic and iban to a correct BankAccount case class" in {
      val readResult = Json.fromJson(encryptedOverseasJson)(encryptionFormat).get
      readResult mustBe bankAccountOverseas
    }
  }
}
