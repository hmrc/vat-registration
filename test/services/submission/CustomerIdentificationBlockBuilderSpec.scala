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
import models.api.{BvCtEnrolled, BvPass, BvUnchallenged, FailedStatus}
import models.submission.Individual
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class CustomerIdentificationBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service: CustomerIdentificationBlockBuilder = new CustomerIdentificationBlockBuilder(
      registrationMongoRepository = mockRegistrationMongoRepository
    )
  }

  lazy val customerIdentificationBlockWithBPJson: JsObject = Json.parse(
    """
      |{
      |    "tradingName": "trading-name",
      |    "tradersPartyType": "50",
      |    "primeBPSafeID": "testBpSafeId"
      |}
      |""".stripMargin).as[JsObject]

  def customerIdentificationBlockJson(idVerificationStatusCode: Int): JsObject = Json.parse(
    s"""
       |{
       |    "tradingName": "trading-name",
       |    "tradersPartyType": "50",
       |    "shortOrgName": "testCompanyName",
       |    "customerID": [
       |      {
       |        "idValue": "testCtUtr",
       |        "idType": "UTR",
       |        "IDsVerificationStatus": "$idVerificationStatusCode"
       |      },
       |      {
       |        "idValue": "testCrn",
       |        "idType": "CRN",
       |        "IDsVerificationStatus": "$idVerificationStatusCode",
       |        "date": "2020-01-02"
       |      }
       |    ]
       |}
       |""".stripMargin).as[JsObject]

  val soleTraderBlockJson = Json.parse(
    s"""
       |{
       |    "tradingName": "trading-name",
       |    "tradersPartyType": "Z1",
       |    "customerID": [
       |      {
       |        "idValue": "testCtUtr",
       |        "idType": "UTR",
       |        "IDsVerificationStatus": "1"
       |      }
       |    ]
       |}
       |""".stripMargin).as[JsObject]

  "buildCustomerIdentificationBlock" should {
    "build the correct json for a sole trader entity type" in new Setup {
      val appDetails = validApplicantDetails.copy(entity = testSoleTraderEntity)
      val eligibilityData = testEligibilitySubmissionData.copy(partyType = Individual)

      when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
        .thenReturn(Future.successful(Some(testFullVatScheme.copy(applicantDetails = Some(appDetails), eligibilitySubmissionData = Some(eligibilityData)))))

      val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
      result mustBe soleTraderBlockJson
    }
    "return Status Code 1" when {
      "the businessVerificationStatus is BvPass" in new Setup {
        val appDetails = validApplicantDetails.copy(entity = testLtdCoEntity.copy(businessVerification = BvPass, registration = FailedStatus))

        when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
          .thenReturn(Future.successful(Some(testFullVatScheme.copy(applicantDetails = Some(appDetails)))))

        val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
        result mustBe customerIdentificationBlockJson(1)
      }
      "the businessVerificationStatus is CtEnrolled" in new Setup {
        val appDetails = validApplicantDetails.copy(entity = testLtdCoEntity.copy(businessVerification = BvCtEnrolled, registration = FailedStatus))

        when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
          .thenReturn(Future.successful(Some(testFullVatScheme.copy(applicantDetails = Some(appDetails)))))

        val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
        result mustBe customerIdentificationBlockJson(1)
      }
    }
    "return Status Code 2" when {
      "the identifiersMatch is false" in new Setup {
        val appDetails = validApplicantDetails.copy(entity = testLtdCoEntity.copy(businessVerification = BvUnchallenged, identifiersMatch = false))

        when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
          .thenReturn(Future.successful(Some(testFullVatScheme.copy(applicantDetails = Some(appDetails)))))

        val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
        result mustBe customerIdentificationBlockJson(2)
      }
    }
    "return Status Code 3" when {
      "businessVerification fails" in new Setup {
        when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
          .thenReturn(Future.successful(Some(testFullVatScheme)))

        val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
        result mustBe customerIdentificationBlockJson(3)
      }
      "businessVerification is not called" in new Setup {
        val appDetails = validApplicantDetails.copy(entity = testLtdCoEntity.copy(businessVerification = BvUnchallenged))

        when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
          .thenReturn(Future.successful(Some(testFullVatScheme.copy(applicantDetails = Some(appDetails)))))

        val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
        result mustBe customerIdentificationBlockJson(3)
      }
    }
    "return the BP Safe ID" when {
      "businessVerificationStatus is Pass" in new Setup {
        val appDetails = validApplicantDetails.copy(entity = testLtdCoEntity.copy(businessVerification = BvPass, bpSafeId = Some(testBpSafeId)))

        when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
          .thenReturn(Future.successful(Some(testFullVatScheme.copy(applicantDetails = Some(appDetails)))))

        val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
        result mustBe customerIdentificationBlockWithBPJson
      }
      "businessVerification is CT-Enrolled" in new Setup {
        val appDetails = validApplicantDetails.copy(entity = testLtdCoEntity.copy(businessVerification = BvCtEnrolled, bpSafeId = Some(testBpSafeId)))

        when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
          .thenReturn(Future.successful(Some(testFullVatScheme.copy(applicantDetails = Some(appDetails)))))

        val result: JsObject = await(service.buildCustomerIdentificationBlock(testRegId))
        result mustBe customerIdentificationBlockWithBPJson
      }
    }
    "throw an Interval Server Exception" when {
      "applicant details is missing" in new Setup {
        when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
          .thenReturn(Future.successful(Some(testFullVatScheme.copy(applicantDetails = None))))

        intercept[InternalServerException](await(service.buildCustomerIdentificationBlock(testRegId)))
      }
      "trading details is missing" in new Setup {
        when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
          .thenReturn(Future.successful(Some(testFullVatScheme.copy(tradingDetails = None))))

        intercept[InternalServerException](await(service.buildCustomerIdentificationBlock(testRegId)))
      }
      "applicant details and trading details are missing" in new Setup {
        when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
          .thenReturn(Future.successful(Some(testFullVatScheme.copy(applicantDetails = None, tradingDetails = None))))

        intercept[InternalServerException](await(service.buildCustomerIdentificationBlock(testRegId)))
      }
    }
  }
}
