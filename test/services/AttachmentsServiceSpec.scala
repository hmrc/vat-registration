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

package services

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.MockRegistrationRepository
import models.api._
import models.submission.NETP
import play.api.test.Helpers._

import scala.concurrent.Future

class AttachmentsServiceSpec extends VatRegSpec with VatRegistrationFixture with MockRegistrationRepository {

  object Service extends AttachmentsService(mockRegistrationRepository)

  val attachmentsKey = "attachments"
  val netpEligibilityData = testEligibilitySubmissionData.copy(partyType = NETP)
  val testNetpVatScheme = testVatScheme.copy(eligibilitySubmissionData = Some(netpEligibilityData))

  "getAttachmentsList" when {
    "attachments are required" must {
      "return a list of the required attachments" in {
        mockGetVatScheme(testRegId)(Some(testNetpVatScheme))

        val res = await(Service.getAttachmentList(testRegId))

        res mustBe List(IdentityEvidence)
      }
    }
    "attachments are not required" must {
      "return an empty list" in {
        mockGetVatScheme(testRegId)(Some(testVatScheme))

        val res = await(Service.getAttachmentList(testRegId))

        res mustBe Nil
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
