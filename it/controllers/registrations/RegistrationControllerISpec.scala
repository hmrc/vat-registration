
package controllers.registrations

import enums.VatRegStatus
import itutil.{FakeRegistrationIdService, IntegrationStubbing}
import models.api.VatScheme
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import services.RegistrationIdService

import scala.concurrent.ExecutionContext

class RegistrationControllerISpec extends IntegrationStubbing {

  implicit val fmt = VatScheme.format()
  implicit val ec = ExecutionContext.Implicits.global

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .overrides(bind[RegistrationIdService].to[FakeRegistrationIdService])
    .build()

  val registrationsUrl = "/registrations"

  def registrationUrl(regID: String) = s"$registrationsUrl/$regID"

  val testRegId2 = testRegId + "2"

  val testVatScheme2 = testVatScheme.copy(id = testRegId2)

  "GET /registrations" when {
    "registrations exist for the user" must {
      "return OK with a list of registrations" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)
        insertIntoDb(testVatScheme2)

        val res = await(client(registrationsUrl).get)

        res.status mustBe OK
        res.json mustBe Json.toJson(Seq(testVatScheme, testVatScheme2))
      }
    }
    "registrations do not exist for the user" must {
      "return OK with an empty list" in new SetupHelper {
        given.user.isAuthorised

        val res = await(client(registrationsUrl).get)

        res.status mustBe OK
        res.json mustBe Json.toJson(Nil)
      }
    }
  }

  "POST /registrations" when {
    "the user has no existing registrations" must {
      "return CREATED with the json of the new registration" in new SetupHelper {
        given.user.isAuthorised
        FakeRegistrationIdService.id = testRegId

        val res = await(client(registrationsUrl).post(Json.obj()))

        res.status mustBe CREATED
        res.json mustBe Json.toJson(testVatScheme)
      }
    }
    "the user has existing registrations" must {
      "return CREATED with the json of the new registration without affecting other registrations" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)
        FakeRegistrationIdService.id = testRegId2

        val res = await(client(registrationsUrl).post(Json.obj()))

        res.status mustBe CREATED
        res.json mustBe Json.toJson(testVatScheme2)
        await(repo.getRegistration(testInternalid, testRegId2)) mustBe Some(Json.toJson(testVatScheme2))
      }
    }
  }

  "GET /registrations/:regId" when {
    "the registration exists" must {
      "return OK with the registration JSON" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res = await(client(registrationUrl(testRegId)).get)

        res.status mustBe OK
        res.json mustBe Json.toJson(testVatScheme)
      }
    }
    "the registration donesn't exists" must {
      "return NOT FOUND" in new SetupHelper {
        given.user.isAuthorised

        val res = await(client(registrationUrl(testRegId)).get)

        res.status mustBe NOT_FOUND
      }
    }
  }

  "PUT /registrations/:regID" when {
    "the registration exists" must {
      "update the registration and return OK with the updated json" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val updatedRegistration = testVatScheme.copy(status = VatRegStatus.locked)

        val res = await(client(registrationUrl(testRegId)).put(Json.toJson(updatedRegistration)))

        res.status mustBe OK
        res.json mustBe Json.toJson(updatedRegistration)
      }
    }
    "the registration doesn't exist" must {
      "create a record and return OK with the json" in new SetupHelper {
        given.user.isAuthorised

        val res = await(client(registrationUrl(testRegId)).put(Json.toJson(testVatScheme)))

        res.status mustBe OK
        res.json mustBe Json.toJson(testVatScheme)
      }
    }
  }

  "DELETE /registrations/:regId" when {
    "the registration exists" must {
      "return NO CONTENT and remove the record" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res = await(client(registrationUrl(testRegId)).delete())

        res.status mustBe NO_CONTENT
        await(repo.getRegistration(testRegId, testRegId)) mustBe None
      }
    }
    "the regustrtion doesn't exist" must {
      "return NO CONTENT" in new SetupHelper {
        given.user.isAuthorised

        val res = await(client(registrationUrl(testRegId)).delete())

        res.status mustBe NO_CONTENT
      }
    }
  }

}
