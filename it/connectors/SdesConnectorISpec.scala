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

package connectors

import connectors.stubs.AuditStub.{stubAudit, stubMergedAudit}
import connectors.stubs.SdesNotifyStub.stubNrsNotification
import featureswitch.core.config.{FeatureSwitching, StubSubmission}
import itutil.IntegrationStubbing
import models.sdes._
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT}
import play.api.libs.json.Json
import play.api.test.Helpers.{OK, await}
import services.SdesService._

import java.time.LocalDateTime

class SdesConnectorISpec extends IntegrationStubbing with FeatureSwitching {

  lazy val connector: SdesConnector = app.injector.instanceOf[SdesConnector]

  val testReference = "testReference"
  val testDownloadUrl = "testDownloadUrl"
  val testFileName = "testFileName"
  val testMimeType = "testMimeType"
  val testTimeStamp = LocalDateTime.now()
  val testChecksum = "1234567890"
  val testSize = 123
  val testFormBundleId = "123412341234"
  val testNrsKey = "testNrsKey"
  val testCorrelationid = "testCorrelationid"

  val testPayload: SdesNotification = SdesNotification(
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
          value = testReference
        ),
        Property(
          name = submissionDateKey,
          value = testTimeStamp.format(dateTimeFormatter)
        ),
        Property(
          name = nrsSubmissionKey,
          value = testNrsKey
        )
      )
    ),
    audit = AuditDetals(
      correlationID = testCorrelationid
    )
  )

  "notifySdes" when {
    "SDES returns NO_CONTENT" must {
      "return SdesNotificationSuccess" in {
        disable(StubSubmission)
        stubAudit(OK)
        stubMergedAudit(OK)
        stubNrsNotification(Json.toJson(testPayload))(NO_CONTENT)

        val result = await(connector.notifySdes(testPayload))

        result mustBe SdesNotificationSuccess
      }
    }

    "SDES returns an unexpected response" must {
      "return SdesNotificationFailure" in {
        disable(StubSubmission)
        stubAudit(OK)
        stubMergedAudit(OK)
        stubNrsNotification(Json.toJson(testPayload))(BAD_REQUEST)

        val result = await(connector.notifySdes(testPayload))

        result mustBe SdesNotificationFailure(BAD_REQUEST, "")
      }
    }
  }
}
