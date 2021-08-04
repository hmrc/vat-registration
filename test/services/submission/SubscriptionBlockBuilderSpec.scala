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
import models.api._
import models.api.returns.{JanuaryStagger, Quarterly, Returns}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate
import scala.concurrent.Future

class SubscriptionBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  object TestService extends SubscriptionBlockBuilder(mockRegistrationMongoRepository)

  def fullSubscriptionBlockJson(reason: String = "0016"): JsValue = Json.parse(
    s"""
       |{
       | "corporateBodyRegistered": {
       |   "dateOfIncorporation": "2020-01-02",
       |   "companyRegistrationNumber": "testCrn",
       |   "countryOfIncorporation": "GB"
       | },
       | "reasonForSubscription": {
       |   "voluntaryOrEarlierDate": "2020-02-02",
       |   "relevantDate": "2020-10-01",
       |   "registrationReason": "$reason",
       |   "exemptionOrException": "0"
       | },
       | "yourTurnover": {
       |   "VATRepaymentExpected": false,
       |   "turnoverNext12Months": 123456,
       |   "zeroRatedSupplies": 12.99
       | },
       | "schemes": {
       |   "startDate": "2018-01-01",
       |   "FRSCategory": "testCategory",
       |   "FRSPercentage": 15,
       |   "limitedCostTrader": false
       | },
       | "businessActivities": {
       |   "SICCodes": {
       |     "primaryMainCode": "12345",
       |     "mainCode2": "00002",
       |     "mainCode3": "00003",
       |     "mainCode4": "00004"
       |   },
       |   "description": "testDescription"
       | }
       |}""".stripMargin
  )

  val minimalSubscriptionBlockJson: JsValue = Json.parse(
    """
      |{
      | "corporateBodyRegistered": {
      |   "dateOfIncorporation": "2020-01-02",
      |   "companyRegistrationNumber": "testCrn",
      |   "countryOfIncorporation": "GB"
      | },
      | "reasonForSubscription": {
      |   "voluntaryOrEarlierDate": "2020-02-02",
      |   "relevantDate": "2020-02-02",
      |   "registrationReason": "0018",
      |   "exemptionOrException": "1"
      | },
      | "yourTurnover": {
      |   "VATRepaymentExpected": false,
      |   "turnoverNext12Months": 123456,
      |   "zeroRatedSupplies": 12.99
      | },
      | "businessActivities": {
      |   "SICCodes": {
      |     "primaryMainCode": "12345"
      |   },
      |   "description": "testDescription"
      | }
      |}""".stripMargin
  )

  "buildSubscriptionBlock" should {
    val testDate = LocalDate.of(2020, 2, 2)
    val testReturns = Returns(Some(12.99), reclaimVatOnMostReturns = false, Quarterly, JanuaryStagger, Some(testDate), None)
    val otherActivities = List(
      SicCode("00002", "testBusiness 2", "testDetails"),
      SicCode("00003", "testBusiness 3", "testDetails"),
      SicCode("00004", "testBusiness 4", "testDetails")
    )
    val testSicAndCompliance = SicAndCompliance(
      "testDescription",
      None,
      SicCode("12345", "testMainBusiness", "testDetails"),
      otherActivities
    )

    "build a full subscription json when all data is provided and user is mandatory on a forward look" in {
      when(mockRegistrationMongoRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))
      when(mockRegistrationMongoRepository.fetchReturns(any()))
        .thenReturn(Future.successful(Some(testReturns)))
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(threshold = Threshold(
          mandatoryRegistration = true, Some(LocalDate.of(2020, 10, 1)), None, None
        )))))
      when(mockRegistrationMongoRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(Some(validFullFlatRateScheme)))
      when(mockRegistrationMongoRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(Some(testSicAndCompliance)))

      val result = await(TestService.buildSubscriptionBlock(testRegId))

      result mustBe fullSubscriptionBlockJson(reason = EligibilitySubmissionData.forwardLookKey)
    }

    "build a full subscription json when all data is provided and user is mandatory on a backward look" in {
      when(mockRegistrationMongoRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))
      when(mockRegistrationMongoRepository.fetchReturns(any()))
        .thenReturn(Future.successful(Some(testReturns)))
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(threshold = Threshold(
          mandatoryRegistration = true, None, Some(LocalDate.of(2020, 10, 1)), None
        )))))
      when(mockRegistrationMongoRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(Some(validFullFlatRateScheme)))
      when(mockRegistrationMongoRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(Some(testSicAndCompliance)))

      val result = await(TestService.buildSubscriptionBlock(testRegId))

      result mustBe fullSubscriptionBlockJson(reason = EligibilitySubmissionData.backwardLookKey)
    }

    "build a minimal subscription json when minimum data is provided and user is voluntary" in {
      when(mockRegistrationMongoRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))
      when(mockRegistrationMongoRepository.fetchReturns(any()))
        .thenReturn(Future.successful(Some(testReturns)))
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(mandatoryRegistration = false, None, None, None),
          exceptionOrExemption = "1"
        ))))
      when(mockRegistrationMongoRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(Some(validEmptyFlatRateScheme)))
      when(mockRegistrationMongoRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(Some(testSicAndCompliance.copy(businessActivities = List.empty))))

      val result = await(TestService.buildSubscriptionBlock(testRegId))

      result mustBe minimalSubscriptionBlockJson
    }

    "build a minimal subscription json when no Flat Rate Scheme is provided" in {
      when(mockRegistrationMongoRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))
      when(mockRegistrationMongoRepository.fetchReturns(any()))
        .thenReturn(Future.successful(Some(testReturns)))
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(mandatoryRegistration = false, None, None, None),
          exceptionOrExemption = "1"
        ))))
      when(mockRegistrationMongoRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(None))
      when(mockRegistrationMongoRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(Some(testSicAndCompliance.copy(businessActivities = List.empty))))

      val result = await(TestService.buildSubscriptionBlock(testRegId))

      result mustBe minimalSubscriptionBlockJson
    }

    "fail if the Flat Rate Scheme is invalid" in {
      when(mockRegistrationMongoRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))
      when(mockRegistrationMongoRepository.fetchReturns(any()))
        .thenReturn(Future.successful(Some(testReturns)))
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))
      when(mockRegistrationMongoRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(Some(invalidEmptyFlatRateScheme)))
      when(mockRegistrationMongoRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(Some(testSicAndCompliance)))

      val result = TestService.buildSubscriptionBlock(testRegId)

      intercept[InternalServerException](await(result)).message mustBe "[SubscriptionBlockBuilder] FRS scheme data missing when joinFrs is true"
    }

    "fail if the party type cannot be retrieved from the eligibility data" in {
      when(mockRegistrationMongoRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(None))
      when(mockRegistrationMongoRepository.fetchReturns(any()))
        .thenReturn(Future.successful(None))
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(None))
      when(mockRegistrationMongoRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(None))
      when(mockRegistrationMongoRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(None))

      val result = TestService.buildSubscriptionBlock(testRegId)

      intercept[InternalServerException](await(result)).message mustBe "[SubscriptionBlockBuilder] Could not build subscription block due to missing party type"
    }

    "fail if any of the repository requests return nothing" in {
      when(mockRegistrationMongoRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))
      when(mockRegistrationMongoRepository.fetchReturns(any()))
        .thenReturn(Future.successful(None))
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))
      when(mockRegistrationMongoRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(Some(validEmptyFlatRateScheme)))
      when(mockRegistrationMongoRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(None))

      val result = TestService.buildSubscriptionBlock(testRegId)

      intercept[InternalServerException](await(result)).message mustBe "[SubscriptionBlockBuilder] Could not build subscription block " +
        "for submission because some of the data is missing: ApplicantDetails found - true, EligibilitySubmissionData found - true, " +
        "Returns found - false, SicAndCompliance found - false."
    }
  }
}
