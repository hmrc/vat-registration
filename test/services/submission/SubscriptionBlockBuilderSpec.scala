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
import models.registration.OtherBusinessInvolvementsSectionId
import models.submission.{IdType, NETP, UtrIdType, VrnIdType}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate
import scala.concurrent.Future

class SubscriptionBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture with MockVatSchemeRepository {

  object TestService extends SubscriptionBlockBuilder(mockVatSchemeRepository)

  override lazy val testDate = LocalDate.of(2020, 2, 2)
  override lazy val testReturns = Returns(Some(12.99), reclaimVatOnMostReturns = false, Quarterly, JanuaryStagger, Some(testDate), None, None, None)
  lazy val otherActivities = List(
    SicCode("00002", "testBusiness 2", "testDetails"),
    SicCode("00003", "testBusiness 3", "testDetails"),
    SicCode("00004", "testBusiness 4", "testDetails")
  )
  override lazy val testSicAndCompliance = SicAndCompliance(
    "testDescription",
    None,
    SicCode("12345", "testMainBusiness", "testDetails"),
    otherActivities
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
       |   "turnoverNext12Months": 123456,
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

  def fullNetpSubscriptionBlockJson: JsValue = Json.obj(
    "reasonForSubscription" -> Json.obj(
      "relevantDate" -> "2020-10-01",
      "registrationReason" -> Json.toJson[RegistrationReason](NonUk),
      "exemptionOrException" -> "0"
    ),
    "yourTurnover" -> Json.obj(
      "VATRepaymentExpected" -> false,
      "turnoverNext12Months" -> 123456,
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
        "mainCode2" -> "00002",
        "mainCode3" -> "00003",
        "mainCode4" -> "00004"
      ),
      "description" -> "testDescription",
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
      "exemptionOrException" -> EligibilitySubmissionData.exemptionKey
    ),
    "yourTurnover" -> Json.obj(
      "VATRepaymentExpected" -> false,
      "turnoverNext12Months" -> 123456,
      "zeroRatedSupplies" -> 12.99
    ),
    "businessActivities" -> Json.obj(
      "SICCodes" -> Json.obj(
        "primaryMainCode" -> "12345"
      ),
      "description" -> "testDescription"
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
      when(mockVatSchemeRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))
      when(mockVatSchemeRepository.fetchReturns(any()))
        .thenReturn(Future.successful(Some(testReturns)))
      when(mockVatSchemeRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(
            mandatoryRegistration = true, Some(LocalDate.of(2020, 10, 1)), None, None
          ),
          registrationReason = ForwardLook
        ))))
      when(mockVatSchemeRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(Some(validFullFlatRateScheme)))
      when(mockVatSchemeRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(Some(testSicAndCompliance)))
      mockGetSection[List[OtherBusinessInvolvement]](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(None))

      val result = await(TestService.buildSubscriptionBlock(testInternalId, testRegId))

      result mustBe fullSubscriptionBlockJson(reason = ForwardLook.key)
    }

    "build a full subscription json when all data is provided and user is mandatory on a backward look" in {
      when(mockVatSchemeRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))
      when(mockVatSchemeRepository.fetchReturns(any()))
        .thenReturn(Future.successful(Some(testReturns)))
      when(mockVatSchemeRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(
            mandatoryRegistration = true, None, Some(LocalDate.of(2020, 10, 1)), None
          ),
          registrationReason = BackwardLook
        ))))
      when(mockVatSchemeRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(Some(validFullFlatRateScheme)))
      when(mockVatSchemeRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(Some(testSicAndCompliance)))
      mockGetSection[List[OtherBusinessInvolvement]](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(None))

      val result = await(TestService.buildSubscriptionBlock(testInternalId, testRegId))

      result mustBe fullSubscriptionBlockJson(reason = BackwardLook.key)
    }

    "build a full subscription json when all data is provided and user is NETP" in {
      when(mockVatSchemeRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails.copy(
          entity = testSoleTraderEntity.copy(
            nino = None,
            trn = Some(testTrn)
          )
        ))))
      when(mockVatSchemeRepository.fetchReturns(any()))
        .thenReturn(Future.successful(Some(testOverseasReturns)))
      when(mockVatSchemeRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(
            mandatoryRegistration = true, None, None, None, Some(LocalDate.of(2020, 10, 1))
          ),
          partyType = NETP,
          registrationReason = NonUk
        ))))
      when(mockVatSchemeRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(Some(validFullFlatRateScheme)))
      when(mockVatSchemeRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(Some(testSicAndCompliance)))
      mockGetSection[List[OtherBusinessInvolvement]](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(None))

      val result = await(TestService.buildSubscriptionBlock(testInternalId, testRegId))

      result mustBe fullNetpSubscriptionBlockJson
    }

    "build a minimal subscription json when minimum data is provided and user is voluntary" in {
      when(mockVatSchemeRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))
      when(mockVatSchemeRepository.fetchReturns(any()))
        .thenReturn(Future.successful(Some(testReturns)))
      when(mockVatSchemeRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(mandatoryRegistration = false, None, None, None),
          exceptionOrExemption = "1",
          registrationReason = Voluntary
        ))))
      when(mockVatSchemeRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(Some(validEmptyFlatRateScheme)))
      when(mockVatSchemeRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(Some(testSicAndCompliance.copy(businessActivities = List.empty))))
      mockGetSection[List[OtherBusinessInvolvement]](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(None))

      val result = await(TestService.buildSubscriptionBlock(testInternalId, testRegId))

      result mustBe minimalSubscriptionBlockJson
    }

    "build a minimal subscription json when no Flat Rate Scheme is provided" in {
      when(mockVatSchemeRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))
      when(mockVatSchemeRepository.fetchReturns(any()))
        .thenReturn(Future.successful(Some(testReturns)))
      when(mockVatSchemeRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(mandatoryRegistration = false, None, None, None),
          exceptionOrExemption = "1",
          registrationReason = Voluntary
        ))))
      when(mockVatSchemeRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(None))
      when(mockVatSchemeRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(Some(testSicAndCompliance.copy(businessActivities = List.empty))))
      mockGetSection[List[OtherBusinessInvolvement]](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(None))

      val result = await(TestService.buildSubscriptionBlock(testInternalId, testRegId))

      result mustBe minimalSubscriptionBlockJson
    }

    "build a minimal subscription json with other business involvements" in {
      when(mockVatSchemeRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))
      when(mockVatSchemeRepository.fetchReturns(any()))
        .thenReturn(Future.successful(Some(testReturns)))
      when(mockVatSchemeRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(
          threshold = Threshold(mandatoryRegistration = false, None, None, None),
          exceptionOrExemption = "1",
          registrationReason = Voluntary
        ))))
      when(mockVatSchemeRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(None))
      when(mockVatSchemeRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(Some(testSicAndCompliance.copy(businessActivities = List.empty, otherBusinessInvolvement = Some(true)))))
      mockGetSection[List[OtherBusinessInvolvement]](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(Some(
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
      )))

      val result = await(TestService.buildSubscriptionBlock(testInternalId, testRegId))

      result mustBe otherBusinessInvolvementsJson
    }

    "fail if the Flat Rate Scheme is invalid" in {
      when(mockVatSchemeRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))
      when(mockVatSchemeRepository.fetchReturns(any()))
        .thenReturn(Future.successful(Some(testReturns)))
      when(mockVatSchemeRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))
      when(mockVatSchemeRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(Some(invalidEmptyFlatRateScheme)))
      when(mockVatSchemeRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(Some(testSicAndCompliance)))
      mockGetSection[List[OtherBusinessInvolvement]](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(None))

      val result = TestService.buildSubscriptionBlock(testInternalId, testRegId)

      intercept[InternalServerException](await(result)).message mustBe "[SubscriptionBlockBuilder] FRS scheme data missing when joinFrs is true"
    }

    "fail if the party type cannot be retrieved from the eligibility data" in {
      when(mockVatSchemeRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(None))
      when(mockVatSchemeRepository.fetchReturns(any()))
        .thenReturn(Future.successful(None))
      when(mockVatSchemeRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(None))
      when(mockVatSchemeRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(None))
      when(mockVatSchemeRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(None))
      mockGetSection[List[OtherBusinessInvolvement]](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(None))

      val result = TestService.buildSubscriptionBlock(testInternalId, testRegId)

      intercept[InternalServerException](await(result)).message mustBe "[SubscriptionBlockBuilder] Could not build subscription block due to missing party type"
    }

    "fail if any of the repository requests return nothing" in {
      when(mockVatSchemeRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))
      when(mockVatSchemeRepository.fetchReturns(any()))
        .thenReturn(Future.successful(None))
      when(mockVatSchemeRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))
      when(mockVatSchemeRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(Some(validEmptyFlatRateScheme)))
      when(mockVatSchemeRepository.fetchSicAndCompliance(any()))
        .thenReturn(Future.successful(None))
      mockGetSection[List[OtherBusinessInvolvement]](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(None))

      val result = TestService.buildSubscriptionBlock(testInternalId, testRegId)

      intercept[InternalServerException](await(result)).message mustBe "[SubscriptionBlockBuilder] Could not build subscription block " +
        "for submission because some of the data is missing: ApplicantDetails found - true, EligibilitySubmissionData found - true, " +
        "Returns found - false, SicAndCompliance found - false."
    }
  }
}
