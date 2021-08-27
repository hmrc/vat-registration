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

package services.submission

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.submission.NETP
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class BankDetailsBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service: BankDetailsBlockBuilder = new BankDetailsBlockBuilder(
      registrationMongoRepository = mockRegistrationMongoRepository
    )
  }

  val bankDetailsBlockJson: JsObject = Json.parse(
    """
      |{ "UK": {
      |       "accountName": "Test Bank Account",
      |       "sortCode": "010203",
      |       "accountNumber": "01023456"
      |       }
      |}
      |""".stripMargin).as[JsObject]

  val bankDetailsOverseasBlockJson: JsObject = Json.parse(
    """
      |{ "Overseas": {
      |       "name": "Test Overseas Bank Account",
      |       "bic": "010203",
      |       "iban": "01023456"
      |       }
      |}
      |""".stripMargin).as[JsObject]

  val bankDetailsNotProvidedBlockJson: JsObject = Json.parse(
    """
      |{ "UK":
      |  {
      |     "reasonBankAccNotProvided": "1"
      |  }
      |}
      |""".stripMargin).as[JsObject]

  val bankDetailsOverseasNotProvidedBlockJson: JsObject = Json.parse(
    """
      |{ "Overseas":
      |  {
      |     "reasonBankAccNotProvided": "3"
      |  }
      |}
      |""".stripMargin).as[JsObject]

  "buildBankDetailsBlock" should {
    "return the correct json" when {
      "the applicant has a bank account" in new Setup {
        when(mockRegistrationMongoRepository.fetchBankAccount(testRegId))
          .thenReturn(Future.successful(Some(testBankAccount)))
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))

        val result: Option[JsObject] = await(service.buildBankDetailsBlock(testRegId))
        result mustBe Some(bankDetailsBlockJson)
      }

      "the applicant has a overseas bank account" in new Setup {
        when(mockRegistrationMongoRepository.fetchBankAccount(testRegId))
          .thenReturn(Future.successful(Some(testBankAccountOverseas)))
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(partyType = NETP))))

        val result: Option[JsObject] = await(service.buildBankDetailsBlock(testRegId))
        result mustBe Some(bankDetailsOverseasBlockJson)
      }

      "the applicant has a bank account with a sortcode containing hyphens" in new Setup {
        when(mockRegistrationMongoRepository.fetchBankAccount(testRegId))
          .thenReturn(Future.successful(Some(testBankAccount.copy(details = Some(testBankDetails.copy(sortCode = "01-02-03"))))))
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))

        val result: Option[JsObject] = await(service.buildBankDetailsBlock(testRegId))
        result mustBe Some(bankDetailsBlockJson)
      }

      "the applicant does not have a bank account" in new Setup {
        when(mockRegistrationMongoRepository.fetchBankAccount(testRegId))
          .thenReturn(Future.successful(Some(testBankAccountNotProvided)))
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))

        val result: Option[JsObject] = await(service.buildBankDetailsBlock(testRegId))
        result mustBe Some(bankDetailsNotProvidedBlockJson)
      }
    }
    "throw an Interval Server Exception" when {
      "the bank account details are missing" in new Setup {
        when(mockRegistrationMongoRepository.fetchBankAccount(testRegId))
          .thenReturn(Future.successful(Some(testBankAccount.copy(details = None))))
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))

        intercept[InternalServerException](await(service.buildBankDetailsBlock(testRegId)))
      }
      "the bank account overseas details are missing" in new Setup {
        when(mockRegistrationMongoRepository.fetchBankAccount(testRegId))
          .thenReturn(Future.successful(Some(testBankAccountOverseas.copy(overseasDetails = None))))
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(partyType = NETP))))

        intercept[InternalServerException](await(service.buildBankDetailsBlock(testRegId)))
      }
      "the bank account is missing" in new Setup {
        when(mockRegistrationMongoRepository.fetchBankAccount(testRegId))
          .thenReturn(Future.successful(None))
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))

        intercept[InternalServerException](await(service.buildBankDetailsBlock(testRegId)))
      }
      "the bank account is missing and user is a NETP" in new Setup {
        when(mockRegistrationMongoRepository.fetchBankAccount(testRegId))
          .thenReturn(Future.successful(None))
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(partyType = NETP))))

        val result: Option[JsObject] = await(service.buildBankDetailsBlock(testRegId))
        result mustBe None
      }

    }
  }
}
