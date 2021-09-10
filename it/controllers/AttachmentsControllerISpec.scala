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

package controllers

import itutil.IntegrationStubbing
import models.api.{AttachmentType, IdentityEvidence}
import models.submission.NETP
import play.api.libs.json.{JsArray, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class AttachmentsControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper

  val url: String = routes.AttachmentsController.getAttachmentList(testRegId).url

  s"GET $url" must {
    "return OK with identityEvidence for a NETP vat scheme" in new Setup {
      given.user.isAuthorised
      insertIntoDb(testSoleTraderVatScheme.copy(eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = NETP))))

      val testJson: JsArray = JsArray(List(Json.toJson[AttachmentType](IdentityEvidence)))

      val response: WSResponse = await(client(url).get())

      response.status mustBe OK
      response.json mustBe testJson
    }

    "return OK with an empty body for a UK Company" in new Setup {
      given.user.isAuthorised
      insertIntoDb(testFullVatScheme)

      val testJson: JsArray = JsArray(Nil)

      val response: WSResponse = await(client(url).get())

      response.status mustBe OK
      response.json mustBe testJson
    }

    "return NOT_FOUND if no document found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(url).get())

      response.status mustBe NOT_FOUND
    }

    "return FORBIDDEN if user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(url).get())

      response.status mustBe FORBIDDEN
    }
  }
}
