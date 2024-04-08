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

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.MockVatSchemeRepository
import models.api.{EligibilitySubmissionData, VatScheme}
import models.registration.{EligibilityJsonSectionId, EligibilitySectionId}
import models.submission.RegSociety
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers, Mockito}
import play.api.libs.json.{JsObject, JsResultException, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class EligibilityServiceSpec extends VatRegSpec with VatRegistrationFixture with MockVatSchemeRepository {

  class Setup {
    val service: EligibilityService = new EligibilityService(
      registrationRepository = mockVatSchemeRepository
    )
  }

  implicit val request: Request[_] = FakeRequest()

  "updateEligibilityData" should {
    val eligibilityData = Json.obj(
      "fixedEstablishment"          -> true,
      "businessEntity"              -> "50",
      "agriculturalFlatRateScheme"  -> false,
      "internationalActivities"     -> false,
      "registeringBusiness"         -> "own",
      "registrationReason"          -> "selling-goods-and-services",
      "thresholdPreviousThirtyDays" -> Json.obj(
        "value"        -> true,
        "optionalData" -> testEligibilitySubmissionData.threshold.thresholdPreviousThirtyDays
      ),
      "thresholdInTwelveMonths"     -> Json.obj(
        "value"        -> true,
        "optionalData" -> testEligibilitySubmissionData.threshold.thresholdInTwelveMonths
      ),
      "thresholdNextThirtyDays"     -> Json.obj(
        "value"        -> true,
        "optionalData" -> testEligibilitySubmissionData.threshold.thresholdNextThirtyDays
      ),
      "vatRegistrationException"    -> false
    )

    "return the data that is being provided" in new Setup {
      mockUpsertSection(testInternalId, testRegId, EligibilitySectionId.repoKey, testEligibilitySubmissionData)(
        Some(testEligibilitySubmissionData)
      )
      mockUpsertSection(testInternalId, testRegId, EligibilityJsonSectionId.repoKey, eligibilityData)(
        Some(eligibilityData)
      )
      mockGetSection[EligibilitySubmissionData](testInternalId, testRegId, EligibilitySectionId.repoKey)(
        Future.successful(None)
      )

      val result: Option[JsObject] = await(service.updateEligibilityData(testInternalId, testRegId, eligibilityData))

      result mustBe Some(eligibilityData)
    }

    "return eligibility data and clear user's vat scheme if the partytype is changed" in new Setup {
      val testClearedVatScheme: VatScheme = testVatScheme.copy(
        confirmInformationDeclaration = Some(true)
      )

      mockUpsertSection(testInternalId, testRegId, EligibilitySectionId.repoKey, testEligibilitySubmissionData)(
        Some(testEligibilitySubmissionData)
      )
      mockUpsertSection(testInternalId, testRegId, EligibilityJsonSectionId.repoKey, eligibilityData)(
        Some(eligibilityData)
      )
      mockGetSection[EligibilitySubmissionData](testInternalId, testRegId, EligibilitySectionId.repoKey)(
        Future.successful(
          Some(testEligibilitySubmissionData.copy(partyType = RegSociety))
        )
      )
      mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testFullVatScheme)))
      mockUpsertRegistration(testInternalId, testRegId, testClearedVatScheme)(
        Future.successful(Some(testClearedVatScheme))
      )

      val result: Option[JsObject] = await(service.updateEligibilityData(testInternalId, testRegId, eligibilityData))

      result mustBe Some(eligibilityData)
      verify(mockVatSchemeRepository, Mockito.atLeastOnce())
        .upsertRegistration(
          ArgumentMatchers.eq(testInternalId),
          ArgumentMatchers.eq(testRegId),
          ArgumentMatchers.eq(testClearedVatScheme)
        )
    }

    "return eligibility data and clear user's transactor details if the transactor answer is changed" in new Setup {
      val vatSchemeWithTransactor: VatScheme    = testFullVatScheme.copy(transactorDetails = Some(validTransactorDetails))
      val vatSchemeWithoutTransactor: VatScheme = testFullVatScheme.copy(transactorDetails = None)

      mockUpsertSection(testInternalId, testRegId, EligibilitySectionId.repoKey, testEligibilitySubmissionData)(
        Some(testEligibilitySubmissionData)
      )
      mockUpsertSection(testInternalId, testRegId, EligibilityJsonSectionId.repoKey, eligibilityData)(
        Some(eligibilityData)
      )
      mockGetSection[EligibilitySubmissionData](testInternalId, testRegId, EligibilitySectionId.repoKey)(
        Future.successful(
          Some(testEligibilitySubmissionData.copy(isTransactor = true))
        )
      )
      mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(vatSchemeWithTransactor)))
      mockUpsertRegistration(testInternalId, testRegId, vatSchemeWithoutTransactor)(
        Future.successful(Some(vatSchemeWithoutTransactor))
      )

      val result: Option[JsObject] = await(service.updateEligibilityData(testInternalId, testRegId, eligibilityData))

      result mustBe Some(eligibilityData)
      verify(mockVatSchemeRepository, Mockito.atLeastOnce())
        .upsertRegistration(
          ArgumentMatchers.eq(testInternalId),
          ArgumentMatchers.eq(testRegId),
          ArgumentMatchers.eq(vatSchemeWithoutTransactor)
        )
    }

    "return eligibility data and clear user's exemption answer if the exception answer is true" in new Setup {
      val eligibilityData: JsObject = Json.obj(
        "fixedEstablishment"          -> true,
        "businessEntity"              -> "50",
        "agriculturalFlatRateScheme"  -> false,
        "internationalActivities"     -> false,
        "registeringBusiness"         -> "own",
        "registrationReason"          -> "selling-goods-and-services",
        "thresholdPreviousThirtyDays" -> Json.obj(
          "value"        -> true,
          "optionalData" -> "2020-10-07"
        ),
        "thresholdInTwelveMonths"     -> Json.obj(
          "value"        -> true,
          "optionalData" -> "2020-10-07"
        ),
        "thresholdNextThirtyDays"     -> Json.obj(
          "value"        -> true,
          "optionalData" -> "2020-10-07"
        ),
        "vatRegistrationException"    -> true
      )

      val vatSchemeWithExemption: VatScheme                             = testFullVatScheme
        .copy(
          vatApplication = Some(testVatApplicationDetails.copy(appliedForExemption = Some(true))),
          eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(appliedForException = Some(false)))
        )
      val vatSchemeWithoutExemption: VatScheme                          = vatSchemeWithExemption
        .copy(
          vatApplication = Some(testVatApplicationDetails.copy(appliedForExemption = None))
        )
      val exceptionEligibilitySubmissionData: EligibilitySubmissionData =
        testEligibilitySubmissionData.copy(appliedForException = Some(true))

      mockUpsertSection(testInternalId, testRegId, EligibilitySectionId.repoKey, exceptionEligibilitySubmissionData)(
        Some(exceptionEligibilitySubmissionData)
      )
      mockUpsertSection(testInternalId, testRegId, EligibilityJsonSectionId.repoKey, eligibilityData)(
        Some(eligibilityData)
      )
      mockGetSection[EligibilitySubmissionData](testInternalId, testRegId, EligibilitySectionId.repoKey)(
        Future.successful(
          Some(testEligibilitySubmissionData)
        )
      )
      mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(vatSchemeWithExemption)))
      mockUpsertRegistration(testInternalId, testRegId, vatSchemeWithoutExemption)(
        Future.successful(Some(vatSchemeWithoutExemption))
      )

      val result: Option[JsObject] = await(service.updateEligibilityData(testInternalId, testRegId, eligibilityData))

      result mustBe Some(eligibilityData)
      verify(mockVatSchemeRepository, Mockito.atLeastOnce())
        .upsertRegistration(
          ArgumentMatchers.eq(testInternalId),
          ArgumentMatchers.eq(testRegId),
          ArgumentMatchers.eq(vatSchemeWithoutExemption)
        )
    }

    "return eligibility data and not clear any vatscheme fields where eligibility data is unchanged" in new Setup {
      mockUpsertSection(testInternalId, testRegId, EligibilitySectionId.repoKey, testEligibilitySubmissionData)(
        Some(testEligibilitySubmissionData)
      )
      mockUpsertSection(testInternalId, testRegId, EligibilityJsonSectionId.repoKey, eligibilityData)(
        Some(eligibilityData)
      )
      mockGetSection[EligibilitySubmissionData](testInternalId, testRegId, EligibilitySectionId.repoKey)(
        Future.successful(Some(testEligibilitySubmissionData))
      )
      mockGetRegistration(testInternalId, testRegId)(
        Future.successful(
          Some(
            testFullVatScheme.copy(eligibilitySubmissionData = Some(testEligibilitySubmissionData))
          )
        )
      )

      val result: Option[JsObject] = await(service.updateEligibilityData(testInternalId, testRegId, eligibilityData))

      result mustBe Some(eligibilityData)
    }

    "encounter a JsResultException if json provided is incorrect" in new Setup {
      val incorrectEligibilityData: JsObject = Json.obj("invalid" -> "json")

      intercept[JsResultException](
        await(service.updateEligibilityData(testInternalId, testRegId, incorrectEligibilityData))
      )
    }

    "encounter an exception if an error occurs during eligibility submission data update" in new Setup {
      mockUpsertSectionFail(testInternalId, testRegId, EligibilitySectionId.repoKey, testEligibilitySubmissionData)(
        new Exception("")
      )

      intercept[Exception](await(service.updateEligibilityData(testInternalId, testRegId, eligibilityData)))
    }

    "encounter an exception if an error occurs" in new Setup {
      mockUpsertSection(testInternalId, testRegId, EligibilitySectionId.repoKey, testEligibilitySubmissionData)(
        Some(testEligibilitySubmissionData)
      )
      mockUpsertSectionFail(testInternalId, testRegId, EligibilityJsonSectionId.repoKey, eligibilityData)(
        new Exception("")
      )

      intercept[Exception](await(service.updateEligibilityData(testInternalId, testRegId, eligibilityData)))
    }
  }
}
