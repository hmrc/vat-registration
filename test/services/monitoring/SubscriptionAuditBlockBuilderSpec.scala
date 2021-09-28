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

package services.monitoring

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api._
import models.api.returns.{JanuaryStagger, Quarterly, Returns}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate

class SubscriptionAuditBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  object TestService extends SubscriptionAuditBlockBuilder

  val fullSubscriptionBlockJson: JsValue = Json.parse(
    """
      |{
      | "overThresholdIn12MonthPeriod": true,
      | "overThresholdIn12MonthDate": "2020-10-07",
      | "overThresholdInPreviousMonth": true,
      | "overThresholdInPreviousMonthDate": "2020-10-07",
      | "overThresholdInNextMonth": true,
      | "overThresholdInNextMonthDate": "2020-10-07",
      | "reasonForSubscription": {
      |   "voluntaryOrEarlierDate": "2020-02-02",
      |   "exemptionOrException": "0"
      | },
      | "yourTurnover": {
      |   "vatRepaymentExpected": false,
      |   "turnoverNext12Months": 123456,
      |   "zeroRatedSupplies": 12.99
      | },
      | "schemes": {
      |   "startDate": "2018-01-01",
      |   "flatRateSchemeCategory": "testCategory",
      |   "flatRateSchemePercentage": 15,
      |   "limitedCostTrader": false
      | },
      | "businessActivities": {
      |   "sicCodes": {
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
      | "overThresholdIn12MonthPeriod": false,
      | "overThresholdInPreviousMonth": false,
      | "overThresholdInNextMonth": false,
      | "reasonForSubscription": {
      |   "voluntaryOrEarlierDate": "2020-02-02",
      |   "exemptionOrException": "1"
      | },
      | "yourTurnover": {
      |   "vatRepaymentExpected": false,
      |   "turnoverNext12Months": 123456,
      |   "zeroRatedSupplies": 12.99
      | },
      | "businessActivities": {
      |   "sicCodes": {
      |     "primaryMainCode": "12345"
      |   },
      |   "description": "testDescription"
      | }
      |}""".stripMargin
  )

  "buildSubscriptionBlock" should {
    val testDate = LocalDate.of(2020, 2, 2)
    val testReturns = Returns(Some(12.99), reclaimVatOnMostReturns = false, Quarterly, JanuaryStagger, Some(testDate), None, None, None)
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

    "build a full subscription json when all data is provided" in {
      val vatScheme = testVatScheme.copy(
        eligibilitySubmissionData = Some(testEligibilitySubmissionData),
        sicAndCompliance = Some(testSicAndCompliance),
        returns = Some(testReturns),
        flatRateScheme = Some(validFullFlatRateScheme)
      )

      val result = TestService.buildSubscriptionBlock(vatScheme)

      result mustBe fullSubscriptionBlockJson
    }

    "build a minimal subscription json when minimum data is provided" in {
      val vatScheme = testVatScheme.copy(
        eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(mandatoryRegistration = false, None, None, None),
          exceptionOrExemption = "1"
        )),
        sicAndCompliance = Some(testSicAndCompliance.copy(businessActivities = List.empty)),
        returns = Some(testReturns),
        flatRateScheme = Some(validEmptyFlatRateScheme)
      )

      val result = TestService.buildSubscriptionBlock(vatScheme)

      result mustBe minimalSubscriptionBlockJson
    }

    "build a minimal subscription json when no Flat Rate Scheme is provided" in {
      val vatScheme = testVatScheme.copy(
        eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(mandatoryRegistration = false, None, None, None),
          exceptionOrExemption = "1"
        )),
        sicAndCompliance = Some(testSicAndCompliance.copy(businessActivities = List.empty)),
        returns = Some(testReturns),
        flatRateScheme = None
      )

      val result = TestService.buildSubscriptionBlock(vatScheme)

      result mustBe minimalSubscriptionBlockJson
    }

    "fail if the Flat Rate Scheme is invalid" in {
      val vatScheme = testVatScheme.copy(
        eligibilitySubmissionData = Some(testEligibilitySubmissionData),
        sicAndCompliance = Some(testSicAndCompliance),
        returns = Some(testReturns),
        flatRateScheme = Some(invalidEmptyFlatRateScheme)
      )

      intercept[InternalServerException](TestService.buildSubscriptionBlock(vatScheme))
        .message mustBe "[SubscriptionBlockBuilder] FRS scheme data missing when joinFrs is true"
    }

    "fail if the scheme is missing all data" in {
      intercept[InternalServerException](TestService.buildSubscriptionBlock(testVatScheme))
        .message mustBe "[SubscriptionBlockBuilder] Could not build subscription block " +
        "for submission because some of the data is missing: EligibilitySubmissionData found - false, " +
        "Returns found - false, SicAndCompliance found - false."
    }

    "fail if any of the repository requests return nothing" in {
      val vatScheme = testVatScheme.copy(
        eligibilitySubmissionData = Some(testEligibilitySubmissionData)
      )

      intercept[InternalServerException](TestService.buildSubscriptionBlock(vatScheme))
        .message mustBe "[SubscriptionBlockBuilder] Could not build subscription block " +
        "for submission because some of the data is missing: EligibilitySubmissionData found - true, " +
        "Returns found - false, SicAndCompliance found - false."
    }
  }
}
