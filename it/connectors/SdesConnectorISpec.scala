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
import connectors.stubs.SdesNotifyStub.stubSdesNotification
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

  val testPayload: SdesNotification = testSdesPayload(testReference)

  "notifySdes" when {
    "SDES returns NO_CONTENT" must {
      "return SdesNotificationSuccess" in {
        disable(StubSubmission)
        stubAudit(OK)
        stubMergedAudit(OK)
        stubSdesNotification(Json.toJson(testPayload))(NO_CONTENT)

        val result = await(connector.notifySdes(testPayload))

        result mustBe SdesNotificationSuccess
      }
    }

    "SDES returns an unexpected response" must {
      "return SdesNotificationFailure" in {
        disable(StubSubmission)
        stubAudit(OK)
        stubMergedAudit(OK)
        stubSdesNotification(Json.toJson(testPayload))(BAD_REQUEST)

        val result = await(connector.notifySdes(testPayload))

        result mustBe SdesNotificationFailure(BAD_REQUEST, "")
      }
    }
  }
}
