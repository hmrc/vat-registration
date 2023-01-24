
package controllers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import itutil.IntegrationStubbing
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.EligibilityDataJsonUtils

class EligibilityControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper {
    def writeAudit: StubMapping = stubPost("/write/audit/merged", OK, "")
  }

  val questions2 = Seq(
    Json.obj("questionId" -> "voluntaryRegistration", "question" -> "testQuestion", "answer" -> "testAnswer", "answerValue" -> true),
    Json.obj("questionId" -> "registeringBusiness", "question" -> "testQuestion", "answer" -> "testAnswer", "answerValue" -> "own"),
    Json.obj("questionId" -> "businessEntity", "question" -> "testQuestion", "answer" -> "testAnswer", "answerValue" -> "50"),
    Json.obj("questionId" -> "fixedEstablishment", "question" -> "testQuestion", "answer" -> "testAnswer", "answerValue" -> true),
    Json.obj("questionId" -> "registrationReason", "question" -> "testQuestion", "answer" -> "testAnswer", "answerValue" -> "selling-goods-and-services")
  )
  val section: JsObject = Json.obj("title" -> "testTitle", "data" -> JsArray(questions2))
  val sections: JsArray = JsArray(Seq(section))
  val validEligibilityJson: JsObject = Json.obj("sections" -> sections)

  val invalidEligibilityJson: JsValue = Json.obj("invalid" -> "json")

  "updatingEligibility" should {
    "return OK with an eligibility json body" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val response: WSResponse = await(client(routes.EligibilityController.updateEligibilityData("regId").url)
        .patch(validEligibilityJson))

      response.status mustBe OK
      response.json mustBe EligibilityDataJsonUtils.toJsObject(validEligibilityJson, "regId")
    }

    "return BAD_REQUEST if an invalid json body is posted" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val response: WSResponse = await(client(routes.EligibilityController.updateEligibilityData("regId").url)
        .patch(invalidEligibilityJson))

      response.status mustBe INTERNAL_SERVER_ERROR
    }

    "return NOT_FOUND if no reg document is found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(routes.EligibilityController.updateEligibilityData("regId").url)
        .patch(validEligibilityJson))

      response.status mustBe NOT_FOUND
    }

    "return FORBIDDEN if user is not authorised obtained" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(routes.EligibilityController.updateEligibilityData("regId").url)
        .patch(validEligibilityJson))

      response.status mustBe FORBIDDEN
    }
  }
}
