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

package services.monitoring

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.{BankAccountDetails, ValidStatus}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.InternalServerException

class BankAuditBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  val bankAuditBlockJson: JsObject = Json
    .parse("""
      |{
      |    "accountName": "Test Bank Account",
      |    "sortCode": "01-02-03",
      |    "accountNumber": "01023456"
      |}
      |""".stripMargin)
    .as[JsObject]

  val bankDetailsNotProvidedBlockJson: JsObject = Json
    .parse("""
      |{
      |     "reasonBankAccNotProvided": "BeingSetup"
      |}
      |""".stripMargin)
    .as[JsObject]

  object TestBuilder extends BankAuditBlockBuilder

  implicit val request: Request[_] = FakeRequest()

  "buildBankAuditBlock" should {
    "return the correct json" when {
      "the applicant has a bank account" in {

        val testBank: BankAccountDetails = BankAccountDetails(
          name = "Test Bank Account",
          sortCode = "01-02-03",
          number = "01023456",
          status = ValidStatus
        )

        val testScheme = testVatScheme.copy(bankAccount = Some(testBankAccount).map(_.copy(details = Some(testBank))))

        val res = TestBuilder.buildBankAuditBlock(testScheme)

        res mustBe bankAuditBlockJson
      }

      "the applicant does not have a bank account" in {
        val testScheme = testVatScheme.copy(bankAccount = Some(testBankAccountNotProvided))

        val result: JsObject = TestBuilder.buildBankAuditBlock(testScheme)
        result mustBe bankDetailsNotProvidedBlockJson
      }
    }
    "throw an Interval Server Exception" when {
      "the bank account details are missing" in {

        val testScheme = testVatScheme.copy(bankAccount = Some(testBankAccount).map(_.copy(details = None)))

        intercept[InternalServerException](TestBuilder.buildBankAuditBlock(testScheme))
      }
      "the bank account is missing" in {
        val testScheme = testVatScheme.copy(bankAccount = None)

        intercept[InternalServerException](TestBuilder.buildBankAuditBlock(testScheme))
      }
    }
  }
}
