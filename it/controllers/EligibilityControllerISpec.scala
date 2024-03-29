
package controllers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import itutil.IntegrationStubbing
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class EligibilityControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper {
    def writeAudit: StubMapping = stubPost("/write/audit/merged", OK, "")
  }

  val validEligibilityJson: JsObject = Json.obj(
    "fixedEstablishment" -> true,
    "businessEntity" -> "50",
    "agriculturalFlatRateScheme" -> false,
    "internationalActivities" -> false,
    "registeringBusiness" -> "own",
    "registrationReason" -> "selling-goods-and-services",
    "thresholdPreviousThirtyDays" -> Json.obj(
      "value" -> false
    ),
    "thresholdInTwelveMonths" -> Json.obj(
      "value" -> false
    ),
    "voluntaryRegistration" -> true,
    "vatRegistrationException" -> false
  )

  val invalidEligibilityJson: JsValue = Json.obj("invalid" -> "json")

  "updatingEligibility" should {
    "return OK with an eligibility json body" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val response: WSResponse = await(client(routes.EligibilityController.updateEligibilityData("regId").url)
        .patch(validEligibilityJson))

      response.status mustBe OK
      response.json mustBe validEligibilityJson
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
