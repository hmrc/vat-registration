
package controllers

import auth.CryptoSCRS
import itutil.IntegrationStubbing
import models.api.{Address, BusinessContact, DigitalContact, Letter, VatScheme}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class BusinessContactControllerISpec extends IntegrationStubbing {

  lazy val crypto: CryptoSCRS = app.injector.instanceOf(classOf[CryptoSCRS])

  class Setup extends SetupHelper

  val validBusinessContact: Option[BusinessContact] = Some(BusinessContact(
    digitalContact = DigitalContact("email@email.com",Some("12345"),Some("54321")),
    website = Some("www.foo.com"),
    ppob = Address("line1","line2",None,None,None,Some(testCountry)),
    commsPreference = Letter
  ))

  val validBusinessContactJson: JsObject = Json.parse(
    s"""{
       |"digitalContact":{
       |"email": "email@email.com",
       |"tel": "12345",
       |"mobile": "54321"
       |},
       |"website": "www.foo.com",
       |"ppob": {
       |  "line1": "line1",
       |  "line2": "line2",
       |  "country": {
       |    "code": "UK"
       |  }
       | },
       | "contactPreference": "Letter"
       |}
       |
     """.stripMargin
  ).as[JsObject]

  val validUpdatedBusinessContactJson: JsObject = Json.parse(
    s"""{
       |"digitalContact":{
       |"email": "email@email1.com",
       |"tel": "123456",
       |"mobile": "543210"
       |},
       |"website": "www.foobar.com",
       |"ppob": {
       |  "line1": "line1a",
       |  "line2": "line2b",
       |  "country": {
       |    "code": "UK"
       |  }
       | },
       | "contactPreference": "Email"
       |}
       |
     """.stripMargin
  ).as[JsObject]


  def vatScheme(regId: String): VatScheme = testEmptyVatScheme(regId).copy(businessContact = validBusinessContact)

  "getBusinessContact" should {
    "return OK" in new Setup {
      given.user.isAuthorised
        .regRepo.insertIntoDb(vatScheme("foo"), repo.insert)

      val response: WSResponse = await(client(routes.BusinessContactController.getBusinessContact("foo").url).get())

      response.status mustBe OK
      response.json mustBe validBusinessContactJson
    }
    "return NO_CONTENT when no BusinessContact Record is found but reg doc exists" in new Setup {
      given.user.isAuthorised
        .regRepo.insertIntoDb(testEmptyVatScheme("foo"), repo.insert)

      val response: WSResponse = await(client(routes.BusinessContactController.getBusinessContact("foo").url).get())

      response.status mustBe NO_CONTENT
    }
    "return NOT_FOUND when no reg doc is found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(routes.BusinessContactController.getBusinessContact("fooBar").url).get())

      response.status mustBe NOT_FOUND
    }
    "return FORBIDDEN when user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(routes.BusinessContactController.getBusinessContact("fooBar").url).get())

      response.status mustBe FORBIDDEN
    }
  }
  "updateBusinessConact" should {
    "return OK during update to existing BusinessContact record" in new Setup {
      given.user.isAuthorised
        .regRepo.insertIntoDb(vatScheme("fooBar"),repo.insert)

      val response: WSResponse = await(client(routes.BusinessContactController.updateBusinessContact("fooBar").url)
          .patch(validUpdatedBusinessContactJson))

      response.status mustBe OK
      response.json mustBe validUpdatedBusinessContactJson
    }
    "return OK during update to vat doc whereby no BusinessContact existed before" in new Setup {
      given.user.isAuthorised
        .regRepo.insertIntoDb(testEmptyVatScheme("fooBar"),repo.insert)

      val response: WSResponse = await(client(routes.BusinessContactController.updateBusinessContact("fooBar").url)
        .patch(validBusinessContactJson))

      response.status mustBe OK
      response.json mustBe validBusinessContactJson
    }
    "return NOT_FOUND during update when no regDoc exists" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(routes.BusinessContactController.updateBusinessContact("fooBar").url)
        .patch(validBusinessContactJson))

      response.status mustBe NOT_FOUND
    }
    "return FORBIDDEN when the user is not Authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(routes.BusinessContactController.updateBusinessContact("fooBar").url)
        .patch(validBusinessContactJson))

      response.status mustBe FORBIDDEN
    }
  }
}
