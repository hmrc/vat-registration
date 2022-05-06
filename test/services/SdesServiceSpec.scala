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
import mocks.monitoring.MockAuditService
import mocks.{MockSdesConnector, MockUpscanMongoRepository}
import models.api.{PrimaryIdentityEvidence, Ready, UploadDetails, UpscanDetails}
import models.nonrepudiation.{NonRepudiationAttachment, NonRepudiationAttachmentAccepted}
import models.sdes.PropertyExtractor._
import models.sdes.SdesAuditing.{SdesCallbackFailureAudit, SdesFileSubmissionAudit}
import models.sdes._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.Eventually.eventually
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.Future

class SdesServiceSpec extends VatRegSpec with VatRegistrationFixture with MockUpscanMongoRepository with MockSdesConnector with MockAuditService {

  object TestService extends SdesService(
    mockSdesConnector,
    mockNonRepudiationConnector,
    mockUpscanMongoRepository,
    mockAuditService
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[AnyContent] = FakeRequest()

  val testReference = "testReference"
  val testReference2 = "testReference2"
  val testReference3 = "testReference3"
  val testDownloadUrl = "testDownloadUrl"
  val testFileName = "testFileName"
  val testMimeType = "testMimeType"
  val testTimeStamp: LocalDateTime = LocalDateTime.now()
  val testChecksum = "1234567890"
  val testSize = 123
  val testFormBundleId = "123412341234"
  val testNrsId = "testNrsId"
  val testCorrelationid = "testCorrelationid"

  def testUpscanDetails(reference: String): UpscanDetails = UpscanDetails(
    Some(testRegId),
    reference,
    Some(PrimaryIdentityEvidence),
    Some(testDownloadUrl),
    Ready,
    Some(UploadDetails(
      fileName = testFileName,
      fileMimeType = testMimeType,
      uploadTimestamp = testTimeStamp,
      checksum = testChecksum,
      size = testSize
    )),
    None
  )

  def testPayload(attachmentReference: String, nrsKey: Option[String]): SdesNotification = SdesNotification(
    informationType = "S18",
    file = FileDetails(
      recipientOrSender = "123456789012",
      name = testFileName,
      location = testDownloadUrl,
      checksum = Checksum(
        algorithm = checksumAlgorithm,
        value = testChecksum
      ),
      size = testSize,
      properties = List(
        Property(
          name = mimeTypeKey,
          value = testMimeType
        ),
        Property(
          name = prefixedFormBundleKey,
          value = s"VRS$testFormBundleId"
        ),
        Property(
          name = formBundleKey,
          value = testFormBundleId
        ),
        Property(
          name = attachmentReferenceKey,
          value = attachmentReference
        ),
        Property(
          name = submissionDateKey,
          value = testTimeStamp.format(dateTimeFormatter)
        )
      ) ++ nrsKey.map(id => Property(
        name = nrsSubmissionKey,
        value = id
      ))
    ),
    audit = AuditDetals(
      correlationID = testCorrelationid
    )
  )

  val testFailureReason = "testFailureReason"

  def testCallback(optFailureReason: Option[String]): SdesCallback = SdesCallback(
    notification = testReference,
    filename = testFileName,
    correlationID = testCorrelationid,
    dateTime = testDateTime,
    checksumAlgorithm = Some(checksumAlgorithm),
    checksum = Some(testChecksum),
    availableUntil = Some(testDateTime),
    properties = List(
      Property(
        name = mimeTypeKey,
        value = testMimeType
      ),
      Property(
        name = formBundleKey,
        value = testFormBundleId
      ),
      Property(
        name = attachmentReferenceKey,
        value = testReference
      ),
      Property(
        name = locationKey,
        value = testDownloadUrl
      ),
      Property(
        name = nrsSubmissionKey,
        value = testNrsId
      )),
    failureReason = optFailureReason
  )

  "notifySdes" must {
    "create a payload for every upscanDetails in repository and send it" in {
      val referenceList = Seq(testReference, testReference2, testReference3)
      mockGetAllUpscanDetails(testRegId)(Future.successful(referenceList.map(testUpscanDetails)))
      referenceList.map(reference =>
        mockNotifySdes(testPayload(reference, Some(testNrsId)), Future.successful(SdesNotificationSuccess(NO_CONTENT, "")))
      )

      val result = await(TestService.notifySdes(testRegId, testFormBundleId, testCorrelationid, Some(testNrsId)))

      result mustBe Seq(SdesNotificationSuccess(NO_CONTENT, ""), SdesNotificationSuccess(NO_CONTENT, ""), SdesNotificationSuccess(NO_CONTENT, ""))

      eventually {
        verifyAudit(SdesFileSubmissionAudit(testPayload(testReference, Some(testNrsId)), SdesNotificationSuccess(NO_CONTENT, "")))
        verifyAudit(SdesFileSubmissionAudit(testPayload(testReference2, Some(testNrsId)), SdesNotificationSuccess(NO_CONTENT, "")))
        verifyAudit(SdesFileSubmissionAudit(testPayload(testReference3, Some(testNrsId)), SdesNotificationSuccess(NO_CONTENT, "")))
      }
    }

    "create a payload without nrsSubmissionId and send it" in {
      val referenceList = Seq(testReference, testReference2, testReference3)
      mockGetAllUpscanDetails(testRegId)(Future.successful(referenceList.map(testUpscanDetails)))
      referenceList.map(reference =>
        mockNotifySdes(testPayload(reference, None), Future.successful(SdesNotificationSuccess(NO_CONTENT, "")))
      )

      val result = await(TestService.notifySdes(testRegId, testFormBundleId, testCorrelationid, None))

      result mustBe Seq(SdesNotificationSuccess(NO_CONTENT, ""), SdesNotificationSuccess(NO_CONTENT, ""), SdesNotificationSuccess(NO_CONTENT, ""))

      eventually {
        verifyAudit(SdesFileSubmissionAudit(testPayload(testReference, None), SdesNotificationSuccess(NO_CONTENT, "")))
        verifyAudit(SdesFileSubmissionAudit(testPayload(testReference2, None), SdesNotificationSuccess(NO_CONTENT, "")))
        verifyAudit(SdesFileSubmissionAudit(testPayload(testReference3, None), SdesNotificationSuccess(NO_CONTENT, "")))
      }
    }
  }

  "processCallback" must {
    "call NRS if callback is successful" in {
      val testNrAttachmentId = "testNrAttachmentId"
      val testNrsPayload = NonRepudiationAttachment(
        attachmentUrl = testDownloadUrl,
        attachmentId = testReference,
        attachmentSha256Checksum = testChecksum,
        attachmentContentType = testMimeType,
        nrSubmissionId = testNrsId
      )

      when(mockNonRepudiationConnector.submitAttachmentNonRepudiation(
        ArgumentMatchers.eq(testNrsPayload)
      )(ArgumentMatchers.eq(hc))
      ).thenReturn(Future.successful(NonRepudiationAttachmentAccepted(testNrAttachmentId)))

      val res: Unit = await(TestService.processCallback(testCallback(None)))

      eventually {
        verify(mockNonRepudiationConnector).submitAttachmentNonRepudiation(
          ArgumentMatchers.eq(testNrsPayload)
        )(ArgumentMatchers.eq(hc))
      }
    }

    "audit a failure callback" in {
      val res: Unit = await(TestService.processCallback(testCallback(Some(testFailureReason))))

      eventually {
        verifyAudit(SdesCallbackFailureAudit(testCallback(Some(testFailureReason))))
      }
    }
  }
}
