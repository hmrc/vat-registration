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

import config.BackendConfig
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.monitoring.MockAuditService
import mocks.{MockSdesConnector, MockUpscanMongoRepository}
import models.api.{PrimaryIdentityEvidence, Ready, UploadDetails, UpscanDetails}
import models.nonrepudiation.NonRepudiationAuditing.{NonRepudiationAttachmentFailureAudit, NonRepudiationAttachmentSuccessAudit}
import models.nonrepudiation.{NonRepudiationAttachment, NonRepudiationAttachmentAccepted, NonRepudiationAttachmentFailed}
import models.sdes.PropertyExtractor._
import models.sdes.SdesAuditing.{SdesCallbackFailureAudit, SdesCallbackNotSentToNrsAudit, SdesFileSubmissionAudit}
import models.sdes._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatest.concurrent.Eventually.eventually
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.IdGenerator

import java.time.LocalDateTime
import scala.concurrent.Future

class SdesServiceSpec extends VatRegSpec with VatRegistrationFixture with MockUpscanMongoRepository with MockSdesConnector with MockAuditService {

  val testCorrelationid = "testCorrelationid"

  object TestIdGenerator extends IdGenerator {
    override def createId: String = testCorrelationid
  }

  implicit val appConfig: BackendConfig = app.injector.instanceOf[BackendConfig]

  object TestService extends SdesService(
    mockSdesConnector,
    mockNonRepudiationConnector,
    mockUpscanMongoRepository,
    mockAuditService,
    TestIdGenerator
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: Request[AnyContent] = FakeRequest()

  val testNotificationType = "FileReceived"
  val testReference = "testReference1"
  val testReference2 = "testReference2"
  val testReference3 = "testReference3"
  val testDownloadUrl = "testDownloadUrl"
  val testFileName = "testFileName"
  val testMimeType = "testMimeType"
  val testTimeStamp: LocalDateTime = LocalDateTime.now()
  val testChecksum = "1234567890"
  val testSize = 123
  val testFormBundleId = "123412341234"
  val testInfoType = "1655996667080"
  val testRecipientOrSender = "400063095160"
  val testNrsId = "testNrsId"

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

  def testPayload(attachmentReference: String, nrsKey: Option[String], index: Int): SdesNotification = SdesNotification(
    informationType = testInfoType,
    file = FileDetails(
      recipientOrSender = testRecipientOrSender,
      name = s"$testFormBundleId-$index-$testFileName",
      location = testDownloadUrl,
      checksum = Checksum(
        algorithm = checksumAlgorithm,
        value = testChecksum
      ),
      size = testSize,
      properties = List(
        Property(
          name = locationKey,
          value = testDownloadUrl
        ),
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

  def testCallback(optFailureReason: Option[String], notificationType: String = testNotificationType): SdesCallback = SdesCallback(
    notification = notificationType,
    filename = s"$testFormBundleId-$testFileName",
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
      referenceList.zipWithIndex.map { case (reference, index) =>
        mockNotifySdes(testPayload(reference, Some(testNrsId), index), Future.successful(SdesNotificationSuccess(NO_CONTENT, "")))
      }

      val result = await(TestService.notifySdes(testRegId, testFormBundleId, Some(testNrsId), testProviderId))
      result mustBe Seq(SdesNotificationSuccess(NO_CONTENT, ""), SdesNotificationSuccess(NO_CONTENT, ""), SdesNotificationSuccess(NO_CONTENT, ""))

      eventually {
        verifyAudit(SdesFileSubmissionAudit(testPayload(testReference, Some(testNrsId), 0), SdesNotificationSuccess(NO_CONTENT, ""), testProviderId))
        verifyAudit(SdesFileSubmissionAudit(testPayload(testReference2, Some(testNrsId), 1), SdesNotificationSuccess(NO_CONTENT, ""), testProviderId))
        verifyAudit(SdesFileSubmissionAudit(testPayload(testReference3, Some(testNrsId), 2), SdesNotificationSuccess(NO_CONTENT, ""), testProviderId))
      }
    }

    "create a payload without nrsSubmissionId and send it" in {
      val referenceList = Seq(testReference, testReference2, testReference3)
      mockGetAllUpscanDetails(testRegId)(Future.successful(referenceList.map(testUpscanDetails)))
      referenceList.zipWithIndex.map { case (reference, index) =>
        mockNotifySdes(testPayload(reference, None, index), Future.successful(SdesNotificationSuccess(NO_CONTENT, "")))
      }

      val result = await(TestService.notifySdes(testRegId, testFormBundleId, None, testProviderId))

      result mustBe Seq(SdesNotificationSuccess(NO_CONTENT, ""), SdesNotificationSuccess(NO_CONTENT, ""), SdesNotificationSuccess(NO_CONTENT, ""))

      eventually {
        verifyAudit(SdesFileSubmissionAudit(testPayload(testReference, None, 0), SdesNotificationSuccess(NO_CONTENT, ""), testProviderId))
        verifyAudit(SdesFileSubmissionAudit(testPayload(testReference2, None, 1), SdesNotificationSuccess(NO_CONTENT, ""), testProviderId))
        verifyAudit(SdesFileSubmissionAudit(testPayload(testReference3, None, 2), SdesNotificationSuccess(NO_CONTENT, ""), testProviderId))
      }
    }
  }

  "processCallback" must {
    "call and audit NRS success if callback is successful" in {
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
      )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(request))
      ).thenReturn(Future.successful(NonRepudiationAttachmentAccepted(testNrAttachmentId)))

      await(TestService.processCallback(testCallback(None)))

      eventually {
        verify(mockNonRepudiationConnector).submitAttachmentNonRepudiation(
          ArgumentMatchers.eq(testNrsPayload)
        )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(request))
        verifyAudit(NonRepudiationAttachmentSuccessAudit(testCallback(None), testNrAttachmentId))
      }
    }

    "Ignore any callbacks other than 'FileReceived'" in {
      val fileProcessed = "FileProcessed"

      await(TestService.processCallback(testCallback(optFailureReason = None, notificationType = fileProcessed)))

      eventually(verifyNoInteractions(mockNonRepudiationConnector))
    }

    "audit FileProcessed callbacks which are not sent to NRS" in {

      val fileProcessed = "FileProcessed"

      val testFileProcessedCallback = testCallback(optFailureReason = None, notificationType = fileProcessed)

      await(TestService.processCallback(testFileProcessedCallback))

      eventually {
        verifyNoInteractions(mockNonRepudiationConnector)
        verifyAudit(SdesCallbackNotSentToNrsAudit(testFileProcessedCallback))
      }
    }

    "call and audit NRS failure if callback is successful" in {
      val testNrsPayload = NonRepudiationAttachment(
        attachmentUrl = testDownloadUrl,
        attachmentId = testReference,
        attachmentSha256Checksum = testChecksum,
        attachmentContentType = testMimeType,
        nrSubmissionId = testNrsId
      )

      when(mockNonRepudiationConnector.submitAttachmentNonRepudiation(
        ArgumentMatchers.eq(testNrsPayload)
      )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(request))
      ).thenReturn(Future.successful(NonRepudiationAttachmentFailed("", BAD_REQUEST)))

      await(TestService.processCallback(testCallback(None)))

      eventually {
        verify(mockNonRepudiationConnector).submitAttachmentNonRepudiation(
          ArgumentMatchers.eq(testNrsPayload)
        )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(request))
        verifyAudit(NonRepudiationAttachmentFailureAudit(testCallback(None), BAD_REQUEST))
      }
    }

    "audit a failure callback" in {
      await(TestService.processCallback(testCallback(Some(testFailureReason))))

      eventually {
        verifyAudit(SdesCallbackFailureAudit(testCallback(Some(testFailureReason))))
      }
    }
  }

  "normaliseFileName" must {
    val testFormBundleId = "099000109175"
    val testIndex = 1

    "return an already valid fileName unchanged" in {
      val testExtension = ".docx"
      val testMimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
      val testFileName = "-+()$ _1234567890qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM"
      val fullName = s"$testFormBundleId-$testIndex-$testFileName$testExtension"

      TestService.normaliseFileName(fullName, testMimeType) mustBe fullName
    }

    "replace extension not matching mimetype and return a valid fileName" in {
      val testExtension = ".docx"
      val testFileName = "testFileName"
      val testXlsxMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      val testXlsxExtension = ".xlsx"
      val fullName = s"$testFormBundleId-$testIndex-$testFileName$testExtension"

      val updatedFullName = s"$testFormBundleId-$testIndex-$testFileName$testXlsxExtension"

      TestService.normaliseFileName(fullName, testXlsxMimeType) mustBe updatedFullName
    }

    "return a valid fileName with a lowercase extension if mimetype could not be matched" in {
      val testExtension = ".docx"
      val testFileName = "testFileName"
      val testUnknownMimeType = "unknown"
      val fullName = s"$testFormBundleId-$testIndex-$testFileName${testExtension.toUpperCase}"

      val updatedFullName = s"$testFormBundleId-$testIndex-$testFileName${testExtension.toLowerCase}"

      TestService.normaliseFileName(fullName, testUnknownMimeType) mustBe updatedFullName
    }

    "strip invalid characters and return a valid fileName" in {
      val testExtension = ".pdf"
      val testMimeType = "application/pdf"
      val testFileName = "testFileName"
      val invalidCharacters = "\'\"&^%£@!?*{}[]=€#<>/\\|:;’”‘“.,йцукенгшщздлорпавыфячсмитьбюхъжэ"
      val fullName = s"$testFormBundleId-$testIndex-$testFileName$invalidCharacters$testExtension"

      val strippedFullName = s"$testFormBundleId-$testIndex-$testFileName$testExtension"

      TestService.normaliseFileName(fullName, testMimeType) mustBe strippedFullName
    }

    "truncate a fileName exceeding 99 characters and return a valid fileName" in {
      val testExtension = ".jpeg"
      val testMimeType = "image/jpeg"
      val originalLength = 200
      val testFileName = "a" * originalLength
      val fullName = s"$testFormBundleId-$testIndex-$testFileName$testExtension"

      val allowedLength = 99 - s"$testFormBundleId-$testIndex-".length
      val truncatedFileName = "a" * allowedLength
      val truncatedFullName = s"$testFormBundleId-$testIndex-$truncatedFileName$testExtension"

      TestService.normaliseFileName(fullName, testMimeType) mustBe truncatedFullName
      TestService.normaliseFileName(fullName, testMimeType).length mustBe 99 + testExtension.length
    }
  }
}
