
package repository

import auth.CryptoSCRS
import itutil._
import models.api.VatScheme
import models.registration.{ApplicantSectionId, TransactorSectionId}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.Helpers.{await, _}
import reactivemongo.api.commands.WriteResult
import repositories.VatSchemeRepository
import services.RegistrationIdService

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationRepositoryISpec extends MongoBaseSpec
  with FutureAssertions
  with ITFixtures {

  val fakeRegIdService = new FakeRegistrationIdService

  override implicit lazy val app = GuiceApplicationBuilder()
    .overrides(bind[RegistrationIdService].to(fakeRegIdService))
    .build()

  val testRegId2 = "testRegId2"

  class Setup {
    val repo: VatSchemeRepository = app.injector.instanceOf[VatSchemeRepository]
    val crypto = app.injector.instanceOf[CryptoSCRS]

    def regAsJson(registration: VatScheme): JsValue = VatScheme.writes(Some(crypto)).writes(registration)

    def insert(json: JsObject): WriteResult = await(repo.collection.insert(json))

    def count: Int = await(repo.count)

    def fetchAll: Option[JsObject] = await(repo.collection.find(Json.obj()).one[JsObject])

    await(repo.drop)
    await(repo.ensureIndexes)
  }

  "createNewVatScheme()" must {
    "create a new registration for the user" in new Setup {
      FakeRegistrationIdService.id = testRegId
      await(repo.createNewVatScheme(testRegId, testInternalid))

      val res = await(repo.find("internalId" -> testInternalid))

      res mustBe List(testEmptyVatScheme(testRegId))
    }
  }

  "getAllRegistrations" must {
    "return a list of the JSON for all registrations" in new Setup {
      await(repo.createNewVatScheme(testRegId, testInternalid))
      await(repo.createNewVatScheme(testRegId2, testInternalid))

      val res = await(repo.getAllRegistrations(testInternalid))

      res mustBe List(
        regAsJson(testEmptyVatScheme(testRegId)),
        regAsJson(testEmptyVatScheme(testRegId2))
      )
    }
    "return an empty list when there are no registrations for the user" in new Setup {
      val res = await(repo.getAllRegistrations(testInternalid))

      res mustBe Nil
    }
    "return an empty list when there are no registrations for an internal ID but there are registrations fof another user" in new Setup {
      await(repo.createNewVatScheme(testRegId, testInternalid))

      val res = await(repo.getAllRegistrations("notARealInternalId"))

      res mustBe Nil
    }
  }

  "getRegistration" must {
    "return only the JSON for the specified registration" in new Setup {
      await(repo.createNewVatScheme(testRegId, testInternalid))
      await(repo.createNewVatScheme(testRegId2, testInternalid))

      val res = await(repo.getRegistration(testInternalid, testRegId))

      res mustBe Some(regAsJson(testEmptyVatScheme(testRegId)))
    }
    "return None if the registration doesn't exist" in new Setup {
      await(repo.createNewVatScheme(testRegId, testInternalid))

      val res = await(repo.getRegistration(testInternalid, testRegId2))

      res mustBe None
    }
  }

  "upsertRegistration" must {
    "update an existing registration" in new Setup {
      await(repo.createNewVatScheme(testRegId, testInternalid))
      val updatedRegistration = testEmptyVatScheme(testRegId).copy(eligibilitySubmissionData = Some(testEligibilitySubmissionData))

      val res = await(repo.upsertRegistration(testInternalid, testRegId, regAsJson(updatedRegistration)))

      res mustBe Some(regAsJson(updatedRegistration))
    }
    "create a registration if it doesn't exist" in new Setup {
      val json = regAsJson(testEmptyVatScheme(testRegId))
      val res = await(repo.upsertRegistration(testInternalid, testRegId, json))

      res mustBe Some(json)
    }
  }

  "deleteRegistration" must {
    "remove an existing registration" in new Setup {
      await(repo.createNewVatScheme(testRegId, testInternalid))

      val res = await(repo.deleteRegistration(testInternalid, testRegId))

      res mustBe true
    }
    "return true if the requested registration didn't exist" in new Setup {
      val res = await(repo.deleteRegistration(testInternalid, testRegId))

      res mustBe true
    }
  }

  "getSection" must {
    "get the section if it exists" in new Setup {
      await(repo.insertVatScheme(testFullVatScheme))

      val res = await(repo.getSection[JsValue](testInternalid, testRegId, ApplicantSectionId.repoKey))

      res mustBe Some(Json.toJson(testUnregisteredApplicantDetails))
    }
    "return None if the section doestn't exist" in new Setup {
      await(repo.createNewVatScheme(testRegId, testInternalid))

      val res = await(repo.getSection[JsValue](testInternalid, testRegId, TransactorSectionId.repoKey))

      res mustBe None
    }

    "return None if the registration doesn't exist" in new Setup {
      val res = await(repo.getSection[JsValue](testInternalid, testRegId, TransactorSectionId.repoKey))

      res mustBe None
    }
  }

  "upsertSection" must {
    "update the section if it exists" in new Setup {
      await(repo.createNewVatScheme(testRegId, testInternalid))

      val res = await(repo.upsertSection[JsValue](testInternalid, testRegId, TransactorSectionId.repoKey, Json.toJson(testTransactorDetails)))
      val regs = await(repo.find("internalId" -> testInternalid, "registrationId" -> testRegId))

      res mustBe Some(Json.toJson(testTransactorDetails))
      regs.headOption mustBe Some(testEmptyVatScheme(testRegId).copy(transactorDetails = Some(testTransactorDetails)))
    }
    "upsert the section if it doesn't exist" in new Setup {
      await(repo.createNewVatScheme(testRegId, testInternalid))

      val res = await(repo.upsertSection[JsValue](testInternalid, testRegId, TransactorSectionId.repoKey, Json.toJson(testTransactorDetails)))
      val regs = await(repo.find("internalId" -> testInternalid, "registrationId" -> testRegId))

      res mustBe Some(Json.toJson(testTransactorDetails))
      regs.headOption mustBe Some(testEmptyVatScheme(testRegId).copy(transactorDetails = Some(testTransactorDetails)))
    }
  }

}