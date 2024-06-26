/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package repository

import auth.CryptoSCRS
import itutil._
import models.api.VatScheme
import models.registration.{ApplicantSectionId, TransactorSectionId}
import org.mongodb.scala.model.Filters.{and, equal => mongoEqual}
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, _}
import repositories.VatSchemeRepository
import services.RegistrationIdService
import utils.TimeMachine

class RegistrationRepositoryISpec extends MongoBaseSpec
  with FutureAssertions
  with ITFixtures {

  implicit val request: Request[_] = FakeRequest()

  val fakeRegIdService = new FakeRegistrationIdService
  val fakeTimeMachine = new FakeTimeMachine

  override implicit lazy val app: Application = GuiceApplicationBuilder()
    .overrides(bind[RegistrationIdService].to(fakeRegIdService))
    .overrides(bind[TimeMachine].to(fakeTimeMachine))
    .build()

  val testRegId2 = "testRegId2"

  class Setup {
    val repo: VatSchemeRepository = app.injector.instanceOf[VatSchemeRepository]
    val crypto: CryptoSCRS = app.injector.instanceOf[CryptoSCRS]

    def regAsJson(registration: VatScheme): JsValue = VatScheme.writes(Some(crypto)).writes(registration).as[JsObject]

    await(repo.collection.drop().toFuture())
    await(repo.ensureIndexes())
  }

  "createNewVatScheme" must {
    "create a new registration for the user" in new Setup {
      FakeRegistrationIdService.id = testRegId
      await(repo.createNewVatScheme(testRegId, testInternalid))

      val res = await(repo.collection.find(mongoEqual("internalId", testInternalid)).toFuture())

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

      res mustBe Some(testEmptyVatScheme(testRegId))
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

      val res = await(repo.upsertRegistration(testInternalid, testRegId, updatedRegistration))

      res mustBe Some(updatedRegistration)
    }
    "partially update an existing registration" in new Setup {
      val testReg = testEmptyVatScheme(testRegId).copy(eligibilitySubmissionData = Some(testEligibilitySubmissionData))
      await(repo.upsertRegistration(testInternalid, testRegId, testReg))
      val updatedReg = testReg.copy(vatApplication = Some(testVatApplication.copy(appliedForExemption = Some(true))))

      val res = await(repo.upsertRegistration(testInternalid, testRegId, updatedReg))

      res mustBe Some(updatedReg)
    }
    "create a registration if it doesn't exist" in new Setup {
      val scheme = testEmptyVatScheme(testRegId)
      val res = await(repo.upsertRegistration(testInternalid, testRegId, scheme))

      res mustBe Some(scheme)
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
      await(repo.upsertRegistration(testInternalid, testRegId, testFullVatScheme))

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
      val regs = await(repo.collection.find(and(mongoEqual("internalId", testInternalid), mongoEqual("registrationId", testRegId))).toFuture())

      res mustBe Some(Json.toJson(testTransactorDetails))
      regs.headOption mustBe Some(testEmptyVatScheme(testRegId).copy(transactorDetails = Some(testTransactorDetails)))
    }
    "upsert the section if it doesn't exist" in new Setup {
      await(repo.createNewVatScheme(testRegId, testInternalid))

      val res = await(repo.upsertSection[JsValue](testInternalid, testRegId, TransactorSectionId.repoKey, Json.toJson(testTransactorDetails)))
      val regs = await(repo.collection.find(and(mongoEqual("internalId", testInternalid), mongoEqual("registrationId", testRegId))).toFuture())

      res mustBe Some(Json.toJson(testTransactorDetails))
      regs.headOption mustBe Some(testEmptyVatScheme(testRegId).copy(transactorDetails = Some(testTransactorDetails)))
    }
  }

  "deleteSection" must {
    "delete the section if it exists" in new Setup {
      await(repo.createNewVatScheme(testRegId, testInternalid))
      await(repo.upsertSection(testRegId, testInternalid, TransactorSectionId.repoKey, Json.toJson(testTransactorDetails)))

      val res = await(repo.deleteSection(testInternalid, testRegId, TransactorSectionId.repoKey))
      val regs = await(repo.collection.find(and(mongoEqual("internalId", testInternalid), mongoEqual("registrationId", testRegId))).toFuture())

      res mustBe true
      regs.headOption mustBe Some(testEmptyVatScheme(testRegId))
    }
    "pass after doing nothing if the registration doesn't exist" in new Setup {
      val res = await(repo.deleteSection(testInternalid, testRegId, TransactorSectionId.repoKey))
      val regs = await(repo.collection.find(and(mongoEqual("internalId", testInternalid), mongoEqual("registrationId", testRegId))).toFuture())

      res mustBe true
      regs.headOption mustBe None
    }
  }
}
