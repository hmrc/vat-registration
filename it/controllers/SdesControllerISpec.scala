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

package controllers

import connectors.stubs.AuditStub.{stubAudit, stubMergedAudit}
import connectors.stubs.NonRepudiationStub.stubAttachmentNonRepudiationSubmission
import itutil.IntegrationStubbing
import models.nonrepudiation.NonRepudiationAttachment
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import java.util.UUID

class SdesControllerISpec extends IntegrationStubbing {

  val testNonRepudiationApiKey = "testNonRepudiationApiKey"
  override lazy val additionalConfig = Map("microservice.services.non-repudiation.api-key" -> testNonRepudiationApiKey)

  val url: String = controllers.routes.SdesController.sdesCallback.url

  val testNotification = "FileReady"
  val testFilename = "uploadedFilename.doc"
  val testChecksumAlgorithm = "SHA2"
  override val testChecksum = "23aab10f02dd6ca07bfdf270252904d754bcc844bf3ac1f52bbaa3b14126e266"
  val testCorrelationID: String = UUID.randomUUID().toString
  val testAvailableUntilString = "2021-01-06T10:01:00.889Z"
  val testFailureReason = "Virus Detected"
  val testDateTimeString = "2021-01-01T10:01:00.889Z"
  override val testMimeType = "application/pdf"
  val testNrsSubmissionId: String = UUID.randomUUID().toString
  val testAttachmentId: String = UUID.randomUUID().toString
  override val testFormBundleId = "1234123451234"
  val testLocation = "s3://bucketname/path/to/file/in/upscan"
  val testCallbackJson: JsObject = Json.obj(
    "notification" -> testNotification,
    "filename" -> s"$testFormBundleId-$testFilename",
    "checksumAlgorithm" -> testChecksumAlgorithm,
    "checksum" -> testChecksum,
    "correlationID" -> testCorrelationID,
    "availableUntil" -> testAvailableUntilString,
    "failureReason" -> testFailureReason,
    "dateTime" -> testDateTimeString,
    "properties" -> Json.arr(
      Json.obj(
        "name" -> "location",
        "value" -> testLocation
      ),
      Json.obj(
        "name" -> "mimeType",
        "value" -> testMimeType
      ),
      Json.obj(
        "name" -> "nrsSubmissionId",
        "value" -> testNrsSubmissionId
      ),
      Json.obj(
        "name" -> "attachmentId",
        "value" -> testAttachmentId
      ),
      Json.obj(
        "name" -> "formBundleId",
        "value" -> testFormBundleId
      ),
      Json.obj(
        "name" -> "location",
        "value" -> testLocation
      )
    )
  )

  val testNonRepudiationAttachmentId: String = UUID.randomUUID().toString
  val testNrsPayload: NonRepudiationAttachment = NonRepudiationAttachment(
    attachmentUrl = testLocation,
    attachmentId = testAttachmentId,
    attachmentSha256Checksum = testChecksum,
    attachmentContentType = testMimeType,
    nrSubmissionId = testNrsSubmissionId
  )

  s"POST $url" must {
    "return OK after successfully parsing the callback json and calling NRS" in new SetupHelper {
      stubAudit(OK)
      stubMergedAudit(OK)
      stubAttachmentNonRepudiationSubmission(
        Json.toJson(testNrsPayload),
        testNonRepudiationApiKey)(
        ACCEPTED,
        Json.obj("attachmentId" -> testNonRepudiationAttachmentId)
      )

      val res: WSResponse = await(client(url).post(testCallbackJson - "failureReason"))

      res.status mustBe ACCEPTED
    }

    "return OK and audit after successfully parsing the callback auditing a failure from SDES" in new SetupHelper {
      stubAudit(OK)
      stubMergedAudit(OK)

      val res: WSResponse = await(client(url).post(testCallbackJson))

      res.status mustBe ACCEPTED
    }
  }
}
