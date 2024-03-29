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
import models.api._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext

class AttachmentsControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val attachmentsUrl: String = routes.AttachmentsController.getAttachmentList(testRegId).url

  s"GET /:regId/attachments " must {
    "return OK with identityEvidence attachments if identifiers match not complete" in new Setup {
      given.user.isAuthorised
      insertIntoDb(
        testEmptyVatScheme(testRegId).copy(
          applicantDetails = Some(testUnregisteredApplicantDetails.copy(personalDetails = Some(testPersonalDetails.copy(identifiersMatch = false)))),
          eligibilitySubmissionData = Some(testEligibilitySubmissionData)
        )
      )

      val testJson = Json.arr(Json.toJson[AttachmentType](IdentityEvidence))

      val response: WSResponse = await(client(attachmentsUrl).get())

      response.status mustBe OK
      response.json mustBe testJson
    }

    "return OK with an empty body for a UK Company" in new Setup {
      given.user.isAuthorised
      insertIntoDb(testFullVatScheme)

      val testJson = Json.parse("[]")

      val response: WSResponse = await(client(attachmentsUrl).get())

      response.status mustBe OK
      response.json mustBe testJson
    }

    "return OK with an empty body if no document found" in new Setup {
      given.user.isAuthorised

      val testJson = Json.parse("[]")

      val response: WSResponse = await(client(attachmentsUrl).get())

      response.status mustBe OK
      response.json mustBe testJson
    }

    "return FORBIDDEN if user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(attachmentsUrl).get())

      response.status mustBe FORBIDDEN
    }
  }

  "GET /:regId/incomplete-attachments" must {
    val url = controllers.routes.AttachmentsController.getIncompleteAttachments(testRegId).url
    "return a list of required attachment uploads for a user that needs identity evidence" when {
      val testVatScheme: VatScheme = testEmptyVatScheme(testRegId).copy(
        applicantDetails = Some(testUnregisteredApplicantDetails.copy(personalDetails = Some(testPersonalDetails.copy(identifiersMatch = false)))),
        eligibilitySubmissionData = Some(testEligibilitySubmissionData)
      )
      "no attachments have been uploaded yet" in new Setup {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res: WSResponse = await(client(url).get())

        res.status mustBe OK
        res.body mustBe Json.toJson(List[AttachmentType](PrimaryIdentityEvidence, ExtraIdentityEvidence, ExtraIdentityEvidence)).toString()
      }

      "some attachments have been uploaded" in new Setup {
        given
          .user.isAuthorised
          .upscanDetailsRepo.insertIntoDb(testUpscanDetails(testReference), upscanMongoRepository.collection.insertOne)
          .upscanDetailsRepo.insertIntoDb(testUpscanDetails(testReference2).copy(attachmentType = Some(ExtraIdentityEvidence)), upscanMongoRepository.collection.insertOne)

        insertIntoDb(testVatScheme)

        val res: WSResponse = await(client(url).get())

        res.status mustBe OK
        res.body mustBe Json.toJson(List[AttachmentType](ExtraIdentityEvidence)).toString()
      }
    }
  }
}
