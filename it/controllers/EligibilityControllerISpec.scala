
package controllers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.RegistrationId
import enums.VatRegStatus
import itutil.{ITFixtures, IntegrationStubbing, WiremockHelper}
import models.api.{Eligibility, VatScheme}
import play.api.Configuration
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSClient
import play.api.test.FakeApplication
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult
import repositories.{RegistrationMongo, RegistrationMongoRepository, SequenceMongo, SequenceMongoRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EligibilityControllerISpec extends IntegrationStubbing with ITFixtures {

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
    "microservice.services.incorporation-information.uri" -> "/incorporation-information"
  ))

  lazy val reactiveMongoComponent = app.injector.instanceOf[ReactiveMongoComponent]
  lazy val ws   = app.injector.instanceOf(classOf[WSClient])

  private def client(path: String) = ws.url(s"http://localhost:$port$path").withFollowRedirects(false)

  class Setup {
    val mongo = new RegistrationMongo(reactiveMongoComponent)
    val sequenceMongo = new SequenceMongo(reactiveMongoComponent)
    val repo: RegistrationMongoRepository = mongo.store
    val sequenceRepository: SequenceMongoRepository = sequenceMongo.store

    await(repo.drop)
    await(repo.ensureIndexes)
    await(sequenceRepository.drop)
    await(sequenceRepository.ensureIndexes)

    def insertIntoDb(vatScheme: VatScheme): Future[WriteResult] = await(repo.insert(vatScheme))
    def writeAudit: StubMapping = stubPost("/write/audit/merged",200,"")
  }

  def vatScheme(regId: String): VatScheme = emptyVatScheme(regId).copy(eligibility = Some(Eligibility(1,"success")))

  def emptyVatScheme(regId: String): VatScheme = VatScheme(id = RegistrationId(regId),status = VatRegStatus.draft)

  val validEligibilityJson = Json.parse(
    """
      |{
      | "version": 1,
      | "result": "success"
      |}
    """.stripMargin).as[JsObject]

  val invalidEligibilityJson = Json.parse(
    """
      |{
      | "result": "success"
      |}
    """.stripMargin).as[JsObject]

  "getEligibility" should {
    "return 200" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      await(client(controllers.routes.EligibilityController.getEligibility("regId").url).get() map { response =>
        response.status shouldBe 200
        response.json shouldBe Json.parse("""{"version":1,"result":"success"}""")
      })
    }

    "return 404 if no document found" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.EligibilityController.getEligibility("regId").url).get() map { response =>
        response.status shouldBe 404
      })
    }

    "return 403 if user is not authorised" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.EligibilityController.getEligibility("regId").url).get() map { response =>
        response.status shouldBe 403
      })
    }
  }

  "updatingEligibility" should {
    "return 200 with an eligibility json body" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.EligibilityController.updateEligibility("regId").url).patch(validEligibilityJson) map { response =>
        response.status shouldBe 200
        response.json shouldBe validEligibilityJson
      })
    }

    "return 400 if an invalid json body is posted" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(emptyVatScheme("regId"))

      await(client(controllers.routes.EligibilityController.updateEligibility("regId").url).patch(invalidEligibilityJson) map { response =>
        response.status shouldBe 400
      })
    }

    "return 404 if no reg document is found" in new Setup {
      given
        .user.isAuthorised

      await(client(controllers.routes.EligibilityController.updateEligibility("regId").url).patch(validEligibilityJson) map { response =>
        response.status shouldBe 404
      })
    }

    "return 200 if no data updated because data is same" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(vatScheme("regId"))

      await(client(controllers.routes.EligibilityController.updateEligibility("regId").url).patch(validEligibilityJson) map { response =>
        response.status shouldBe 200
      })
    }

    "return 403 if user is not authorised obtained" in new Setup {
      given
        .user.isNotAuthorised

      await(client(controllers.routes.EligibilityController.updateEligibility("regId").url).patch(validEligibilityJson) map { response =>
        response.status shouldBe 403
      })
    }
  }
}
