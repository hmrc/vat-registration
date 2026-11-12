/*
 * Copyright 2020 HM Revenue & Customs
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

import com.github.tomakehurst.wiremock.client.WireMock.{equalTo, equalToJson, matching, postRequestedFor, urlMatching, verify}
import config.BackendConfig
import connectors.stubs.AuditStub.{stubAudit, stubMergedAudit}
import connectors.stubs.VatSubmissionStub._
import featureswitch.core.config.{FeatureSwitching, StubSubmission}
import httpparsers.{VatSubmissionFailure, VatSubmissionSuccess}
import itutil.IntegrationStubbing
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.SessionId


class HipVatSubmissionConnectorISpec extends IntegrationStubbing with FeatureSwitching {

  implicit val request: Request[_] = FakeRequest()

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(StubSubmission)
  }

  lazy val connector: HipVatSubmissionConnector = app.injector.instanceOf[HipVatSubmissionConnector]
  lazy val backendConfig: BackendConfig = app.injector.instanceOf[BackendConfig]

  "submit" when {
    "the vat submission service returns a success" should {
      s"return VatSubmissionSuccess" in {
        val hcWithSessionId = hc.copy(sessionId = Some(SessionId(testSessionId)))

        stubAudit(OK)
        stubMergedAudit(OK)

        stubHipVatSubmission(CREATED)(testHipSuccessResponse)

        val res = connector.submit(testVatSubmissionRequest, testCorrelationid, testCredentialId)(hcWithSessionId)

        await(res) mustBe Right(VatSubmissionSuccess(testFormBundleId))

        verify(
          1,
          postRequestedFor(urlMatching("/etmp/RESTAdapter/vat/subscription"))
            .withHeader("correlationid", equalTo(testCorrelationid))
            .withHeader("Credential-Id", equalTo(testCredentialId))
            .withHeader("X-Originating-System", equalTo("MDTP"))
            .withHeader("X-Transmitting-System", equalTo("HIP"))
            .withHeader("X-Receipt-Date", matching("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z"""))
            .withHeader("Authorization", equalTo(s"Basic ${backendConfig.hipAuthorizationToken}"))
            .withHeader("X-Session-Id", equalTo(testSessionId))
            .withRequestBody(equalToJson(testVatSubmissionRequest.toString))
        )
      }
    }

    "the vat submission service returns an unexpected failure" should {
      "return VatSubmissionFailure" in {
        stubAudit(OK)
        stubMergedAudit(OK)

        val testHipFailuresResponse: JsObject = Json.obj(
          "origin" -> "HIP",
          "response" -> Json.obj(
            "failures" -> Json.arr(
              Json.obj("type" -> "Type of Failure", "reason" -> "Reason for Failure")
            )
          )
        )

        stubHipVatSubmission(REQUEST_TIMEOUT)(testHipFailuresResponse)

        val res = connector.submit(testVatSubmissionRequest, testCorrelationid, testCredentialId)

        await(res) mustBe Left(
          VatSubmissionFailure(REQUEST_TIMEOUT, "Unexpected response from VAT Submission API - status = 408")
        )
      }
    }
  }
}
