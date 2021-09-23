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

package models.monitoring

import enums.VatRegStatus
import fixtures.SubmissionAuditFixture
import helpers.VatRegSpec
import models.api.{MTDfB, VatScheme}
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate

class SubmissionAuditModelSpec extends VatRegSpec with SubmissionAuditFixture {

  val rootBlockTestVatScheme = VatScheme(
    id = testRegId,
    internalId = testInternalId,
    status = VatRegStatus.draft,
    eligibilitySubmissionData = Some(testEligibilitySubmissionData),
    tradingDetails = Some(validFullTradingDetails),
    applicantDetails = Some(validApplicantDetails),
    returns = Some(testReturns)
  )

  def model(vatScheme: VatScheme): SubmissionAuditModel = SubmissionAuditModel(
    userAnswers = Json.obj("user" -> "answers"),
    vatScheme = vatScheme,
    affinityGroup = Organisation,
    authProviderId = testProviderId,
    optAgentReferenceNumber = None,
    formBundleId = testFormBundleId
  )

  "buildRootBlock" when {
    "All required blocks are present in the vat scheme" when {
      "forward look" should {
        "return the root JSON block when BP Safe ID is missing" in {

          val res = model(rootBlockTestVatScheme)

          res.detail mustBe Json.obj(
            "authProviderId" -> "testProviderID",
            "journeyId" -> "testRegId",
            "userType" -> "Organisation",
            "formBundleId" -> "testFormBundleId",
            "registrationReason" -> "Forward Look",
            "registrationRelevantDate" -> "2020-10-07",
            "messageType" -> "SubscriptionCreate",
            "customerStatus" -> MTDfB.toString,
            "eoriRequested" -> true,
            "corporateBodyRegistered" -> Json.obj(
              "dateOfIncorporation" -> testDateOFIncorp,
              "countryOfIncorporation" -> "GB"
            ),
            "idsVerificationStatus" -> "3",
            "cidVerification" -> "1",
            "userEnteredDetails" -> Json.obj(
              "user" -> "answers"
            )
          )
        }
        "return the root JSON block when BP Safe ID is present" in {
          val applicantDetailsWithSafeId = validApplicantDetails.copy(entity = testLtdCoEntity.copy(bpSafeId = Some(testBpSafeId)))
          val res = model(rootBlockTestVatScheme.copy(applicantDetails = Some(applicantDetailsWithSafeId)))

          res.detail mustBe Json.obj(
            "authProviderId" -> "testProviderID",
            "journeyId" -> "testRegId",
            "userType" -> "Organisation",
            "formBundleId" -> "testFormBundleId",
            "registrationReason" -> "Forward Look",
            "registrationRelevantDate" -> "2020-10-07",
            "messageType" -> "SubscriptionCreate",
            "customerStatus" -> MTDfB.toString,
            "eoriRequested" -> true,
            "corporateBodyRegistered" -> Json.obj(
              "dateOfIncorporation" -> testDateOFIncorp,
              "countryOfIncorporation" -> "GB"
            ),
            "idsVerificationStatus" -> "0",
            "cidVerification" -> "1",
            "businessPartnerReference" -> testBpSafeId,
            "userEnteredDetails" -> Json.obj(
              "user" -> "answers"
            )
          )
        }
      }
      "backward look" should {
        "return the correct json" in {
          val threshold = testMandatoryThreshold.copy(
            thresholdNextThirtyDays = Some(LocalDate.of(2021, 1, 12)),
            thresholdPreviousThirtyDays = Some(LocalDate.of(2021, 1, 12))
          )

          val eligibilityData = testEligibilitySubmissionData.copy(threshold = threshold)
          val res = model(rootBlockTestVatScheme.copy(eligibilitySubmissionData = Some(eligibilityData)))

          res.detail mustBe Json.obj(
            "authProviderId" -> "testProviderID",
            "journeyId" -> "testRegId",
            "userType" -> "Organisation",
            "formBundleId" -> "testFormBundleId",
            "registrationReason" -> "Backward Look",
            "registrationRelevantDate" -> "2020-12-01",
            "userType" -> "Organisation",
            "messageType" -> "SubscriptionCreate",
            "customerStatus" -> MTDfB.toString,
            "eoriRequested" -> true,
            "corporateBodyRegistered" -> Json.obj(
              "dateOfIncorporation" -> testDateOFIncorp,
              "countryOfIncorporation" -> "GB"
            ),
            "idsVerificationStatus" -> "3",
            "cidVerification" -> "1",
            "userEnteredDetails" -> Json.obj(
              "user" -> "answers"
            )
          )
        }
      }
      "registering voluntarily" should {
        "return the correct json" in {
          val eligibilityData = testEligibilitySubmissionData.copy(threshold = testVoluntaryThreshold)
          val res = model(rootBlockTestVatScheme.copy(eligibilitySubmissionData = Some(eligibilityData)))

          res.detail mustBe Json.obj(
            "authProviderId" -> "testProviderID",
            "journeyId" -> "testRegId",
            "userType" -> "Organisation",
            "formBundleId" -> "testFormBundleId",
            "registrationReason" -> "Voluntary",
            "registrationRelevantDate" -> "2018-01-01",
            "messageType" -> "SubscriptionCreate",
            "userType" -> "Organisation",
            "customerStatus" -> MTDfB.toString,
            "eoriRequested" -> true,
            "corporateBodyRegistered" -> Json.obj(
              "dateOfIncorporation" -> testDateOFIncorp,
              "countryOfIncorporation" -> "GB"
            ),
            "idsVerificationStatus" -> "3",
            "cidVerification" -> "1",
            "userEnteredDetails" -> Json.obj(
              "user" -> "answers"
            )
          )
        }
      }
    }
    "the eligibility submission data block is missing in the vat scheme" should {
      "throw an exception" in {
        intercept[InternalServerException] {
          model(rootBlockTestVatScheme.copy(eligibilitySubmissionData = None))
        }
      }
    }
    "the trading details block is missing in the vat scheme" should {
      "throw an exception" in {
        intercept[InternalServerException] {
          model(rootBlockTestVatScheme.copy(tradingDetails = None))
        }
      }
    }
    "the applicant details block is missing in the vat scheme" should {
      "throw an exception" in {
        intercept[InternalServerException] {
          model(rootBlockTestVatScheme.copy(applicantDetails = None))
        }
      }
    }
    "the returns block is missing in the vat scheme" should {
      "throw an exception" in {
        intercept[InternalServerException] {
          model(rootBlockTestVatScheme.copy(returns = None))
        }
      }
    }
  }

}
