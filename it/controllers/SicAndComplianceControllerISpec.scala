
package controllers

import controllers.routes.SicAndComplianceController
import itutil.IntegrationStubbing
import models.api._
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class SicAndComplianceControllerISpec extends IntegrationStubbing {

  val validSicAndCompliance: Option[SicAndCompliance] = Some(SicAndCompliance(
    "this is my business description",
    Some(ComplianceLabour(numOfWorkersSupplied = Some(1000), intermediaryArrangement = Some(true), supplyWorkers = true)),
    SicCode("12345", "the flu", "sic details"),
    Nil
  ))
  val otherBusinessActivities = SicCode("00998", "otherBusiness desc 1", "fooBar 1") :: SicCode("00889", "otherBusiness desc 2", "fooBar 2") :: Nil
  val validSicAndComplianceWithOtherBusinessActivities = Some(validSicAndCompliance.get.copy(businessActivities = otherBusinessActivities))
  val validSicAndComplianceJson = Json.parse(
    s"""
       |{
       |  "businessDescription": "this is my business description",
       |  "labourCompliance" : {
       |    "numOfWorkersSupplied": 1000,
       |    "intermediaryArrangement":true,
       |    "supplyWorkers":true
       |  },
       |  "mainBusinessActivity": {
       |    "code": "12345",
       |    "desc": "the flu",
       |    "indexes": "sic details"
       |  },
       |  "businessActivities":[]
       |}
    """.stripMargin).as[JsObject]

  val validUpdatedSicAndCompliancejson: JsObject = Json.parse(
    s"""
       |{
       |  "businessDescription": "fooBar",
       |  "labourCompliance" : {
       |    "supplyWorkers": true,
       |    "intermediaryArrangement":false
       |  },
       |  "mainBusinessActivity": {
       |    "code": "12345",
       |    "desc": "the flu 1",
       |    "indexes": "sic details 1"
       |  },
       |  "businessActivities": [
       |    {
       |      "code": "99889",
       |      "desc": "oherBusiness",
       |      "indexes": "otherBusiness1"
       |    }
       |  ]
       |}
    """.stripMargin).as[JsObject]

  val validSicAndComplianceJsonWithoutOtherBusinessActivities: JsObject = Json.parse(
    s"""
       |{
       |  "businessDescription": "this is my business description",
       |  "labourCompliance" : {
       |    "supplyWorkers": true,
       |    "intermediaryArrangement":false
       |  },
       |  "mainBusinessActivity": {
       |    "code": "12345",
       |    "desc": "the flu",
       |    "indexes": "sic details"
       |  }
       |}
    """.stripMargin).as[JsObject]

  val invalidSicAndComplianceJson: JsObject = Json.parse(
    s"""
       |{
       | "fooBar":"fooWizzBarBang"
       |}
     """.stripMargin).as[JsObject]

  def vatScheme(regId: String): VatScheme = testEmptyVatScheme(regId).copy(sicAndCompliance = validSicAndCompliance)

  class Setup extends SetupHelper

  "getSicAndCompliance" should {
    "return OK" in new Setup {
      given.user.isAuthorised
        .regRepo.insertIntoDb(vatScheme("foo"), repo.insert)

      val response: WSResponse = await(client(SicAndComplianceController.getSicAndCompliance("foo").url).get())

      response.json mustBe validSicAndComplianceJson
      response.status mustBe OK
    }
    "return NO_CONTENT when no SicAndComplianceRecord is found but reg doc exists" in new Setup {
      given.user.isAuthorised
        .regRepo.insertIntoDb(testEmptyVatScheme("foo"), repo.insert)

      val response: WSResponse = await(client(SicAndComplianceController.getSicAndCompliance("foo").url).get())

      response.status mustBe NO_CONTENT
    }
    "return NOT_FOUND when no reg doc is found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(SicAndComplianceController.getSicAndCompliance("fooBar").url).get())

      response.status mustBe NOT_FOUND
    }
    "return FORBIDDEN when user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(SicAndComplianceController.getSicAndCompliance("fooBar").url).get())

      response.status mustBe FORBIDDEN
    }
  }
  "updateSicAndCompliance" should {
    "return OK during update to existing sicAndComp record" in new Setup {
      given.user.isAuthorised
        .regRepo.insertIntoDb(vatScheme("fooBar"), repo.insert)

      val response: WSResponse = await(client(SicAndComplianceController.updateSicAndCompliance("fooBar").url)
        .patch(validUpdatedSicAndCompliancejson))

      response.status mustBe OK
      response.json mustBe validUpdatedSicAndCompliancejson
    }
    "return OK during update to vat doc whereby no sicAndCompliance existed before" in new Setup {
      given.user.isAuthorised
        .regRepo.insertIntoDb(testEmptyVatScheme("fooBar"), repo.insert)

      val response: WSResponse = await(client(SicAndComplianceController.updateSicAndCompliance("fooBar").url)
        .patch(validSicAndComplianceJson))

      response.status mustBe OK
      response.json mustBe validSicAndComplianceJson
    }
    "return NOT_FOUND during update when no regDoc exists" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(SicAndComplianceController.updateSicAndCompliance("fooBar").url)
        .patch(validSicAndComplianceJson))

      response.status mustBe NOT_FOUND
    }
    "return FORBIDDEN when the user is not Authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(SicAndComplianceController.updateSicAndCompliance("fooBar").url)
        .patch(validSicAndComplianceJson))

      response.status mustBe FORBIDDEN
    }
  }
}


