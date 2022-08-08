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
import mocks.MockAttachmentsService
import models.api._
import models.submission.NETP
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class AdminBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture with MockAttachmentsService {

  object TestBuilder extends AdminBlockBuilder(mockAttachmentService)

  override def beforeEach(): Unit = {
    reset(mockAttachmentService)
    super.afterEach()
  }

  val expectedJson: JsObject =
    Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2"
      ),
      "attachments" -> Json.obj(
        "EORIrequested" -> true
      )
    )

  val expectedNetpJson: JsObject =
    Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2",
        "overseasTrader" -> true
      ),
      "attachments" -> Json.obj(
        "EORIrequested" -> true,
        "identityEvidence" -> Json.toJson[AttachmentMethod](Post)
      )
    )

  "buildAdminBlock" should {
    "return an admin block json object" when {
      "both eligibility and vat application details data are in the database" in {
        when(mockAttachmentService.getAttachmentList(ArgumentMatchers.eq(testInternalId), ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Set[AttachmentType]()))

        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = Some(testEligibilitySubmissionData),
          vatApplication = Some(testVatApplicationDetails)
        )

        val result = TestBuilder.buildAdminBlock(vatScheme)

        result mustBe expectedJson
      }

      "both NETP eligibility and vat application details data are in the database" in {
        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = NETP)),
          vatApplication = Some(testVatApplicationDetails),
          attachments = Some(Attachments(Post))
        )

        when(mockAttachmentService.attachmentList(ArgumentMatchers.eq(vatScheme)))
          .thenReturn(Set[AttachmentType](IdentityEvidence))

        val result = TestBuilder.buildAdminBlock(vatScheme)

        result mustBe expectedNetpJson
      }

      "NETP the attachment method is Email" in {
        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = NETP)),
          vatApplication = Some(testVatApplicationDetails),
          attachments = Some(Attachments(EmailMethod))
        )

        when(mockAttachmentService.attachmentList(ArgumentMatchers.eq(vatScheme)))
          .thenReturn(Set[AttachmentType](IdentityEvidence))

        val result = TestBuilder.buildAdminBlock(vatScheme)

        result mustBe expectedNetpJson
      }
    }

    "throw an exception" when {
      "the eligibility data is missing from the database" in {
        when(mockAttachmentService.getAttachmentList(ArgumentMatchers.eq(testInternalId), ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Set[AttachmentType]()))

        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = None,
          vatApplication = Some(testVatApplicationDetails),
          attachments = Some(Attachments(Post))
        )

        intercept[InternalServerException] {
          TestBuilder.buildAdminBlock(vatScheme)
        }
      }

      "the vat application details data is missing from the database" in {
        when(mockAttachmentService.getAttachmentList(ArgumentMatchers.eq(testInternalId), ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Set[AttachmentType]()))

        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = Some(testEligibilitySubmissionData),
          vatApplication = None,
          attachments = Some(Attachments(Post))
        )

        intercept[InternalServerException] {
          TestBuilder.buildAdminBlock(vatScheme)
        }
      }

      "there is no eligibility data or trading details data in the database" in {
        when(mockAttachmentService.getAttachmentList(ArgumentMatchers.eq(testInternalId), ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Set[AttachmentType]()))

        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = None,
          attachments = Some(Attachments(Post)),
          business = None
        )

        intercept[InternalServerException] {
          TestBuilder.buildAdminBlock(vatScheme)
        }
      }
    }
  }
}
