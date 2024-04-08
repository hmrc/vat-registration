/*
 * Copyright 2024 HM Revenue & Customs
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
import itutil.IntegrationStubbing
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._

class EmailConnectorISpec extends IntegrationStubbing {

  implicit val request: Request[_] = FakeRequest()

  val connector = app.injector.instanceOf[EmailConnector]
  val emptyBody = ""
  val basicTemplate = "mtdfb_vatreg_registration_received"
  val emailTemplate = s"${basicTemplate}_email"
  val postalTemplate = s"${basicTemplate}_post"
  val url = s"/hmrc/email"

  def request(template: String, params: JsValue = Json.obj()): JsValue =
    Json.obj(
      "to" -> Json.arr(testEmail),
      "templateId" -> template,
      "parameters" -> params,
      "force" -> true
    )

  "The email connector" when {
    Seq(basicTemplate, emailTemplate, postalTemplate) foreach { template =>
      s"requesting the ${template} template" when {
        "the email API returns ACCEPTED" must {
          "return EmailSent" in {
            stubPost(url, request(template, Json.obj("name" -> "testName", "ref" -> "testRef")), ACCEPTED, emptyBody)
            stubAudit(OK)
            stubMergedAudit(OK)

            val res = await(connector.sendEmail(testEmail, template, params = Map("name" -> "testName", "ref" -> "testRef"), force = true))

            res mustBe EmailSent
          }
        }
        "the email API returns any other status" must {
          "return EmailFailedToSend" in {
            stubPost(url, request(template), IM_A_TEAPOT, emptyBody)
            stubAudit(OK)
            stubMergedAudit(OK)

            val res = await(connector.sendEmail(testEmail, template, params = Map(), force = true))

            res mustBe EmailFailedToSend
          }
        }
      }
    }
  }

}
