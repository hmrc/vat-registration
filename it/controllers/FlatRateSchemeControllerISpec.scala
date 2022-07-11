
package controllers

import java.time.LocalDate

import itutil.IntegrationStubbing
import models.api.{FlatRateScheme, VatScheme}
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import controllers.routes.FlatRateSchemeController
import play.api.libs.ws.WSResponse

class FlatRateSchemeControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper

  val dateNow: LocalDate = LocalDate.of(2018, 1, 1)

  def vatScheme(regId: String): VatScheme = testEmptyVatScheme(regId).copy(
      flatRateScheme = Some(FlatRateScheme(joinFrs = true, Some(frsDetails.copy(startDate = Some(dateNow)))))
  )

  val validFullFlatRateSchemeJson: JsObject = Json.parse(
    s"""
       |{
       |  "joinFrs":true,
       |  "frsDetails" : {
       |    "businessGoods" : {
       |      "overTurnover": true,
       |      "estimatedTotalSales": 12345678
       |    },
       |    "startDate": "$dateNow",
       |    "categoryOfBusiness":"123",
       |    "percent":15.00,
       |    "limitedCostTrader": false
       |  }
       |}
     """.stripMargin).as[JsObject]

  val invalidFlatRateSchemeJson: JsObject = Json.parse(
    s"""
       |{
       |
       |}
     """.stripMargin).as[JsObject]


  "fetchFlatRateScheme" should {
    "return OK if successfully obtained" in new Setup {
      given.user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      val response: WSResponse = await(client(FlatRateSchemeController.fetchFlatRateScheme("regId").url).get())

      response.status mustBe OK
      response.json mustBe validFullFlatRateSchemeJson
    }

    "return NO_CONTENT if successfully obtained" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val response: WSResponse = await(client(FlatRateSchemeController.fetchFlatRateScheme("regId").url).get())

      response.status mustBe NO_CONTENT
    }

    "return NOT_FOUND if no document found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(FlatRateSchemeController.fetchFlatRateScheme("regId").url).get())

      response.status mustBe NOT_FOUND
    }

    "return FORBIDDEN if user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(FlatRateSchemeController.fetchFlatRateScheme("regId").url).get())

      response.status mustBe FORBIDDEN
    }
  }

  "updatingFlatRateScheme" should {
    "return OK with a valid threshold json body" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val response: WSResponse = await(client(FlatRateSchemeController.updateFlatRateScheme("regId").url)
        .patch(validFullFlatRateSchemeJson))

      response.status mustBe OK
      response.json mustBe validFullFlatRateSchemeJson
    }

    "return BAD_REQUEST if an invalid json body is posted" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val response: WSResponse = await(client(FlatRateSchemeController.updateFlatRateScheme("regId").url)
        .patch(invalidFlatRateSchemeJson))

      response.status mustBe BAD_REQUEST
    }

    "return NOT_FOUND if no reg document is found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(FlatRateSchemeController.updateFlatRateScheme("regId").url)
        .patch(validFullFlatRateSchemeJson))

      response.status mustBe NOT_FOUND
    }

    "return OK if no data updated because data to be updated already exists" in new Setup {
      given.user.isAuthorised
      insertIntoDb(vatScheme)

      val response: WSResponse = await(client(FlatRateSchemeController.updateFlatRateScheme("regId").url)
        .patch(validFullFlatRateSchemeJson))

      response.status mustBe OK
    }

    "return FORBIDDEN if user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(FlatRateSchemeController.updateFlatRateScheme("regId").url).patch(validFullFlatRateSchemeJson))

      response.status mustBe FORBIDDEN
    }
  }

  "removeFlatRateScheme" should {
    "return OK if successful" in new Setup {
      given.user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      val response: WSResponse = await(client(FlatRateSchemeController.removeFlatRateScheme("regId").url).delete())

      response.status mustBe OK
    }

    "return NOT_FOUND if no reg document is found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(FlatRateSchemeController.removeFlatRateScheme("regId").url).delete())

      response.status mustBe NOT_FOUND
    }

    "return FORBIDDEN if user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(FlatRateSchemeController.removeFlatRateScheme("regId").url).delete())

      response.status mustBe FORBIDDEN
    }
  }
}
