/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import config.BackendConfig
import enums.VatRegStatus
import featureswitch.core.config.FeatureSwitching
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.{MockDailyQuotaRepository, MockTrafficManagementService, MockVatSchemeRepository}
import models.api._
import models.registration.TransactorSectionId
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.FakeRegistrationIdService

import java.time.LocalDate
import scala.concurrent.Future

class RegistrationServiceSpec extends VatRegSpec
  with MockDailyQuotaRepository
  with MockTrafficManagementService
  with MockVatSchemeRepository
  with FeatureSwitching
  with BeforeAndAfterEach
  with VatRegistrationFixture {

  implicit val hc = HeaderCarrier()
  implicit val config = app.injector.instanceOf[BackendConfig]
  override lazy val testRegId = "testRegId"
  override lazy val testInternalId = "testInternalId"
  override lazy val testDate = LocalDate.parse("2020-01-01")

  override lazy val testVatScheme = VatScheme(
    id = testRegId,
    internalId = testInternalId,
    status = VatRegStatus.draft
  )

  val testRegInfo = RegistrationInformation(
    internalId = testInternalId,
    registrationId = testRegId,
    status = Draft,
    regStartDate = testDate,
    channel = VatReg,
    lastModified = testDate
  )

  val fakeRegIdService = new FakeRegistrationIdService

  object Service extends RegistrationService(
    vatSchemeRepository = mockVatSchemeRepository,
    registrationIdService = fakeRegIdService
  )

  "newRegistration" should {
    "return a vat scheme" in {
      mockNewVatScheme(testRegId, testInternalId)(testVatScheme)

      val res = await(Service.newRegistration(testInternalId))

      res mustBe testVatScheme
    }
  }

  "insertVatScheme" should {
    "return a vat scheme" in {
      mockInsertVatScheme(testVatScheme)

      val res = await(Service.insertVatScheme(testVatScheme))

      res mustBe testVatScheme
    }
  }

  "getAllRegistrations" when {
    "registrations exist" when {
      "requested as a Registration" must {
        "return a list of schemes in VAT scheme format" in {
          val registrationsJson = Json.toJson(testVatScheme)(VatScheme.format())
          mockGetAllRegistrations(testInternalId)(Future.successful(List(registrationsJson)))

          val res = await(Service.getAllRegistrations[VatScheme](testInternalId)(VatScheme.format()))

          res mustBe List(testVatScheme)
        }
      }
      "requested as JSON" must {
        "return a list of registrations in JSON format" in {
          val registrationsJson = Json.toJson(testVatScheme)(VatScheme.format())
          mockGetAllRegistrations(testInternalId)(Future.successful(List(registrationsJson)))

          val res = await(Service.getAllRegistrations[JsValue](testInternalId))

          res mustBe List(registrationsJson)
        }
      }
    }
    "The user has no registrations" must {
      "return NIl" in {
        mockGetAllRegistrations(testInternalId)(Future.successful(Nil))

        val res = await(Service.getAllRegistrations[JsValue](testInternalId))

        res mustBe Nil
      }
    }
  }

  "getRegistration" when {
    "the requested registration exists" when {
      "requested as a VAT scheme" must {
        "return the registration as a VAT scheme" in {
          val registrationJson = Json.toJson(testVatScheme)(VatScheme.format())
          mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(registrationJson)))

          val res = await(Service.getRegistration[VatScheme](testInternalId, testRegId)(VatScheme.format()))

          res mustBe Some(testVatScheme)
        }
      }
      "requested as JSON" must {
        "return the registration in JSON format" in {
          val registrationJson = Json.toJson(testVatScheme)(VatScheme.format())
          mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(registrationJson)))

          val res = await(Service.getRegistration[JsValue](testInternalId, testRegId))

          res mustBe Some(registrationJson)
        }
      }
    }
    "the requested registration doesn't exist" must {
      "return None" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(None))

        val res = await(Service.getRegistration[JsValue](testInternalId, testRegId))

        res mustBe None
      }
    }
  }

  "upsertRegistration" must {
    "return the updated registration in JSON format" in {
      val updatedJson = Json.toJson(testVatScheme.copy(status = VatRegStatus.locked))(VatScheme.format())
      mockUpsertRegistration(testInternalId, testRegId, updatedJson)(Future.successful(Some(updatedJson)))

      val res = await(Service.upsertRegistration[JsValue](testInternalId, testRegId, updatedJson))

      res mustBe Some(updatedJson)
    }
  }

  "deleteRegistration" must {
    "return true" in {
      mockDeleteRegistration(testInternalId, testRegId)(Future.successful(true))

      val res = await(Service.deleteRegistration(testInternalId, testRegId))

      res mustBe true
    }
  }

  "getSection" when {
    "the section exists" when {
      "requested parsed into the relevant model" must {
        "return the section as the relevant model" in {
          mockGetSection[JsValue](testInternalId, testRegId, TransactorSectionId.repoKey)(Future.successful(Some(Json.toJson(validTransactorDetails))))

          val res = await(Service.getSection[TransactorDetails](testInternalId, testRegId, TransactorSectionId))

          res mustBe Some(validTransactorDetails)
        }
      }
      "requested as JSON" must {
        "return the section in JSON format" in {
          val sectionJson = Json.toJson(validTransactorDetails)
          mockGetSection[JsValue](testInternalId, testRegId, TransactorSectionId.repoKey)(Future.successful(Some(sectionJson)))

          val res = await(Service.getSection[JsValue](testInternalId, testRegId, TransactorSectionId))

          res mustBe Some(sectionJson)
        }
      }
    }
    "the section doesn't exist" must {
      "return None" in {
        mockGetSection[JsValue](testInternalId, testRegId, TransactorSectionId.repoKey)(Future.successful(None))

        val res = await(Service.getSection[JsValue](testInternalId, testRegId, TransactorSectionId))

        res mustBe None
      }
    }
  }

  "upsertSection" when {
    "return the updated section" in {
      val updateJson = Json.toJson(validTransactorDetails)
      mockUpsertSection[JsValue](testInternalId, testRegId, TransactorSectionId.repoKey, updateJson)(Some(updateJson))

      val res = await(Service.upsertSection(testInternalId, testRegId, TransactorSectionId, updateJson))

      res mustBe Some(updateJson)
    }
    "return None if the section can't be updated" in {
      val updateJson = Json.toJson(validTransactorDetails)
      mockUpsertSection[JsValue](testInternalId, testRegId, TransactorSectionId.repoKey, updateJson)(None)

      val res = await(Service.upsertSection(testInternalId, testRegId, TransactorSectionId, updateJson))

      res mustBe None
    }
  }

  "getAnswer" when {
    "the answer exists" must {
      "return the answer as the requested type if it will parse to it" in {
        val sectionJson = Json.toJson(validTransactorDetails)
        mockGetSection[JsValue](testInternalId, testRegId, TransactorSectionId.repoKey)(Future.successful(Some(sectionJson)))

        val res = await(Service.getAnswer[PersonalDetails](testInternalId, testRegId, TransactorSectionId, "personalDetails"))

        res mustBe Some(PersonalDetails(
          name = testName,
          nino = Some(testNino),
          trn = None,
          identifiersMatch = true,
          dateOfBirth = testDate
        ))
      }
      "return the answer in JSON format" in {
        val sectionJson = Json.toJson(validTransactorDetails)
        mockGetSection[JsValue](testInternalId, testRegId, TransactorSectionId.repoKey)(Future.successful(Some(sectionJson)))

        val res = await(Service.getAnswer[JsValue](testInternalId, testRegId, TransactorSectionId, "personalDetails"))

        res mustBe Some(Json.toJson(PersonalDetails(
          name = testName,
          nino = Some(testNino),
          trn = None,
          identifiersMatch = true,
          dateOfBirth = testDate
        )))
      }
    }
    "the answer doesn't exist" must {
      "return None" in {
        val sectionJson = Json.toJson(validTransactorDetails)
        mockGetSection[JsValue](testInternalId, testRegId, TransactorSectionId.repoKey)(Future.successful(Some(sectionJson)))

        val res = await(Service.getAnswer[JsValue](testInternalId, testRegId, TransactorSectionId, "trn"))

        res mustBe None
      }
    }
  }

}
