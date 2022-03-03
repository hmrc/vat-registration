
package controllers

import itutil.IntegrationStubbing
import models.api.{InProgress, UpscanDetails}
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext

class UpscanControllerISpec extends IntegrationStubbing {

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  override val testRegId = "testRegId"
  val testUpscanDetails: UpscanDetails = UpscanDetails(
    Some(testRegId),
    testReference,
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

  "GET /:regId/upscan-file-details/:reference" must {
    "return OK with upscan details if data exists" in new SetupHelper {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testEmptyVatScheme(testRegId), repo.insert)
        .upscanDetailsRepo.insertIntoDb(testUpscanDetails, upscanMongoRepository.insert)

      val res: WSResponse = await(client(controllers.routes.UpscanController.getUpscanDetails(testRegId, testReference).url).get)

      res.status mustBe OK
    }

    "return NOT_FOUND if data does not exist" in new SetupHelper {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testEmptyVatScheme(testRegId), repo.insert)

      val res: WSResponse = await(client(controllers.routes.UpscanController.getUpscanDetails(testRegId, testReference).url).get)

      res.status mustBe NOT_FOUND
    }
  }

  "POST /:regId/upscan-reference" must {
    "return OK after successfully creating upscan details" in new SetupHelper {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testEmptyVatScheme(testRegId), repo.insert)

      val res: WSResponse = await(client(controllers.routes.UpscanController.createUpscanDetails(testRegId).url)
        .post(JsString(testReference)))

      res.status mustBe OK
    }
  }

  "POST /upscan-callback" must {
    "return OK after successfully storing callback" in new SetupHelper {
      given.upscanDetailsRepo.insertIntoDb(testUpscanDetails, upscanMongoRepository.insert)

      stubPost("/write/audit", OK, """{"x":2}""")
      stubPost("/write/audit/merged", OK, """{"x":2}""")

      val res: WSResponse = await(client(controllers.routes.UpscanController.upscanDetailsCallback.url)
        .post(Json.toJson(testCallbackJson(testReference))))

      res.status mustBe OK
    }

    "throw an exception if callback attempts to update non-existant upscan details" in new SetupHelper {
      stubPost("/write/audit", OK, """{"x":2}""")
      stubPost("/write/audit/merged", OK, """{"x":2}""")

      val res: WSResponse = await(client(controllers.routes.UpscanController.upscanDetailsCallback.url)
        .post(testCallbackJson(testReference)))

      res.status mustBe INTERNAL_SERVER_ERROR
    }
  }
}
