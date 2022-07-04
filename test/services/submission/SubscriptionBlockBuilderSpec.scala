/*
 * Copyright 2022 HM Revenue & Customs
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
import mocks.MockVatSchemeRepository
import models._
import models.api._
import models.api.returns._
import models.submission.{IdType, NETP, UtrIdType, VrnIdType}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate

class SubscriptionBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture with MockVatSchemeRepository {

  object TestService extends SubscriptionBlockBuilder

  override lazy val testDate = LocalDate.of(2020, 2, 2)
  override lazy val testReturns = Returns(
    Some(testTurnover),
    None,
    Some(12.99),
    reclaimVatOnMostReturns = false,
    Quarterly,
    JanuaryStagger,
    Some(testDate),
    None,
    None,
    None,
    None
  )

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
       |   "turnoverNext12Months": $testTurnover,
       |   "zeroRatedSupplies": 12.99
       | },
       | "schemes": {
       |   "startDate": "$testDate",
       |   "FRSCategory": "testCategory",
       |   "FRSPercentage": 15,
       |   "limitedCostTrader": false
       | },
       | "businessActivities": {
       |   "SICCodes": {
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
       |   "turnoverNext12Months": $testTurnover,
       |   "zeroRatedSupplies": 12.99
       | },
       | "businessActivities": {
       |   "SICCodes": {
       |     "primaryMainCode": "12345"
       |   },
       |   "description": "testBusinessDescription"
       | }
       |}""".stripMargin
  )

  def fullNetpSubscriptionBlockJson: JsValue = Json.obj(
    "reasonForSubscription" -> Json.obj(
      "relevantDate" -> "2020-10-01",
      "registrationReason" -> Json.toJson[RegistrationReason](NonUk),
      "exemptionOrException" -> "0"
    ),
    "yourTurnover" -> Json.obj(
      "VATRepaymentExpected" -> false,
      "turnoverNext12Months" -> testTurnover,
      "zeroRatedSupplies" -> 12.99
    ),
    "schemes" -> Json.obj(
      "startDate" -> testDate,
      "FRSCategory" -> "testCategory",
      "FRSPercentage" -> 15,
      "limitedCostTrader" -> false
    ),
    "businessActivities" -> Json.obj(
      "SICCodes" -> Json.obj(
        "primaryMainCode" -> "12345",
        "mainCode2" -> "23456",
        "mainCode3" -> "34567"
      ),
      "description" -> "testBusinessDescription",
      "goodsToOverseas" -> true,
      "goodsToCustomerEU" -> true,
      "storingGoodsForDispatch" -> Json.toJson[StoringGoodsForDispatch](StoringWithinUk),
      "fulfilmentWarehouse" -> true,
      "FHDDSWarehouseNumber" -> testWarehouseNumber,
      "nameOfWarehouse" -> testWarehouseName
    )
  )

  val otherBusinessInvolvementsJson: JsValue = Json.obj(
    "corporateBodyRegistered" -> Json.obj(
      "dateOfIncorporation" -> testDateOFIncorp,
      "companyRegistrationNumber" -> testCrn,
      "countryOfIncorporation" -> "GB"
    ),
    "reasonForSubscription" -> Json.obj(
      "voluntaryOrEarlierDate" -> testDate,
      "relevantDate" -> testDate,
      "registrationReason" -> Voluntary.key,
      "exemptionOrException" -> VatScheme.exemptionKey
    ),
    "yourTurnover" -> Json.obj(
      "VATRepaymentExpected" -> false,
      "turnoverNext12Months" -> testTurnover,
      "zeroRatedSupplies" -> 12.99
    ),
    "businessActivities" -> Json.obj(
      "SICCodes" -> Json.obj(
        "primaryMainCode" -> "12345"
      ),
      "description" -> "testBusinessDescription"
    ),
    "otherBusinessActivities" -> Json.arr(
      Json.obj(
        "businessName" -> testCompanyName,
        "idType" -> Json.toJson[IdType](VrnIdType),
        "idValue" -> testVrn,
        "stillTrading" -> true
      ),
      Json.obj(
        "businessName" -> testCompanyName,
        "idType" -> Json.toJson[IdType](UtrIdType),
        "idValue" -> testUtr,
        "stillTrading" -> true
      ),
      Json.obj(
        "businessName" -> testCompanyName,
        "stillTrading" -> false
      )
    )
  )

  "buildSubscriptionBlock" should {
    "build a full subscription json when all data is provided and user is mandatory on a forward look" in {
      val vatScheme = testVatScheme.copy(
        applicantDetails = Some(validApplicantDetails),
        returns = Some(testReturns),
        eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(
            mandatoryRegistration = true, Some(LocalDate.of(2020, 10, 1)), None, None
          ),
          registrationReason = ForwardLook
        )),
        flatRateScheme = Some(validFullFlatRateScheme),
        business = Some(testBusiness)
      )

      val result = TestService.buildSubscriptionBlock(vatScheme)

      result mustBe fullSubscriptionBlockJson(reason = ForwardLook.key)
    }

    "build a full subscription json when all data is provided and user is mandatory on a backward look" in {
      val vatScheme = testVatScheme.copy(
        applicantDetails = Some(validApplicantDetails),
        returns = Some(testReturns),
        eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(
            mandatoryRegistration = true, None, Some(LocalDate.of(2020, 10, 1)), None
          ),
          registrationReason = BackwardLook
        )),
        flatRateScheme = Some(validFullFlatRateScheme),
        business = Some(testBusiness)
      )

      val result = TestService.buildSubscriptionBlock(vatScheme)

      result mustBe fullSubscriptionBlockJson(reason = BackwardLook.key)
    }

    "build a full subscription json when all data is provided and user is NETP" in {
      val vatScheme = testVatScheme.copy(
        applicantDetails = Some(validApplicantDetails.copy(
          entity = testSoleTraderEntity.copy(
            nino = None,
            trn = Some(testTrn)
          )
        )),
        returns = Some(testOverseasReturns),
        eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(
            mandatoryRegistration = true, None, None, None, Some(LocalDate.of(2020, 10, 1))
          ),
          partyType = NETP,
          registrationReason = NonUk
        )),
        flatRateScheme = Some(validFullFlatRateScheme),
        business = Some(testBusiness)
      )

      val result = TestService.buildSubscriptionBlock(vatScheme)

      result mustBe fullNetpSubscriptionBlockJson
    }

    "build a minimal subscription json when minimum data is provided and user is voluntary" in {
      val vatScheme = testVatScheme.copy(
        applicantDetails = Some(validApplicantDetails),
        returns = Some(testReturns.copy(appliedForExemption = Some(true))),
        eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(mandatoryRegistration = false, None, None, None),
          registrationReason = Voluntary
        )),
        flatRateScheme = Some(validEmptyFlatRateScheme),
        business = Some(testBusiness.copy(businessActivities = Some(Nil)))
      )

      val result = TestService.buildSubscriptionBlock(vatScheme)

      result mustBe minimalSubscriptionBlockJson
    }

    "build a minimal subscription json when no Flat Rate Scheme is provided" in {
      val vatScheme = testVatScheme.copy(
        applicantDetails = Some(validApplicantDetails),
        returns = Some(testReturns.copy(appliedForExemption = Some(true))),
        eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(mandatoryRegistration = false, None, None, None),
          registrationReason = Voluntary
        )),
        business = Some(testBusiness.copy(businessActivities = Some(Nil)))
      )

      val result = TestService.buildSubscriptionBlock(vatScheme)

      result mustBe minimalSubscriptionBlockJson
    }

    "build a minimal subscription json with other business involvements" in {
      val vatScheme = testVatScheme.copy(
        applicantDetails = Some(validApplicantDetails),
        returns = Some(testReturns.copy(appliedForExemption = Some(true))),
        eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(mandatoryRegistration = false, None, None, None),
          registrationReason = Voluntary
        )),
        business = Some(testBusiness.copy(businessActivities = Some(Nil), otherBusinessInvolvement = Some(true))),
        otherBusinessInvolvements = Some(
          List(
            OtherBusinessInvolvement(
              businessName = testCompanyName,
              hasVrn = true,
              vrn = Some(testVrn),
              hasUtr = None,
              utr = None,
              stillTrading = true
            ),
            OtherBusinessInvolvement(
              businessName = testCompanyName,
              hasVrn = false,
              vrn = None,
              hasUtr = Some(true),
              utr = Some(testUtr),
              stillTrading = true
            ),
            OtherBusinessInvolvement(
              businessName = testCompanyName,
              hasVrn = false,
              vrn = None,
              hasUtr = Some(false),
              utr = None,
              stillTrading = false
            )
          )
        )
      )

      val result = TestService.buildSubscriptionBlock(vatScheme)

      result mustBe otherBusinessInvolvementsJson
    }

    "fail if the Flat Rate Scheme is invalid" in {
      val vatScheme = testVatScheme.copy(
        applicantDetails = Some(validApplicantDetails),
        returns = Some(testReturns),
        eligibilitySubmissionData = Some(testEligibilitySubmissionData),
        flatRateScheme = Some(invalidEmptyFlatRateScheme),
        business = Some(testBusiness)
      )

      intercept[InternalServerException](
        TestService.buildSubscriptionBlock(vatScheme)
      ).message mustBe "[SubscriptionBlockBuilder] FRS scheme data missing when joinFrs is true"
    }

    "fail if any of the repository requests return nothing" in {
      val vatScheme = testVatScheme.copy(
        applicantDetails = Some(validApplicantDetails),
        eligibilitySubmissionData = Some(testEligibilitySubmissionData),
        flatRateScheme = Some(validEmptyFlatRateScheme)
      )

      intercept[InternalServerException](
        TestService.buildSubscriptionBlock(vatScheme)
      ).message mustBe "[SubscriptionBlockBuilder] Could not build subscription block " +
        "for submission because some of the data is missing: ApplicantDetails found - true, EligibilitySubmissionData found - true, " +
        "Returns found - false, Business found - false."
    }
  }
}
