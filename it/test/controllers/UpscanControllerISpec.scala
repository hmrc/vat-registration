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

package controllers

import connectors.stubs.AuditStub.{stubAudit, stubMergedAudit}
import itutil.IntegrationStubbing
import models.api.{AttachmentType, InProgress, PrimaryIdentityEvidence, UpscanDetails}
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext

class UpscanControllerISpec extends IntegrationStubbing with Matchers {

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  override val testRegId = "testRegId"
  lazy val testUpscanDetails: UpscanDetails = UpscanDetails(
    Some(testRegId),
    testReference,
    Some(PrimaryIdentityEvidence),
    None,
    InProgress,
    None,
    None
  )

  def testCallbackJson(reference: String): JsValue = Json.parse(
    s"""{
       |    "reference": "$reference",
       |    "downloadUrl": "https://bucketName.s3.eu-west-2.amazonaws.com?1235676",
       |    "fileStatus": "READY",
       |    "uploadDetails": {
       |        "fileName": "test.pdf",
       |        "fileMimeType": "application/pdf",
       |        "uploadTimestamp": "2018-04-24T09:30:00Z",
       |        "checksum": "396f101dd52e8b2ace0dcf5ed09b1d1f030e608938510ce46e7a5c7a4e775100",
       |        "size": 987
       |    }
       |}""".stripMargin)

  val testReferenceJson: JsValue = Json.obj(
    "reference" -> testReference,
    "attachmentType" -> Json.toJson[AttachmentType](PrimaryIdentityEvidence)
  )

  "GET /:regId/upscan-file-details/:reference" must {
    "return OK with upscan details if data exists" in new SetupHelper {
      given
        .user.isAuthorised
        .upscanDetailsRepo.insertIntoDb(testUpscanDetails, upscanMongoRepository.collection.insertOne)

      insertIntoDb(testEmptyVatScheme(testRegId))

      val res: WSResponse = await(client(controllers.routes.UpscanController.getUpscanDetails(testRegId, testReference).url).get())

      res.status mustBe OK
    }

    "return NOT_FOUND if data does not exist" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(testEmptyVatScheme(testRegId))

      val res: WSResponse = await(client(controllers.routes.UpscanController.getUpscanDetails(testRegId, testReference).url).get())

      res.status mustBe NOT_FOUND
    }
  }

  "GET /:regId/upscan-file-details" must {
    "return OK with all upscan details if data exists" in new SetupHelper {
      given
        .user.isAuthorised
        .upscanDetailsRepo.insertIntoDb(testUpscanDetails, upscanMongoRepository.collection.insertOne)

      insertIntoDb(testEmptyVatScheme(testRegId))

      val res: WSResponse = await(client(controllers.routes.UpscanController.getAllUpscanDetails(testRegId).url).get())

      res.status mustBe OK

      val parsedResult: JsValue = Json.parse(res.body)
      parsedResult mustBe a[JsArray]
      parsedResult.asInstanceOf[JsArray].value.size mustBe 1
    }

    "return OK with empty list of upscan details if not data available" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(testEmptyVatScheme(testRegId))

      val res: WSResponse = await(client(controllers.routes.UpscanController.getAllUpscanDetails(testRegId).url).get())

      res.status mustBe OK
      val parsedResult: JsValue = Json.parse(res.body)
      parsedResult mustBe a[JsArray]
      parsedResult.asInstanceOf[JsArray].value.size mustBe 0
    }
  }

  "POST /:regId/upscan-reference" must {
    "return OK after successfully creating upscan details" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(testEmptyVatScheme(testRegId))

      val res: WSResponse = await(client(controllers.routes.UpscanController.createUpscanDetails(testRegId).url)
        .post(testReferenceJson))

      res.status mustBe OK
    }
  }

  "POST /upscan-callback" must {
    "return OK after successfully storing callback" in new SetupHelper {
      given.upscanDetailsRepo.insertIntoDb(testUpscanDetails, upscanMongoRepository.collection.insertOne)

      stubAudit(OK)
      stubMergedAudit(OK)

      val res: WSResponse = await(client(controllers.routes.UpscanController.upscanDetailsCallback.url)
        .post(Json.toJson(testCallbackJson(testReference))))

      res.status mustBe OK
    }

    "return NOT_FOUND if callback attempts to update non-existant upscan details" in new SetupHelper {
      stubAudit(OK)
      stubMergedAudit(OK)

      val res: WSResponse = await(client(controllers.routes.UpscanController.upscanDetailsCallback.url)
        .post(testCallbackJson(testReference)))

      res.status mustBe NOT_FOUND
    }
  }

  "DELETE /regId/upscan-file-details/reference" must {
    "return NO_CONTENT after successfully deleting upscan details" in new SetupHelper {
      given
        .user.isAuthorised
        .upscanDetailsRepo.insertIntoDb(testUpscanDetails, upscanMongoRepository.collection.insertOne)

      insertIntoDb(testEmptyVatScheme(testRegId))

      stubAudit(OK)
      stubMergedAudit(OK)

      val res: WSResponse = await(
        client(controllers.routes.UpscanController.deleteUpscanDetails(testRegId, testReference).url).delete()
      )

      res.status mustBe NO_CONTENT
      await(upscanMongoRepository.getUpscanDetails(testReference)) mustBe None
    }
  }

  "DELETE /regId/upscan-file-details" must {
    "return NO_CONTENT after successfully deleting upscan details" in new SetupHelper {
      given
        .user.isAuthorised
        .upscanDetailsRepo.insertIntoDb(testUpscanDetails, upscanMongoRepository.collection.insertOne)
        .upscanDetailsRepo.insertIntoDb(testUpscanDetails.copy(reference = testReference2), upscanMongoRepository.collection.insertOne)

      insertIntoDb(testEmptyVatScheme(testRegId))

      stubAudit(OK)
      stubMergedAudit(OK)

      val res: WSResponse = await(
        client(controllers.routes.UpscanController.deleteAllUpscanDetails(testRegId).url).delete()
      )

      res.status mustBe NO_CONTENT
      await(upscanMongoRepository.getAllUpscanDetails(testRegId)) mustBe Nil
    }
  }
}
