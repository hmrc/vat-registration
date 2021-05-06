

package controllers

import itutil.IntegrationStubbing
import models.api._
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import java.time.LocalDate

class AnnualAccountingSchemeControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper

  def vatScheme(regId: String): VatScheme = testEmptyVatScheme(regId).copy(
    annualAccountingScheme = Some(AnnualAccountingScheme(
      joinAAS = true,
      submissionType = Some("1"),
      customerRequest = Some(AASDetails(
        paymentMethod = StandingOrder,
        annualStagger = JanDecStagger,
        paymentFrequency = Monthly,
        estimatedTurnover = 123456,
        requestedStartDate = LocalDate.of(2017, 1, 1)
      )
      ))))

  val validFullAnnualAccountingSchemeJson: JsObject = Json.parse(
    """
      {"joinAAS":true,
      |"submissionType":"1",
      |"customerRequest":{
      |   "paymentMethod":"01",
      |   "annualStagger":"YA",
      |   "paymentFrequency":"M",
      |   "estimatedTurnover":123456,
      |   "requestedStartDate":"2017-01-01"
      |   }
      |}
      |""".stripMargin).as[JsObject]

  val invalidAnnualAccountingSchemeJson: JsObject = Json.parse(
    s"""
       |{
       |
       |}
     """.stripMargin).as[JsObject]


  "getAnnualAccountingScheme" should {
    "return OK if successfully obtained" in new Setup {
      given.user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      val response: WSResponse = await(client(routes.AnnualAccountingSchemeController.getAnnualAccountingScheme("regId").url).get())

      response.status mustBe OK
      response.json mustBe validFullAnnualAccountingSchemeJson
    }

    "return NO_CONTENT if successfully obtained" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val response: WSResponse = await(client(routes.AnnualAccountingSchemeController.updateAnnualAccountingScheme("regId").url).get())

      response.status mustBe NO_CONTENT
    }

    "return NOT_FOUND if no document found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(routes.AnnualAccountingSchemeController.getAnnualAccountingScheme("regId").url).get())

      response.status mustBe NOT_FOUND
    }

    "return FORBIDDEN if user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(routes.AnnualAccountingSchemeController.getAnnualAccountingScheme("regId").url).get())

      response.status mustBe FORBIDDEN
    }
  }

  "updatingAnnualAccountingScheme" should {
    "return OK with a valid threshold json body" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val response: WSResponse = await(client(routes.AnnualAccountingSchemeController.updateAnnualAccountingScheme("regId").url)
        .patch(validFullAnnualAccountingSchemeJson))

      response.status mustBe OK
      response.json mustBe validFullAnnualAccountingSchemeJson
    }

    "return BAD_REQUEST if an invalid json body is posted" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val response: WSResponse = await(client(routes.AnnualAccountingSchemeController.updateAnnualAccountingScheme("regId").url)
        .patch(invalidAnnualAccountingSchemeJson))

      response.status mustBe BAD_REQUEST
    }

    "return NOT_FOUND if no reg document is found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(routes.AnnualAccountingSchemeController.updateAnnualAccountingScheme("regId").url)
        .patch(validFullAnnualAccountingSchemeJson))

      response.status mustBe NOT_FOUND
    }

    "return FORBIDDEN if user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(routes.AnnualAccountingSchemeController.updateAnnualAccountingScheme("regId").url).patch(validFullAnnualAccountingSchemeJson))

      response.status mustBe FORBIDDEN
    }
  }

}
