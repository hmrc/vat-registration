
package controllers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import controllers.routes.EligibilityController
import itutil.IntegrationStubbing
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class EligibilityControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper {
    def writeAudit: StubMapping = stubPost("/write/audit/merged", OK, "")
  }

  val completionCapacity: JsObject = Json.obj("role" -> "director", "name" -> Json.obj(
    "forename" -> "First Name Test",
    "other_forenames" -> "Middle Name Test",
    "surname" -> "Last Name Test"
  ))
  val questions1 = Seq(
    Json.obj("questionId" -> "completionCapacity", "question" -> "Some Question 11", "answer" -> "Some Answer 11", "answerValue" -> completionCapacity),
    Json.obj("questionId" -> "testQId12", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> "val12")
  )
  val questions2 = Seq(
    Json.obj("questionId" -> "voluntaryRegistration", "question" -> "Some Question", "answer" -> "Some Answer", "answerValue" -> true),
    Json.obj("questionId" -> "voluntaryInformation", "question" -> "Some Question", "answer" -> "Some Answer", "answerValue" -> true),
    Json.obj("questionId" -> "applicantUKNino-optionalData", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "JW778877A"),
    Json.obj("questionId" -> "turnoverEstimate", "question" -> "Some Question 21", "answer" -> "Some Answer 21", "answerValue" -> 12345),
    Json.obj("questionId" -> "testQId22", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "val22"),
    Json.obj("questionId" -> "registeringBusiness", "question" -> "Some Question 23", "answer" -> "Some Answer 23", "answerValue" -> "own"),
    Json.obj("questionId" -> "businessEntity", "question" -> "Some Question 23", "answer" -> "Some Answer 23", "answerValue" -> "50"),
    Json.obj("questionId" -> "currentlyTrading", "question" -> "testQuestion", "answer" -> "testAnswer", "answerValue" -> false)
  )
  val section1: JsObject = Json.obj("title" -> "test TITLE 1", "data" -> JsArray(questions1))
  val section2: JsObject = Json.obj("title" -> "test TITLE 2", "data" -> JsArray(questions2))
  val sections: JsArray = JsArray(Seq(section1, section2))
  val validEligibilityJson: JsObject = Json.obj("sections" -> sections)

  val invalidEligibilityJson: JsValue = Json.obj("invalid" -> "json")

  "updatingEligibility" should {
    "return OK with an eligibility json body" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val response: WSResponse = await(client(EligibilityController.updateEligibilityData("regId").url)
        .patch(validEligibilityJson))

      response.status mustBe OK
      response.json mustBe validEligibilityJson
    }

    "return BAD_REQUEST if an invalid json body is posted" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val response: WSResponse = await(client(EligibilityController.updateEligibilityData("regId").url)
        .patch(invalidEligibilityJson))

      response.status mustBe INTERNAL_SERVER_ERROR
    }

    "return NOT_FOUND if no reg document is found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(EligibilityController.updateEligibilityData("regId").url)
        .patch(validEligibilityJson))

      response.status mustBe NOT_FOUND
    }

    "return FORBIDDEN if user is not authorised obtained" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(EligibilityController.updateEligibilityData("regId").url)
        .patch(validEligibilityJson))

      response.status mustBe FORBIDDEN
    }
  }
}
