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

package services

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.MockVatSchemeRepository
import models.GroupRegistration
import models.api._
import models.submission.{LtdLiabilityPartnership, NETP, Partnership}
import play.api.test.Helpers._

import scala.concurrent.Future

class AttachmentsServiceSpec extends VatRegSpec with VatRegistrationFixture with MockVatSchemeRepository {

  object Service extends AttachmentsService(mockVatSchemeRepository)

  val attachmentsKey = "attachments"
  val netpEligibilityData = testEligibilitySubmissionData.copy(partyType = NETP)
  val testNetpVatScheme = testVatScheme.copy(eligibilitySubmissionData = Some(netpEligibilityData))
  val testUnverifiedUserVatScheme = testFullVatScheme.copy(
    applicantDetails = Some(unverifiedUserApplicantDetails)
  )
  val partnershipEligibilityData = testEligibilitySubmissionData.copy(partyType = Partnership)
  val testPartnershipVatScheme = testVatScheme.copy(eligibilitySubmissionData = Some(partnershipEligibilityData))
  val llpEligibilityData = testEligibilitySubmissionData.copy(partyType = LtdLiabilityPartnership)
  val testLlpVatScheme = testVatScheme.copy(eligibilitySubmissionData = Some(llpEligibilityData))
  val vatGroupEligibilityData = testEligibilitySubmissionData.copy(registrationReason = GroupRegistration)
  val testVatGroupVatScheme = testVatScheme.copy(eligibilitySubmissionData = Some(vatGroupEligibilityData))

  "getAttachmentsList" when {
    "attachments are required" must {
      "return a list of the required attachments for a NETP" in {
        mockGetVatScheme(testRegId)(Some(testNetpVatScheme))

        val res = await(Service.getAttachmentList(testRegId))

        res mustBe Set(IdentityEvidence)
      }

      "return a list of the required attachments for a UKCompany with unmatched personal details" in {
        mockGetVatScheme(testRegId)(Some(testUnverifiedUserVatScheme))

        val res = await(Service.getAttachmentList(testRegId))

        res mustBe Set(IdentityEvidence)
      }

      "return VAT2 in the attachment list for a Partnership" in {
        mockGetVatScheme(testRegId)(Some(testPartnershipVatScheme))

        val res = await(Service.getAttachmentList(testRegId))

        res mustBe Set(VAT2)
      }

      "return VAT51 in the attachment list fot a Group Registration" in {
        mockGetVatScheme(testRegId)(Some(testVatGroupVatScheme))

        val res = await(Service.getAttachmentList(testRegId))

        res mustBe Set(VAT51)
      }
    }
    "attachments are not required" must {
      "return an empty list" in {
        mockGetVatScheme(testRegId)(Some(testVatScheme))

        val res = await(Service.getAttachmentList(testRegId))

        res mustBe Set.empty
      }

      "not return VAT2 in the attachment list for a Limited Liability Partnership" in {
        mockGetVatScheme(testRegId)(Some(testLlpVatScheme))

        val res = await(Service.getAttachmentList(testRegId))

        res mustBe Set.empty
      }
    }
  }

  "getAttachmentDetails" when {
    "the VatScheme has no attachmentDetails" should {
      "return None" in {
        mockFetchBlock[Attachments](testRegId, attachmentsKey)(Future.successful(None))

        val res = await(Service.getAttachmentDetails(testRegId))

        res mustBe None
      }
    }
    "the VatScheme contains the Post attachment method" should {
      "return the correct attachment details" in {
        mockFetchBlock[Attachments](testRegId, attachmentsKey)(Future.successful(Some(Attachments(Post))))

        val res = await(Service.getAttachmentDetails(testRegId))

        res mustBe Some(Attachments(Post))
      }
    }
    "the VatScheme contains the Attached attachment method" should {
      "return the correct attachment details" in {
        mockFetchBlock[Attachments](testRegId, attachmentsKey)(Future.successful(Some(Attachments(Attached))))

        val res = await(Service.getAttachmentDetails(testRegId))

        res mustBe Some(Attachments(Attached))
      }
    }
    "the VatScheme contains the Other attachment method" should {
      "return the correct attachment details" in {
        mockFetchBlock[Attachments](testRegId, attachmentsKey)(Future.successful(Some(Attachments(Other))))

        val res = await(Service.getAttachmentDetails(testRegId))

        res mustBe Some(Attachments(Other))
      }
    }
  }

  "storeAttachmentDetails" must {
    "store the given attachment method" in {
      val testAttachmentDetails = Attachments(Post)
      mockUpdateBlock[Attachments](testRegId, testAttachmentDetails, attachmentsKey)

      val res = await(Service.storeAttachmentDetails(testRegId, testAttachmentDetails))

      res mustBe testAttachmentDetails
    }
  }

}
