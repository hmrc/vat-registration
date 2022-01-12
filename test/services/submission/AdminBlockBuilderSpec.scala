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
import models.api.{AttachmentMethod, AttachmentType, Attachments, EmailMethod, IdentityEvidence, Post}
import models.submission.NETP
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class AdminBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture with MockAttachmentsService {

  object TestBuilder extends AdminBlockBuilder(mockRegistrationMongoRepository, mockAttachmentService)

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
      "both eligibility and trading details data are in the database" in {
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))
        when(mockRegistrationMongoRepository.retrieveTradingDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(validFullTradingDetails)))
        when(mockAttachmentService.getAttachmentList(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Set[AttachmentType]()))
        when(mockAttachmentService.getAttachmentDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(Attachments(Post))))

        val result = await(TestBuilder.buildAdminBlock(testRegId))

        result mustBe expectedJson
      }

      "both NETP eligibility and trading details data are in the database" in {
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(partyType = NETP))))
        when(mockRegistrationMongoRepository.retrieveTradingDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(validFullTradingDetails)))
        when(mockAttachmentService.getAttachmentList(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Set[AttachmentType](IdentityEvidence)))
        when(mockAttachmentService.getAttachmentDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(Attachments(Post))))

        val result = await(TestBuilder.buildAdminBlock(testRegId))

        result mustBe expectedNetpJson
      }

      "NETP the attachment method is Email" in {
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(partyType = NETP))))
        when(mockRegistrationMongoRepository.retrieveTradingDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(validFullTradingDetails)))
        when(mockAttachmentService.getAttachmentList(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Set[AttachmentType](IdentityEvidence)))
        when(mockAttachmentService.getAttachmentDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(Attachments(EmailMethod))))

        val result = await(TestBuilder.buildAdminBlock(testRegId))

        result mustBe expectedNetpJson
      }
    }

    "throw an exception" when {
      "the eligibility data is missing from the database" in {
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(None))
        when(mockRegistrationMongoRepository.retrieveTradingDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(validFullTradingDetails)))
        when(mockAttachmentService.getAttachmentList(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Set[AttachmentType]()))
        when(mockAttachmentService.getAttachmentDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(Attachments(Post))))

        intercept[InternalServerException] {
          await(TestBuilder.buildAdminBlock(testRegId))
        }
      }

      "the trading details data is missing from the database" in {
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))
        when(mockRegistrationMongoRepository.retrieveTradingDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(None))
        when(mockAttachmentService.getAttachmentList(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Set[AttachmentType]()))
        when(mockAttachmentService.getAttachmentDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(Attachments(Post))))

        intercept[InternalServerException] {
          await(TestBuilder.buildAdminBlock(testRegId))
        }
      }

      "there is no eligibility data or trading details data in the database" in {
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(None))
        when(mockRegistrationMongoRepository.retrieveTradingDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(None))
        when(mockAttachmentService.getAttachmentList(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Set[AttachmentType]()))
        when(mockAttachmentService.getAttachmentDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(Attachments(Post))))

        intercept[InternalServerException] {
          await(TestBuilder.buildAdminBlock(testRegId))
        }
      }
    }
  }
}
