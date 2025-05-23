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
import models.api._
import models.api.vatapplication.{JanuaryStagger, Quarterly, VatApplication}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate

class SubscriptionAuditBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  object TestService extends SubscriptionAuditBlockBuilder

  implicit val request: Request[_] = FakeRequest()

  val fullSubscriptionBlockJson: JsValue = Json.parse(
    s"""
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
       |   "turnoverNext12Months": $testTurnover,
       |   "zeroRatedSupplies": 500
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
       |     "mainCode2": "23456",
       |     "mainCode3": "34567"
       |   },
       |   "description": "testBusinessDescription"
       | }
       |}""".stripMargin
  )

  val minimalSubscriptionBlockJson: JsValue = Json.parse(
    s"""
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
       |   "turnoverNext12Months": $testTurnover,
       |   "zeroRatedSupplies": 500
       | },
       | "businessActivities": {
       |   "sicCodes": {
       |     "primaryMainCode": "12345"
       |   },
       |   "description": "testBusinessDescription"
       | }
       |}""".stripMargin
  )

  "buildSubscriptionBlock" should {
    val testDate                  = LocalDate.of(2020, 2, 2)
    val testVatApplicationDetails = VatApplication(
      None,
      None,
      standardRateSupplies = Some(testTurnover),
      reducedRateSupplies = None,
      zeroRatedSupplies = Some(testZeroRateSupplies),
      turnoverEstimate = Some(testTurnover),
      acceptTurnOverEstimate = None,
      None,
      claimVatRefunds = Some(false),
      Some(Quarterly),
      Some(JanuaryStagger),
      Some(testDate),
      None,
      None,
      None,
      None,
      None
    )

    "build a full subscription json when all data is provided" in {
      val vatScheme = testVatScheme.copy(
        eligibilitySubmissionData = Some(testEligibilitySubmissionData),
        business = Some(testBusiness),
        vatApplication = Some(testVatApplicationDetails),
        flatRateScheme = Some(validFullFlatRateScheme)
      )

      val result = TestService.buildSubscriptionBlock(vatScheme)

      result mustBe fullSubscriptionBlockJson
    }

    "build a minimal subscription json when minimum data is provided" in {
      val vatScheme = testVatScheme.copy(
        eligibilitySubmissionData = Some(
          testEligibilitySubmissionData.copy(
            threshold = Threshold(mandatoryRegistration = false, None, None, None)
          )
        ),
        business = Some(testBusiness.copy(businessActivities = Some(Nil))),
        vatApplication = Some(testVatApplicationDetails.copy(appliedForExemption = Some(true))),
        flatRateScheme = Some(validEmptyFlatRateScheme)
      )

      val result = TestService.buildSubscriptionBlock(vatScheme)

      result mustBe minimalSubscriptionBlockJson
    }

    "build a minimal subscription json when no Flat Rate Scheme is provided" in {
      val vatScheme = testVatScheme.copy(
        eligibilitySubmissionData = Some(
          testEligibilitySubmissionData.copy(
            threshold = Threshold(mandatoryRegistration = false, None, None, None)
          )
        ),
        business = Some(testBusiness.copy(businessActivities = Some(Nil))),
        vatApplication = Some(testVatApplicationDetails.copy(appliedForExemption = Some(true))),
        flatRateScheme = None
      )

      val result = TestService.buildSubscriptionBlock(vatScheme)

      result mustBe minimalSubscriptionBlockJson
    }

    "fail if the Flat Rate Scheme is invalid" in {
      val vatScheme = testVatScheme.copy(
        eligibilitySubmissionData = Some(testEligibilitySubmissionData),
        business = Some(testBusiness),
        vatApplication = Some(testVatApplicationDetails),
        flatRateScheme = Some(invalidEmptyFlatRateScheme)
      )

      intercept[InternalServerException](
        TestService.buildSubscriptionBlock(vatScheme)
      ).message mustBe "[SubscriptionBlockBuilder] FRS scheme data missing when joinFrs is true"
    }

    "fail if the scheme is missing all data" in {
      intercept[InternalServerException](
        TestService.buildSubscriptionBlock(testVatScheme)
      ).message mustBe "[SubscriptionBlockBuilder] Could not build subscription block " +
        "for submission because some of the data is missing: EligibilitySubmissionData found - false, " +
        "VatApplication found - false, Business found - false."
    }

    "fail if any of the repository requests return nothing" in {
      val vatScheme = testVatScheme.copy(
        eligibilitySubmissionData = Some(testEligibilitySubmissionData)
      )

      intercept[InternalServerException](
        TestService.buildSubscriptionBlock(vatScheme)
      ).message mustBe "[SubscriptionBlockBuilder] Could not build subscription block " +
        "for submission because some of the data is missing: EligibilitySubmissionData found - true, " +
        "VatApplication found - false, Business found - false."
    }
  }
}
