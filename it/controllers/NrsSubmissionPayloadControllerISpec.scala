
package controllers

import controllers.NrsSubmissionPayloadController._
import itutil.IntegrationStubbing
import play.api.libs.json.{JsObject, JsString, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class NrsSubmissionPayloadControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper

  val testString = "testEncodedString"
  val testPayload: JsObject = Json.obj(
    payloadKey -> testString
  )

  "storeNrsSubmissionPayload" should {
    "return OK" in new Setup {
      given.user.isAuthorised
      insertIntoDb(testEmptyVatScheme(testRegId))

      val response: WSResponse = await(client(routes.NrsSubmissionPayloadController.storeNrsSubmissionPayload(testRegId).url)
        .patch(testPayload)
      )

      response.status mustBe OK
      response.json mustBe JsString(testString)
    }

    "return OK if there is already a payload stored" in new Setup {
      val testOldPayload = "testOldPayload"
      given.user.isAuthorised
      insertIntoDb(testEmptyVatScheme(testRegId).copy(nrsSubmissionPayload = Some(testOldPayload)))

      val response: WSResponse = await(client(routes.NrsSubmissionPayloadController.storeNrsSubmissionPayload(testRegId).url)
        .patch(testPayload)
      )

      response.status mustBe OK
      response.json mustBe JsString(testString)
    }

    "return NOT_FOUND during update when no regDoc exists" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(routes.NrsSubmissionPayloadController.storeNrsSubmissionPayload(testRegId).url)
        .patch(testPayload)
      )

      response.status mustBe NOT_FOUND
    }
    "return FORBIDDEN when the user is not Authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(routes.NrsSubmissionPayloadController.storeNrsSubmissionPayload(testRegId).url)
        .patch(testPayload)
      )

      response.status mustBe FORBIDDEN
    }
  }
}


