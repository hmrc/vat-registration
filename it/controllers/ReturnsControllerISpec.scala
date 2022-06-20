

package controllers

import itutil.{FakeTimeMachine, IntegrationStubbing}
import models.api.returns._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._
import utils.TimeMachine

class ReturnsControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .overrides(bind[TimeMachine].to[FakeTimeMachine])
    .build()

  val testZeroRatedSupplies = 10000.5

  val testAnnualReturns: Returns = Returns(
    turnoverEstimate = None,
    appliedForExemption = None,
    Some(testZeroRatedSupplies),
    reclaimVatOnMostReturns = false,
    Annual,
    JanDecStagger,
    Some(testDate),
    Some(AASDetails(BankGIRO, MonthlyPayment)),
    None,
    None
  )

  val validAnnualReturnsJson: JsObject = Json.obj(
    "zeroRatedSupplies" -> testZeroRatedSupplies,
    "reclaimVatOnMostReturns" -> false,
    "returnsFrequency" -> Json.toJson[ReturnsFrequency](Annual),
    "staggerStart" -> Json.toJson[Stagger](JanDecStagger),
    "startDate" -> testDate,
    "annualAccountingDetails" -> Json.obj(
      "paymentMethod" -> Json.toJson[PaymentMethod](BankGIRO),
      "paymentFrequency" -> Json.toJson[PaymentFrequency](MonthlyPayment)
    )
  )

  val invalidReturnsJson: JsObject = Json.obj()

  "getReturns" should {
    "return OK if successfully obtained" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme(testRegId).copy(returns = Some(testAnnualReturns)))

      val response: WSResponse = await(client(routes.ReturnsController.getReturns("regId").url).get())

      response.status mustBe OK
      response.json mustBe validAnnualReturnsJson
    }

    "return NO_CONTENT if successfully obtained" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val response: WSResponse = await(client(routes.ReturnsController.updateReturns("regId").url).get())

      response.status mustBe NO_CONTENT
    }

    "return NOT_FOUND if no document found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(routes.ReturnsController.getReturns("regId").url).get())

      response.status mustBe NOT_FOUND
    }

    "return FORBIDDEN if user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(routes.ReturnsController.getReturns("regId").url).get())

      response.status mustBe FORBIDDEN
    }
  }

  "updateReturns" should {
    "return OK with a valid returns json body" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val response: WSResponse = await(client(routes.ReturnsController.updateReturns("regId").url)
        .patch(validAnnualReturnsJson))

      response.status mustBe OK
      response.json mustBe validAnnualReturnsJson
    }

    "return BAD_REQUEST if an invalid json body is posted" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val response: WSResponse = await(client(routes.ReturnsController.updateReturns("regId").url)
        .patch(invalidReturnsJson))

      response.status mustBe BAD_REQUEST
    }

    "return NOT_FOUND if no reg document is found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(routes.ReturnsController.updateReturns("regId").url)
        .patch(validAnnualReturnsJson))

      response.status mustBe NOT_FOUND
    }

    "return FORBIDDEN if user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(routes.ReturnsController.updateReturns("regId").url)
        .patch(validAnnualReturnsJson))

      response.status mustBe FORBIDDEN
    }
  }

}
