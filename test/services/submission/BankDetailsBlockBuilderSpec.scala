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

package services.submission

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api._
import models.submission.NETP
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.InternalServerException

class BankDetailsBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  implicit val request: Request[_] = FakeRequest()

  class Setup {
    val service: BankDetailsBlockBuilder = new BankDetailsBlockBuilder
  }

  val bankDetailsBlockJson: JsObject = Json.obj(
    "UK" -> Json.obj(
      "accountName" -> testBankName,
      "sortCode" -> testSortCode,
      "accountNumber" -> testBankNumber
    )
  )

  val notValidBankDetailsBlockJson: JsObject = Json.obj(
    "UK" -> Json.obj(
      "accountName" -> testBankName,
      "sortCode" -> testSortCode,
      "accountNumber" -> testBankNumber,
      "bankDetailsNotValid" -> true
    )
  )

  val bankDetailsNotProvidedBlockJson: JsObject = Json.obj(
    "UK" -> Json.obj(
      "reasonBankAccNotProvided" -> NoUKBankAccount.reasonId(BeingSetup)
    )
  )

  val bankDetailsOverseasNotProvidedBlockJson: JsObject = Json.obj(
    "UK" -> Json.obj(
      "reasonBankAccNotProvided" -> NoUKBankAccount.reasonId(OverseasAccount)
    )
  )

  "buildBankDetailsBlock" should {
    "return the correct json" when {
      "the applicant has a bank account" in new Setup {
        val vatScheme = testVatScheme.copy(
          bankAccount = Some(testBankAccount),
          eligibilitySubmissionData = Some(testEligibilitySubmissionData)
        )

        val result: Option[JsObject] = service.buildBankDetailsBlock(vatScheme)
        result mustBe Some(bankDetailsBlockJson)
      }

      "the applicant has an indeterminate bank account" in new Setup {
        val vatScheme = testVatScheme.copy(
          bankAccount = Some(testBankAccount.copy(details = Some(testBankDetails.copy(status = IndeterminateStatus)))),
          eligibilitySubmissionData = Some(testEligibilitySubmissionData)
        )

        val result: Option[JsObject] = service.buildBankDetailsBlock(vatScheme)
        result mustBe Some(notValidBankDetailsBlockJson)
      }

      "the applicant has an invalid bank account" in new Setup {
        val vatScheme = testVatScheme.copy(
          bankAccount = Some(testBankAccount.copy(details = Some(testBankDetails.copy(status = InvalidStatus)))),
          eligibilitySubmissionData = Some(testEligibilitySubmissionData)
        )

        val result: Option[JsObject] = service.buildBankDetailsBlock(vatScheme)
        result mustBe Some(notValidBankDetailsBlockJson)
      }

      "the applicant has a bank account with a sortcode containing hyphens" in new Setup {
        val vatScheme = testVatScheme.copy(
          bankAccount = Some(testBankAccount.copy(details = Some(testBankDetails.copy(sortCode = "01-02-03")))),
          eligibilitySubmissionData = Some(testEligibilitySubmissionData)
        )

        val result: Option[JsObject] = service.buildBankDetailsBlock(vatScheme)
        result mustBe Some(bankDetailsBlockJson)
      }

      "the applicant does not have a bank account" in new Setup {
        val vatScheme = testVatScheme.copy(
          bankAccount = Some(testBankAccountNotProvided),
          eligibilitySubmissionData = Some(testEligibilitySubmissionData)
        )

        val result: Option[JsObject] = service.buildBankDetailsBlock(vatScheme)
        result mustBe Some(bankDetailsNotProvidedBlockJson)
      }

      "the bank account is missing and user is a NETP" in new Setup {
        val vatScheme = testVatScheme.copy(
          bankAccount = None,
          eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = NETP))
        )

        val result: Option[JsObject] = service.buildBankDetailsBlock(vatScheme)
        result mustBe Some(bankDetailsOverseasNotProvidedBlockJson)
      }
    }
    "throw an Interval Server Exception" when {
      "the bank account details are missing" in new Setup {
        val vatScheme = testVatScheme.copy(
          bankAccount = Some(testBankAccount.copy(details = None)),
          eligibilitySubmissionData = Some(testEligibilitySubmissionData)
        )

        intercept[InternalServerException](service.buildBankDetailsBlock(vatScheme))
      }

      "the bank account is missing" in new Setup {
        val vatScheme = testVatScheme.copy(
          bankAccount = None,
          eligibilitySubmissionData = Some(testEligibilitySubmissionData)
        )
        intercept[InternalServerException](service.buildBankDetailsBlock(vatScheme))
      }
    }
  }
}
