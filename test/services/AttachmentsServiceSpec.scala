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
  val test1614aVatScheme = testVatScheme.copy(attachments = Some(Attachments(method = Some(Attached), supplyVat1614a = Some(true))))
  val test1614hVatScheme = testVatScheme.copy(attachments = Some(Attachments(method = Some(Attached), supplyVat1614h = Some(true))))
  val testSchemeWithTaxRepresentative = testVatScheme.copy(vatApplication = Some(testVatApplicationDetails.copy(hasTaxRepresentative = Some(true))))

  "getAttachmentsList" when {
    "attachments are required" must {

      "return a list of the required attachments for a UKCompany with unmatched personal details" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testUnverifiedUserVatScheme)))

        val res = await(Service.getAttachmentList(testInternalId, testRegId))

        res mustBe List(IdentityEvidence)
      }

      "return transactorIdentityEvidence in the attachment list fot a transactor with unverified personal details" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testUnverifiedTransactorVatScheme)))

        val res = await(Service.getAttachmentList(testInternalId, testRegId))

        res mustBe List(TransactorIdentityEvidence)
      }

      "return VAT2 in the attachment list for a Partnership" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testPartnershipVatScheme)))

        val res = await(Service.getAttachmentList(testInternalId, testRegId))

        res mustBe List(VAT2)
      }

      "return VAT2 in the attachment list if additional partners documents requested" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(
          testPartnershipVatScheme.copy(attachments = Some(Attachments(additionalPartnersDocuments = Some(true))))
        )))

        val res = await(Service.getAttachmentList(testInternalId, testRegId))

        res mustBe List(VAT2)
      }

      "not return VAT2 in the attachment list if no additional partners documents required" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(
          testPartnershipVatScheme.copy(attachments = Some(Attachments(additionalPartnersDocuments = Some(false))))
        )))

        val res = await(Service.getAttachmentList(testInternalId, testRegId))

        res mustBe Nil
      }

      "return VAT51 in the attachment list fot a Group Registration" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testVatGroupVatScheme)))

        val res = await(Service.getAttachmentList(testInternalId, testRegId))

        res mustBe List(VAT51)
      }

      "return VAT5L in the attachment list for a user with land and property" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testLnpVatScheme)))

        val res = await(Service.getAttachmentList(testInternalId, testRegId))

        res mustBe List(VAT5L)
      }

      "return VAT1TR in the attachment list if user has opted for tax representative" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testSchemeWithTaxRepresentative)))
        val res = await(Service.getAttachmentList(testInternalId, testRegId))
        res mustBe List(TaxRepresentativeAuthorisation)
      }
    }
    "attachments are not required" must {
      "return an empty list" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testVatScheme)))

        val res = await(Service.getAttachmentList(testInternalId, testRegId))

        res mustBe Nil
      }

      "not return VAT2 in the attachment list for a Limited Liability Partnership" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testLlpVatScheme)))

        val res = await(Service.getAttachmentList(testInternalId, testRegId))

        res mustBe Nil
      }
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
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testUnverifiedUserVatScheme)))
        mockGetAllUpscanDetails(testRegId)(Future.successful(Nil))

        val res = await(Service.getIncompleteAttachments(testInternalId, testRegId))

        res mustBe List(PrimaryIdentityEvidence, ExtraIdentityEvidence, ExtraIdentityEvidence)
      }

      "the user has some complete upscan details" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testUnverifiedUserVatScheme)))
        mockGetAllUpscanDetails(testRegId)(
          Future.successful(List(
            testUpscanDetails(PrimaryIdentityEvidence),
            testUpscanDetails(ExtraIdentityEvidence)
          ))
        )

        val res = await(Service.getIncompleteAttachments(testInternalId, testRegId))

        res mustBe List(ExtraIdentityEvidence)
      }
    }

    "return transactorIdentityEvidence in the attachment list fot a transactor with unverified personal details" when {
      "the user has no complete upscan details" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testUnverifiedTransactorVatScheme)))
        mockGetAllUpscanDetails(testRegId)(Future.successful(Nil))

        val res = await(Service.getIncompleteAttachments(testInternalId, testRegId))

        res mustBe List(PrimaryTransactorIdentityEvidence, ExtraTransactorIdentityEvidence, ExtraTransactorIdentityEvidence)
      }

      "the user has some complete upscan details" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testUnverifiedTransactorVatScheme)))
        mockGetAllUpscanDetails(testRegId)(
          Future.successful(List(
            testUpscanDetails(PrimaryTransactorIdentityEvidence),
            testUpscanDetails(ExtraTransactorIdentityEvidence)
          ))
        )

        val res = await(Service.getIncompleteAttachments(testInternalId, testRegId))

        res mustBe List(ExtraTransactorIdentityEvidence)
      }
    }
  }
}
