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
import mocks.{MockUpscanMongoRepository, MockVatSchemeRepository}
import models.GroupRegistration
import models.api._
import models.submission.{LtdLiabilityPartnership, Partnership}
import play.api.test.Helpers._

import scala.concurrent.Future

class AttachmentsServiceSpec extends VatRegSpec with VatRegistrationFixture with MockVatSchemeRepository with MockUpscanMongoRepository {

  object Service extends AttachmentsService(mockVatSchemeRepository, mockUpscanMongoRepository)

  val attachmentsKey = "attachments"
  val testUnverifiedUserVatScheme = testFullVatScheme.copy(
    applicantDetails = Some(unverifiedUserApplicantDetails)
  )
  val testUnverifiedTransactorVatScheme = testFullVatScheme.copy(
    transactorDetails = Some(validTransactorDetails.copy(
      personalDetails = validTransactorDetails.personalDetails.copy(
        identifiersMatch = false
      )
    )),
    eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(
      isTransactor = true
    ))
  )
  val partnershipEligibilityData = testEligibilitySubmissionData.copy(partyType = Partnership)
  val testPartnershipVatScheme = testVatScheme.copy(eligibilitySubmissionData = Some(partnershipEligibilityData))
  val llpEligibilityData = testEligibilitySubmissionData.copy(partyType = LtdLiabilityPartnership)
  val testLlpVatScheme = testVatScheme.copy(eligibilitySubmissionData = Some(llpEligibilityData))
  val vatGroupEligibilityData = testEligibilitySubmissionData.copy(registrationReason = GroupRegistration)
  val testVatGroupVatScheme = testVatScheme.copy(eligibilitySubmissionData = Some(vatGroupEligibilityData))
  val testLnpVatScheme = testVatScheme.copy(business = Some(testBusiness.copy(hasLandAndProperty = Some(true))))
  val testSchemeWithTaxRepresentative = testVatScheme.copy(vatApplication = Some(testVatApplicationDetails.copy(hasTaxRepresentative = Some(true))))

  "getAttachmentsList" when {
    "attachments are required" must {

      "return a list of the required attachments for a UKCompany with unmatched personal details" in {
        mockGetVatScheme(testRegId)(Some(testUnverifiedUserVatScheme))

        val res = await(Service.getAttachmentList(testRegId))

        res mustBe Set(IdentityEvidence)
      }

      "return transactorIdentityEvidence in the attachment list fot a transactor with unverified personal details" in {
        mockGetVatScheme(testRegId)(Some(testUnverifiedTransactorVatScheme))

        val res = await(Service.getAttachmentList(testRegId))

        res mustBe Set(TransactorIdentityEvidence)
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

      "return VAT5L in the attachment list fot a user with land and property" in {
        mockGetVatScheme(testRegId)(Some(testLnpVatScheme))

        val res = await(Service.getAttachmentList(testRegId))

        res mustBe Set(VAT5L)
      }

      "return VAT1TR in the attachment list if user has opted for tax representative" in {
        mockGetVatScheme(testRegId)(Some(testSchemeWithTaxRepresentative))
        val res = await(Service.getAttachmentList(testRegId))
        res mustBe Set(TaxRepresentativeAuthorisation)
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

  "getIncompleteAttachments" must {
    val testReference = "testReference"

    def testUpscanDetails(attachmentType: AttachmentType): UpscanDetails = UpscanDetails(
      registrationId = Some(testRegId),
      reference = testReference,
      attachmentType = Some(attachmentType),
      fileStatus = Ready
    )

    "return a list of the required attachments for a UKCompany with unmatched personal details" when {
      "the user has no complete upscan details" in {
        mockGetVatScheme(testRegId)(Some(testUnverifiedUserVatScheme))
        mockGetAllUpscanDetails(testRegId)(Future.successful(Nil))

        val res = await(Service.getIncompleteAttachments(testRegId))

        res mustBe List(PrimaryIdentityEvidence, ExtraIdentityEvidence, ExtraIdentityEvidence)
      }

      "the user has some complete upscan details" in {
        mockGetVatScheme(testRegId)(Some(testUnverifiedUserVatScheme))
        mockGetAllUpscanDetails(testRegId)(
          Future.successful(List(
            testUpscanDetails(PrimaryIdentityEvidence),
            testUpscanDetails(ExtraIdentityEvidence)
          ))
        )

        val res = await(Service.getIncompleteAttachments(testRegId))

        res mustBe List(ExtraIdentityEvidence)
      }
    }

    "return transactorIdentityEvidence in the attachment list fot a transactor with unverified personal details" when {
      "the user has no complete upscan details" in {
        mockGetVatScheme(testRegId)(Some(testUnverifiedTransactorVatScheme))
        mockGetAllUpscanDetails(testRegId)(Future.successful(Nil))

        val res = await(Service.getIncompleteAttachments(testRegId))

        res mustBe List(PrimaryTransactorIdentityEvidence, ExtraTransactorIdentityEvidence, ExtraTransactorIdentityEvidence)
      }

      "the user has some complete upscan details" in {
        mockGetVatScheme(testRegId)(Some(testUnverifiedTransactorVatScheme))
        mockGetAllUpscanDetails(testRegId)(
          Future.successful(List(
            testUpscanDetails(PrimaryTransactorIdentityEvidence),
            testUpscanDetails(ExtraTransactorIdentityEvidence)
          ))
        )

        val res = await(Service.getIncompleteAttachments(testRegId))

        res mustBe List(ExtraTransactorIdentityEvidence)
      }
    }
  }
}
