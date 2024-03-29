/*
 * Copyright 2023 HM Revenue & Customs
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

package services

import auth.CryptoSCRS
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.BankAccountMongoFormat
import models.registration.{BankAccountSectionId, BusinessSectionId}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.InternalServerException

class CipherServiceSpec extends VatRegSpec with VatRegistrationFixture {

  val crypto = app.injector.instanceOf[CryptoSCRS]

  implicit val request: Request[_] = FakeRequest()

  object Service extends CipherService(crypto)

  "conditionallyEncrypt" must {

    "return encrypted BankAccount" in {
      val encryptedBankAccount = Json.toJson(testBankAccount)(BankAccountMongoFormat.encryptedFormat(crypto))

      val result = Service.conditionallyEncrypt(BankAccountSectionId, Json.toJson(testBankAccount))

      result mustBe encryptedBankAccount
    }

    "return section without encryption if the section is not BankAccount" in {
      val nonEncryptedBusiness = Json.toJson(testBusiness)

      val result = Service.conditionallyEncrypt(BusinessSectionId, nonEncryptedBusiness)

      result mustBe nonEncryptedBusiness
    }

    "throw an exception when BankAccount format is not valid" in {
      intercept[InternalServerException] {
        val invalidBankAccount: JsValue = Json.obj("invalid" -> "format")

        Service.conditionallyEncrypt(BankAccountSectionId, invalidBankAccount)
      }
    }
  }

  "conditionallyDecrypt" must {

    "return decrypted BankAccount" in {
      val encryptedBankAccount = Json.toJson(testBankAccount)(BankAccountMongoFormat.encryptedFormat(crypto))

      val result = Service.conditionallyDecrypt(BankAccountSectionId, encryptedBankAccount)

      result mustBe Json.toJson(testBankAccount)
    }

    "return section without decryption if the section is not BankAccount" in {
      val nonEncryptedBusiness = Json.toJson(testBusiness)

      val result = Service.conditionallyDecrypt(BusinessSectionId, nonEncryptedBusiness)

      result mustBe nonEncryptedBusiness
    }

    "throw an exception when BankAccount format is not valid" in {
      intercept[InternalServerException] {
        val invalidBankAccount: JsValue = Json.obj("invalid" -> "format")

        Service.conditionallyDecrypt(BankAccountSectionId, invalidBankAccount)
      }
    }
  }
}
