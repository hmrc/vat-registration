/*
 * Copyright 2023 HM Revenue & Customs
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
import mocks.MockVatSchemeRepository
import models.api._
import models.registration.{OtherBusinessInvolvementsSectionId, TransactorSectionId}
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.FakeRegistrationIdService

import java.time.{LocalDate, LocalDateTime, LocalTime}
import scala.concurrent.Future

class RegistrationServiceSpec extends VatRegSpec
  with MockVatSchemeRepository
  with FeatureSwitching
  with BeforeAndAfterEach
  with VatRegistrationFixture {

  implicit val hc = HeaderCarrier()
  implicit val config = app.injector.instanceOf[BackendConfig]
  val testValidIndex = 1
  val testSecondIndex = 2
  override lazy val testRegId = "testRegId"
  override lazy val testInternalId = "testInternalId"
  override lazy val testDate = LocalDate.parse("2020-01-01")
  override lazy val testDateTime: LocalDateTime = LocalDateTime.of(testDate, LocalTime.MIDNIGHT)

  override lazy val testVatScheme = VatScheme(
    registrationId = testRegId,
    internalId = testInternalId,
    status = VatRegStatus.draft,
    createdDate = testDate
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
    "the requested registration exists" must {
      "return the registration as a VAT scheme" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testVatScheme)))

        val res = await(Service.getRegistration(testInternalId, testRegId))

        res mustBe Some(testVatScheme)
      }
    }
    "the requested registration doesn't exist" must {
      "return None" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(None))

        val res = await(Service.getRegistration(testInternalId, testRegId))

        res mustBe None
      }
    }
  }

  "upsertRegistration" must {
    "return the updated registration in JSON format" in {
      val scheme = testVatScheme.copy(status = VatRegStatus.locked)
      mockUpsertRegistration(testInternalId, testRegId, scheme)(Future.successful(Some(scheme)))

      val res = await(Service.upsertRegistration(testInternalId, testRegId, scheme))

      res mustBe Some(scheme)
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

  "deleteSection" when {
    "return true" in {
      mockDeleteSection(testInternalId, testRegId, TransactorSectionId.repoKey)(true)

      val res = await(Service.deleteSection(testInternalId, testRegId, TransactorSectionId))

      res mustBe true
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
          arn = None,
          identifiersMatch = true,
          dateOfBirth = Some(testDate),
          score = None
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
          arn = None,
          identifiersMatch = true,
          dateOfBirth = Some(testDate),
          score = None
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

  "getSectionIndex" must {
    "return the index" in {
      val sectionJson = Json.toJson(validFullOtherBusinessInvolvement)
      val sectionJsonArr = Json.arr(sectionJson)
      mockGetSection[JsValue](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(Some(sectionJsonArr)))

      val res = await(Service.getSectionIndex(testInternalId, testRegId, OtherBusinessInvolvementsSectionId, testValidIndex))

      res mustBe Some(sectionJson)
    }
    "return the second index" in {
      val sectionJson = Json.toJson(validFullOtherBusinessInvolvement)
      val sectionJsonArr = Json.arr(sectionJson, sectionJson)
      mockGetSection[JsValue](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(Some(sectionJsonArr)))

      val res = await(Service.getSectionIndex(testInternalId, testRegId, OtherBusinessInvolvementsSectionId, testSecondIndex))

      res mustBe Some(sectionJson)
    }
    "return None" when {
      "there are no sections" in {
        mockGetSection[JsValue](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(None))

        val res = await(Service.getSectionIndex(testInternalId, testRegId, OtherBusinessInvolvementsSectionId, testValidIndex))

        res mustBe None
      }
      "the list does not contain that index" in {
        val sectionJson = Json.arr(Json.toJson(validFullOtherBusinessInvolvement))
        mockGetSection[JsValue](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(Some(sectionJson)))

        val res = await(Service.getSectionIndex(testInternalId, testRegId, OtherBusinessInvolvementsSectionId, testSecondIndex))

        res mustBe None
      }
    }
  }

  "upsertSectionIndex" must {
    "return the stored json after updating an existing index in the section" in {
      val sectionJson = Json.arr(Json.toJson(validFullOtherBusinessInvolvement))
      val updateJson = Json.toJson(validFullOtherBusinessInvolvement.copy(stillTrading = Some(false)))
      val updateJsonArr = Json.arr(Json.toJson(validFullOtherBusinessInvolvement.copy(stillTrading = Some(false))))
      mockGetSection[JsValue](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(Some(sectionJson)))
      mockUpsertSection[JsValue](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey, updateJsonArr)(Some(updateJsonArr))

      val res = await(Service.upsertSectionIndex(testInternalId, testRegId, OtherBusinessInvolvementsSectionId, updateJson, testValidIndex))

      res mustBe Some(updateJson)
    }
    "return the stored json after creating the section" in {
      val updateJson = Json.arr(Json.toJson((validFullOtherBusinessInvolvement)))
      mockGetSection[JsValue](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(None))
      mockUpsertSection[JsValue](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey, updateJson)(Some(updateJson))

      val res = await(Service.upsertSectionIndex(testInternalId, testRegId, OtherBusinessInvolvementsSectionId, Json.toJson(validFullOtherBusinessInvolvement), testValidIndex))

      res mustBe Some(Json.toJson(validFullOtherBusinessInvolvement))
    }
    "return the stored json after adding a new index to an existing section" in {
      val sectionJson = Json.arr(Json.toJson(validFullOtherBusinessInvolvement))
      val updateJson = Json.arr(Json.toJson(validFullOtherBusinessInvolvement), Json.toJson(validFullOtherBusinessInvolvement))
      mockGetSection[JsValue](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(Some(sectionJson)))
      mockUpsertSection[JsValue](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey, updateJson)(Some(updateJson))

      val res = await(Service.upsertSectionIndex(testInternalId, testRegId, OtherBusinessInvolvementsSectionId, Json.toJson(validFullOtherBusinessInvolvement), testSecondIndex))

      res mustBe Some(Json.toJson(validFullOtherBusinessInvolvement))
    }
  }

  "deleteSectionIndex" must {
    "return the array without the deleted index" in {
      val sectionJson = Json.arr(Json.toJson(validFullOtherBusinessInvolvement), Json.toJson(validFullOtherBusinessInvolvement.copy(stillTrading = Some(false))))
      val updateJson = Json.arr(Json.toJson(validFullOtherBusinessInvolvement.copy(stillTrading = Some(false))))
      mockGetSection[JsValue](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(Some(sectionJson)))
      mockUpsertSection[JsValue](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey, updateJson)(Some(updateJson))

      val res = await(Service.deleteSectionIndex(testInternalId, testRegId, OtherBusinessInvolvementsSectionId, testValidIndex))

      res mustBe Some(updateJson)
    }
    "return a None when deleting the last index" in {
      val sectionJson = Json.arr(Json.toJson(validFullOtherBusinessInvolvement))
      mockGetSection[JsValue](testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(Future.successful(Some(sectionJson)))
      mockDeleteSection(testInternalId, testRegId, OtherBusinessInvolvementsSectionId.repoKey)(response = true)

      val res = await(Service.deleteSectionIndex(testInternalId, testRegId, OtherBusinessInvolvementsSectionId, testValidIndex))

      res mustBe None
    }
  }

}
