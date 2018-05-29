
package controllers

import java.time.LocalDate

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.RegistrationId
import enums.VatRegStatus
import itutil.{ITFixtures, IntegrationStubbing, WiremockHelper}
import models.api._
import play.api.libs.json.{JsBoolean, JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.test.FakeApplication
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import repositories.{RegistrationMongo, RegistrationMongoRepository, SequenceMongo, SequenceMongoRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LodgingOfficerControllerISpec extends IntegrationStubbing {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl  = s"http://$mockHost:$mockPort"

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
    "auditing.consumer.baseUri.host" -> s"$mockHost",
    "auditing.consumer.baseUri.port" -> s"$mockPort",
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort",
    "microservice.services.des-stub.host" -> s"$mockHost",
    "microservice.services.des-stub.port" -> s"$mockPort",
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "microservice.services.incorporation-information.uri" -> "/incorporation-information",
    "mongo-encryption.key" -> "ABCDEFGHIJKLMNOPQRSTUV=="
  ))

  class Setup extends SetupHelper {
    def writeAudit: StubMapping = stubPost("/write/audit/merged",200,"")
  }

  val currentAddress            = Address("12 Lukewarm","Oriental lane")
  val skylakeValiarm            = Name(first = Some("Skylake"), middle = None, last = Some("Valiarm"))
  val skylakeDigitalContact     = DigitalContactOptional(None, Some("123456789012345678"), None)
  val lodgingOfficerDetails     = LodgingOfficerDetails(currentAddress = currentAddress, None, None, contact = skylakeDigitalContact)
  val validLodgingOfficerPreIV  = LodgingOfficer(
    dob = LocalDate.now(),
    nino = "AB123456A",
    role = "secretary",
    name = skylakeValiarm,
    ivPassed = None,
    details = None)
  val validLodgingOfficerPostIv = validLodgingOfficerPreIV.copy(details = Some(lodgingOfficerDetails))

  def vatScheme(regId: String): VatScheme = emptyVatScheme(regId).copy(lodgingOfficer = Some(validLodgingOfficerPreIV))

  val upsertLodgingOfficerJson = Json.parse(
    s"""
       |{
       | "name": {
       |   "first" : "Skylake",
       |   "last" : "Valiarm"
       | },
       | "dob" : "${LocalDate.now()}",
       | "nino" : "AB123456A",
       | "role" : "secretary",
       | "ivPassed" : true,
       | "details" : {
       |   "currentAddress" : {
       |     "line1" : "12 Lukewarm",
       |     "line2"  : "Oriental lane"
       |   },
       |   "contact" : {
       |     "email" : "skylake@vilikariet.com"
       |   }
       | }
       |}
    """.stripMargin).as[JsObject]

  val validLodgingOfficerJson = Json.parse(
    s"""
       |{
       | "name": {
       |   "first" : "Skylake",
       |   "last" : "Valiarm"
       | },
       | "dob" : "${LocalDate.now()}",
       | "nino" : "AB123456A",
       | "role" : "secretary"
       |}
    """.stripMargin).as[JsObject]

  val invalidLodgingOfficerJson = Json.parse(
    s"""
       |{
       | "dob" : "${LocalDate.now()}",
       | "nino" : "AB123456A",
       | "role" : "secretary"
       |}
    """.stripMargin).as[JsObject]

  "getLodgingOfficer" should {
    "return 200" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      await(client(controllers.routes.LodgingOfficerController.getLodgingOfficer("regId").url).get() map { response =>
        response.status shouldBe 200
        response.json shouldBe validLodgingOfficerJson
      })
    }

    "return 204" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.LodgingOfficerController.getLodgingOfficer("regId").url).get() map { response =>
        response.status shouldBe 204
      })
    }

    "return 404 if no document found" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.LodgingOfficerController.getLodgingOfficer("regId").url).get() map { response =>
        response.status shouldBe 404
      })
    }

    "return 403 if user is not authorised" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.LodgingOfficerController.getLodgingOfficer("regId").url).get() map { response =>
        response.status shouldBe 403
      })
    }
  }

  "updatingLodgingOfficer" should {
    "return 200 with an lodgingOfficer json body" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.LodgingOfficerController.updateLodgingOfficer("regId").url).patch(validLodgingOfficerJson) map { response =>
        response.status shouldBe 200
        response.json shouldBe validLodgingOfficerJson
      })
    }

    "return 200 with when updating the lodging officer post IV" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      await(client(controllers.routes.LodgingOfficerController.updateLodgingOfficer("regId").url).patch(upsertLodgingOfficerJson) map { response =>
        response.status shouldBe 200
        response.json shouldBe upsertLodgingOfficerJson
      })
    }

    "return 400 if an invalid json body is posted" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.LodgingOfficerController.updateLodgingOfficer("regId").url).patch(invalidLodgingOfficerJson) map { response =>
        response.status shouldBe 400
      })
    }

    "return 404 if no reg document is found" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.LodgingOfficerController.updateLodgingOfficer("regId").url).patch(validLodgingOfficerJson) map { response =>
        response.status shouldBe 404
      })
    }

    "return 200 if no data updated because data is same" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      await(client(controllers.routes.LodgingOfficerController.updateLodgingOfficer("regId").url).patch(validLodgingOfficerJson) map { response =>
        response.status shouldBe 200
      })
    }

    "return 403 if user is not authorised obtained" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.LodgingOfficerController.updateLodgingOfficer("regId").url).patch(validLodgingOfficerJson) map { response =>
        response.status shouldBe 403
      })
    }
  }

  "updateIVStatus" should {
    "return 200" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      await(client(controllers.routes.LodgingOfficerController.updateIVStatus("regId", true).url).patch("") map { response =>
        response.status shouldBe 200
        response.json shouldBe JsBoolean(true)
      })
    }

    "return 404 if a none boolean is provided in the query parameter" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client("/regId/update-iv-status/test").patch("") map { response =>
        response.status shouldBe 404
      })
    }

    "return 404 if no reg document is found" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.LodgingOfficerController.updateIVStatus("regId", true).url).patch("") map { response =>
        response.status shouldBe 404
      })
    }

    "return 404 if the reg document has no lodgingOfficer block" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.LodgingOfficerController.updateIVStatus("regId", true).url).patch("") map { response =>
        response.status shouldBe 404
      })
    }

    "return 403 if user is not authorised obtained" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.LodgingOfficerController.updateIVStatus("regId", true).url).patch("") map { response =>
        response.status shouldBe 403
      })
    }
  }
}