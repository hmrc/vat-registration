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
import mocks.{MockSdesConnector, MockUpscanMongoRepository}
import models.api.{Ready, UploadDetails, UpscanDetails}
import models.sdes._
import play.api.test.Helpers._
import services.SdesService._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.Future

class SdesServiceSpec extends VatRegSpec with VatRegistrationFixture with MockUpscanMongoRepository with MockSdesConnector {

  object TestService extends SdesService(
    mockSdesConnector,
    mockUpscanMongoRepository
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val testReference = "testReference"
  val testReference2 = "testReference2"
  val testReference3 = "testReference3"
  val testDownloadUrl = "testDownloadUrl"
  val testFileName = "testFileName"
  val testMimeType = "testMimeType"
  val testTimeStamp = LocalDateTime.now()
  val testChecksum = "1234567890"
  val testSize = 123
  val testFormBundleId = "123412341234"
  val testNrsKey = "testNrsKey"
  val testCorrelationid = "testCorrelationid"

  def testUpscanDetails(reference: String): UpscanDetails = UpscanDetails(
    Some(testRegId),
    reference,
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

  "notifySdes" must {
    "create a payload for every upscanDetails in repository and send it" in {
      val referenceList = Seq(testReference, testReference2, testReference3)
      mockGetAllUpscanDetails(testRegId)(Future.successful(referenceList.map(testUpscanDetails)))
      referenceList.map(reference =>
        mockNotifySdes(testPayload(reference, Some(testNrsKey)), Future.successful(SdesNotificationSuccess))
      )

      val result = await(TestService.notifySdes(testRegId, testFormBundleId, testCorrelationid, Some(testNrsKey)))

      result mustBe Seq(SdesNotificationSuccess, SdesNotificationSuccess, SdesNotificationSuccess)
    }

    "create a payload without nrsSubmissionId and send it" in {
      val referenceList = Seq(testReference, testReference2, testReference3)
      mockGetAllUpscanDetails(testRegId)(Future.successful(referenceList.map(testUpscanDetails)))
      referenceList.map(reference =>
        mockNotifySdes(testPayload(reference, None), Future.successful(SdesNotificationSuccess))
      )

      val result = await(TestService.notifySdes(testRegId, testFormBundleId, testCorrelationid, None))

      result mustBe Seq(SdesNotificationSuccess, SdesNotificationSuccess, SdesNotificationSuccess)
    }
  }
}
