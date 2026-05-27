/*
 * Copyright 2026 HM Revenue & Customs
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
import models.api._
import org.mockito.Mockito._
import play.api.Configuration
import play.api.libs.json._

class BankAccountSpec extends VatRegSpec with JsonFormatValidation {

  val fullBankAccountModel: BankAccount = BankAccount(
    isProvided = true,
    details = Some(
      BankAccountDetails(
        name = "Test Account name",
        sortCode = "00-99-22",
        number = "12345678",
        rollNumber = None,
        status = Some(ValidStatus)
      )
    ),
    reason = None,
    bankAccountType = None
  )
  val fullBankAccountJson: JsValue = Json.parse(s"""
       |{
       |  "isProvided":true,
       |  "details":{
       |    "name":"Test Account name",
       |    "sortCode":"00-99-22",
       |    "number":"12345678",
       |    "status":${Json.toJson[BankAccountDetailsStatus](ValidStatus)}
       |  }
       |}
        """.stripMargin)

  val noDetailsBankAccountModel: BankAccount = BankAccount(isProvided = false, None, Some(BeingSetup), None)
  val noDetailsBankAccountJson: JsValue = Json.parse(s"""
       |{
       |  "isProvided":false,
       |  "reason":"BeingSetup"
       |}
        """.stripMargin)

  val fullBankAccountWithRollNumberModel: BankAccount = BankAccount(
    isProvided = true,
    details = Some(
      BankAccountDetails(
        name = "Test Account name",
        sortCode = "00-99-22",
        number = "12345678",
        rollNumber = Some("AB/121212"),
        status = Some(ValidStatus)
      )),
    reason = None,
    bankAccountType = None
  )

  val fullBankAccountWithRollNumberJson: JsValue = Json.parse(s"""
       |{
       |  "isProvided":true,
       |  "details":{
       |    "name":"Test Account name",
       |    "sortCode":"00-99-22",
       |    "number":"12345678",
       |    "rollNumber":"AB/121212",
       |    "status":${Json.toJson[BankAccountDetailsStatus](ValidStatus)}
       |  }
       |}
""".stripMargin)

  val fullBankAccountWithTypeModel: BankAccount = BankAccount(
    isProvided = true,
    details = Some(
      BankAccountDetails(
        name = "Test Account name",
        sortCode = "00-99-22",
        number = "12345678",
        rollNumber = None,
        status = Some(ValidStatus)
      )
    ),
    reason = None,
    bankAccountType = None
  )

  val fullBankAccountWithTypeJson: JsValue = Json.parse(s"""
       |{
       |  "isProvided":true,
       |  "details":{
       |    "name":"Test Account name",
       |    "sortCode":"00-99-22",
       |    "number":"12345678",
       |    "status":${Json.toJson[BankAccountDetailsStatus](ValidStatus)}
       |  },
       |  "bankAccountType":"business"
       |}
""".stripMargin)

  private val baseBankAccountDetails = BankAccountDetails(
    name = "myName",
    sortCode = "00-11-22",
    number = "12345678",
    rollNumber = None,
    status = None
  )

  "Creating a BankAccount model from Json" should {
    implicit val format: Format[BankAccount] = BankAccount.format
    "complete successfully" when {
      "from full Json" in {
        Json.fromJson[BankAccount](fullBankAccountJson) mustBe JsSuccess(fullBankAccountModel)
      }
      "from full Json without details" in {
        Json.fromJson[BankAccount](noDetailsBankAccountJson) mustBe JsSuccess(noDetailsBankAccountModel)
      }
      "from full Json with roll number" in {
        Json.fromJson[BankAccount](fullBankAccountWithRollNumberJson) mustBe JsSuccess(fullBankAccountWithRollNumberModel)
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
             |    "number":"12345678",
             |    "status":${Json.toJson[BankAccountDetailsStatus](ValidStatus)}
             |  }
             |}
           """.stripMargin
        )
        val result = Json.fromJson[BankAccount](json)
        result shouldHaveErrors (__ \ "isProvided" -> JsonValidationError("error.path.missing"))
      }

      "from Json with missing name" in {
        val json = Json.parse(s"""
             |{
             |  "isProvided":true,
             |  "details":{
             |    "sortCode":"00-99-22",
             |    "number":"12345678",
             |    "status":${Json.toJson[BankAccountDetailsStatus](ValidStatus)}
             |  }
             |}
        """.stripMargin)

        val result = Json.fromJson[BankAccount](json)
        result shouldHaveErrors (__ \ "details" \ "name" -> JsonValidationError("error.path.missing"))
      }

      "from Json with missing number" in {
        val json = Json.parse(s"""
             |{
             |  "isProvided":true,
             |  "details":{
             |    "name":"Test Account name",
             |    "sortCode":"00-99-22",
             |    "status":${Json.toJson[BankAccountDetailsStatus](ValidStatus)}
             |  }
             |}
        """.stripMargin)

        val result = Json.fromJson[BankAccount](json)
        result shouldHaveErrors (__ \ "details" \ "number" -> JsonValidationError("error.path.missing"))
      }

      "from Json with missing sort code" in {
        val json = Json.parse(s"""
             |{
             |  "isProvided":true,
             |  "details":{
             |    "name":"Test Account name",
             |    "number":"12345678",
             |    "status":${Json.toJson[BankAccountDetailsStatus](ValidStatus)}
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
      "full model without details is given" in {
        Json.toJson[BankAccount](noDetailsBankAccountModel) mustBe noDetailsBankAccountJson
      }
      "full model with roll number is given" in {
        Json.toJson[BankAccount](fullBankAccountWithRollNumberModel) mustBe fullBankAccountWithRollNumberJson
      }
    }
  }

  "The BankAccount encryption formatter" should {

    val testEncryptionKey = "YWJjZGVmZ2hpamtsbW5vcA=="

    val mockConfig = mock[Configuration]
    val crypto     = new CryptoSCRS(mockConfig)
    when(mockConfig.underlying).thenReturn(ConfigFactory.parseString(s"""
         |json {
         |  encryption.key:"$testEncryptionKey"
          }
        """.stripMargin))

    val encryptionFormat: OFormat[BankAccount] = BankAccountMongoFormat.encryptedFormat(crypto)

    val bankAccount = BankAccount(
      isProvided = true,
      Some(
        BankAccountDetails(
          name = "Test Account name",
          sortCode = "00-99-22",
          number = "12345678",
          rollNumber = None,
          status = Some(ValidStatus)
        )
      ),
      reason = None,
      bankAccountType = None
    )

    val encryptedJson = Json.parse(s"""
         |{
         | "isProvided":true,
         | "details":{
         |   "name":"Test Account name",
         |   "sortCode":"00-99-22",
         |   "number":"V3BrR3VxdHB2YzBYb1BrbHk3UGJzdz09",
         |   "status":${Json.toJson[BankAccountDetailsStatus](ValidStatus)}
         | }
         |}
      """.stripMargin)

    val bankAccountWithRollNumber = BankAccount(
      isProvided = true,
      Some(
        BankAccountDetails(
          name = "Test Account name",
          sortCode = "00-99-22",
          number = "12345678",
          rollNumber = Some("AB/121212"),
          status = Some(ValidStatus)
        )
      ),
      reason = None,
    bankAccountType = None
    )

    val encryptedJsonWithRollNumber = Json.parse(s"""
         |{
         | "isProvided":true,
         | "details":{
         |   "name":"Test Account name",
         |   "sortCode":"00-99-22",
         |   "number":"V3BrR3VxdHB2YzBYb1BrbHk3UGJzdz09",
         |   "rollNumber":"AB/121212",
         |   "status":${Json.toJson[BankAccountDetailsStatus](ValidStatus)}
         | }
         |}
  """.stripMargin)

    "write from a BankAccount case class to a correct Json representation with an encrypted account number" in {
      val writeResult = Json.toJson(bankAccount)(encryptionFormat)
      writeResult mustBe encryptedJson
    }
    "read from a Json object with an encrypted account number to a correct BankAccount case class" in {
      val readResult = Json.fromJson(encryptedJson)(encryptionFormat).get
      readResult mustBe bankAccount
    }
    "write from a BankAccount with roll number and type to correct Json with encrypted account number" in {
      val writeResult = Json.toJson(bankAccountWithRollNumber)(encryptionFormat)
      writeResult mustBe encryptedJsonWithRollNumber
    }
    "read from a Json object with roll number and type with encrypted account number to correct BankAccount" in {
      val readResult = Json.fromJson(encryptedJsonWithRollNumber)(encryptionFormat).get
      readResult mustBe bankAccountWithRollNumber
    }
  }

  "invalidBuildReason" should {
    "return 'isProvided = true but details are not defined'" when {
      "isProvided = true but details are not defined" in {
        BankAccount(isProvided = true, details = None, None, None).invalidBuildReason mustBe "isProvided = true but details are not defined"
      }
    }
    "return 'isProvided = false but reason is not defined'" when {
      "isProvided = false but reason is not defined" in {
        BankAccount(isProvided = false, None, reason = None, None).invalidBuildReason mustBe "isProvided = false but reason is not defined"
      }
    }
    "return 'failure reason unknown'" when {
      "isProvided = true and details are defined" in {
        BankAccount(isProvided = true, details = Some(baseBankAccountDetails), None, None).invalidBuildReason mustBe "failure reason unknown"
      }
      "isProvided = false and reason is defined" in {
        BankAccount(isProvided = false, None, reason = Some(BeingSetup), None).invalidBuildReason mustBe "failure reason unknown"
      }
    }
  }

  "BankAccountDetails" when {
    "reading from Json" must {
      "return a valid model" when {
        "all values including rollNumber and status are present" in {
          val fullJson = Json.parse("""
              |{
              |  "name": "myName",
              |  "sortCode": "00-11-22",
              |  "number": "12345678",
              |  "rollNumber": "myRollNumber",
              |  "status": "yes"
              |}
              |""".stripMargin)

          fullJson.as[BankAccountDetails] mustBe baseBankAccountDetails.copy(rollNumber = Some("myRollNumber"), status = Some(ValidStatus))
        }
        "all values except rollNumber and status are present" in {
          val fullJson = Json.parse("""
              |{
              |  "name": "myName",
              |  "sortCode": "00-11-22",
              |  "number": "12345678"
              |}
              |""".stripMargin)

          fullJson.as[BankAccountDetails] mustBe baseBankAccountDetails
        }
      }

      "throw an error" when {
        "a mandatory field is missing" in {
          val jsonWithMissingName = Json.parse("""
              |{
              |  "sortCode": "00-11-22",
              |  "number": "12345678",
              |  "status": "yes"
              |}
              |""".stripMargin)
          val jsonWithMissingSortCode = Json.parse("""
              |{
              |  "name": "myName",
              |  "number": "12345678",
              |  "status": "yes"
              |}
              |""".stripMargin)
          val jsonWithMissingNumber = Json.parse("""
              |{
              |  "name": "myName",
              |  "sortCode": "00-11-22",
              |  "status": "yes"
              |}
              |""".stripMargin)

          Json.fromJson[BankAccountDetails](jsonWithMissingName) shouldHaveErrors (__ \ "name"         -> JsonValidationError("error.path.missing"))
          Json.fromJson[BankAccountDetails](jsonWithMissingSortCode) shouldHaveErrors (__ \ "sortCode" -> JsonValidationError("error.path.missing"))
          Json.fromJson[BankAccountDetails](jsonWithMissingNumber) shouldHaveErrors (__ \ "number"     -> JsonValidationError("error.path.missing"))
        }
      }
    }

    "writing to Json" must {
      "convert the model successfully" in {
        val jsonWithRollNumberAndStatus = Json.parse("""
            |{
            |  "name": "myName",
            |  "sortCode": "00-11-22",
            |  "number": "12345678",
            |  "rollNumber": "myRollNumber",
            |  "status": "yes"
            |}
            |""".stripMargin)
        val jsonWithoutRollNumberOrStatus = Json.parse("""
            |{
            |  "name": "myName",
            |  "sortCode": "00-11-22",
            |  "number": "12345678"
            |}
            |""".stripMargin)

        Json.toJson(baseBankAccountDetails.copy(rollNumber = Some("myRollNumber"), status = Some(ValidStatus))) mustBe jsonWithRollNumberAndStatus
        Json.toJson(baseBankAccountDetails.copy(rollNumber = None, status = None)) mustBe jsonWithoutRollNumberOrStatus
      }
    }
  }

  "statusIsNotValid" should {
    "return true" when {
      "status is Invalid" in {
        baseBankAccountDetails.copy(status = Some(InvalidStatus)).statusIsNotValid mustBe true
      }
      "status is Indeterminate" in {
        baseBankAccountDetails.copy(status = Some(IndeterminateStatus)).statusIsNotValid mustBe true
      }
    }
    "return false" when {
      "status is Valid" in {
        baseBankAccountDetails.copy(status = Some(ValidStatus)).statusIsNotValid mustBe false
      }
    }
  }

}
