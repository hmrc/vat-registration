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

import enums.VatRegStatus
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.MockRegistrationService
import models.api.{ApplicantDetails, Attachments, EmailMethod, Entity}
import models.registration._
import models.submission.{Individual, PartyType, UkCompany}
import play.api.libs.json.{Format, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class SectionValidationServiceSpec extends VatRegSpec with MockRegistrationService with VatRegistrationFixture {

  object Service extends SectionValidationService(mockRegistrationService)
  implicit val request: Request[_] = FakeRequest()

  "validate" when {
    "the section is Applicant"                 must {
      "return ValidSection when the data is valid" in {
        mockGetAnswer[PartyType](testInternalId, testRegId, EligibilitySectionId, "partyType")(
          Future.successful(Some(UkCompany))
        )
        val data = Json.toJson(validApplicantDetails)(
          Format[ApplicantDetails](ApplicantDetails.reads(UkCompany), ApplicantDetails.writes)
        )
        val res  = await(Service.validate(testInternalId, testRegId, ApplicantSectionId, data))

        res mustBe Right(ValidSection(data))
      }
    }
    "the section is Attachments"               must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(Attachments(Some(EmailMethod)))
        val res  = await(Service.validate(testInternalId, testRegId, AttachmentsSectionId, data))

        res mustBe Right(ValidSection(data))
      }
    }
    "the section is BankAccount"               must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(testBankAccount)
        val res  = await(Service.validate(testInternalId, testRegId, BankAccountSectionId, data))

        res mustBe Right(ValidSection(data))
      }
    }
    "the section is Business"                  must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(testBusiness)
        val res  = await(Service.validate(testInternalId, testRegId, BusinessSectionId, data))

        res mustBe Right(ValidSection(data))
      }
    }
    "the section is Eligibility Json"          must {
      "return ValidSection for any valid json" in {
        val data = Json.obj()
        val res  = await(Service.validate(testInternalId, testRegId, EligibilityJsonSectionId, data))

        res mustBe Right(ValidSection(data))
      }
    }
    "the section is Eligibility"               must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(testEligibilitySubmissionData)
        val res  = await(Service.validate(testInternalId, testRegId, EligibilitySectionId, data))

        res mustBe Right(ValidSection(data))
      }
      "return InvalidSection when required fields is missing" in {
        val res = await(Service.validate(testInternalId, testRegId, EligibilitySectionId, Json.obj()))

        res mustBe Left(InvalidSection(Seq("/registrationReason", "/threshold", "/partyType")))
      }
    }
    "the section is FlatRateScheme"            must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(validFullFlatRateScheme)
        val res  = await(Service.validate(testInternalId, testRegId, FlatRateSchemeSectionId, data))

        res mustBe Right(ValidSection(data))
      }
    }
    "the section is VatApplication"            must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(testVatApplicationDetails)
        val res  = await(Service.validate(testInternalId, testRegId, VatApplicationSectionId, data))

        res mustBe Right(ValidSection(data))
      }
    }
    "the section is Transactor"                must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(validTransactorDetails)
        val res  = await(Service.validate(testInternalId, testRegId, TransactorSectionId, data))

        res mustBe Right(ValidSection(data))
      }
    }
    "the section is OtherBusinessInvolvements" must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(List(validFullOtherBusinessInvolvement))
        val res  = await(Service.validate(testInternalId, testRegId, OtherBusinessInvolvementsSectionId, data))

        res mustBe Right(ValidSection(data))
      }

      "return InvalidSection when the data is invalid" in {
        val data = Json.toJson(List(Json.toJson("hasVrn" -> "notBoolean")))
        val res  = await(Service.validate(testInternalId, testRegId, OtherBusinessInvolvementsSectionId, data))

        res mustBe Left(InvalidSection(List("(0)")))
      }
    }
    "the section is Entities"                  must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(
          List(
            Entity(
              details = Some(testSoleTraderEntity),
              partyType = Individual,
              isLeadPartner = Some(true),
              address = Some(testAddress),
              email = Some(testEmail),
              telephoneNumber = Some(testTelephone)
            )
          )
        )
        val res  = await(Service.validate(testInternalId, testRegId, EntitiesSectionId, data))

        res mustBe Right(ValidSection(data))
      }

      "return InvalidSection when the data is invalid" in {
        val data = Json.toJson(List(Json.obj()))
        val res  = await(Service.validate(testInternalId, testRegId, EntitiesSectionId, data))

        res mustBe Left(InvalidSection(List("(0)/partyType")))
      }
    }
    "the section is Status"                    must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(VatRegStatus.draft)
        val res  = await(Service.validate(testInternalId, testRegId, StatusSectionId, data))

        res mustBe Right(ValidSection(data))
      }
      "return InvalidSection when the data is invalid" in {
        val data = Json.obj()
        val res  = await(Service.validate(testInternalId, testRegId, StatusSectionId, data))

        res mustBe Left(InvalidSection(List("")))
      }
    }
    "the section is InformationDeclaration"    must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(true)
        val res  = await(Service.validate(testInternalId, testRegId, InformationDeclarationSectionId, data))

        res mustBe Right(ValidSection(data))
      }
      "return InvalidSection when the data is invalid" in {
        val data = Json.obj()
        val res  = await(Service.validate(testInternalId, testRegId, InformationDeclarationSectionId, data))

        res mustBe Left(InvalidSection(List("")))
      }
    }
    "the section is ApplicationReference"      must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson("test")
        val res  = await(Service.validate(testInternalId, testRegId, ApplicationReferenceSectionId, data))

        res mustBe Right(ValidSection(data))
      }
      "return InvalidSection when the data is invalid" in {
        val data = Json.obj()
        val res  = await(Service.validate(testInternalId, testRegId, ApplicationReferenceSectionId, data))

        res mustBe Left(InvalidSection(List("")))
      }
    }
    "the section is AcknowledgementReference"  must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson("test")
        val res  = await(Service.validate(testInternalId, testRegId, AcknowledgementReferenceSectionId, data))

        res mustBe Right(ValidSection(data))
      }
      "return InvalidSection when the data is invalid" in {
        val data = Json.obj()
        val res  = await(Service.validate(testInternalId, testRegId, AcknowledgementReferenceSectionId, data))

        res mustBe Left(InvalidSection(List("")))
      }
    }
    "the section is NrsSubmissionPayload"      must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson("test")
        val res  = await(Service.validate(testInternalId, testRegId, NrsSubmissionPayloadSectionId, data))

        res mustBe Right(ValidSection(data))
      }
      "return InvalidSection when the data is invalid" in {
        val data = Json.obj()
        val res  = await(Service.validate(testInternalId, testRegId, NrsSubmissionPayloadSectionId, data))

        res mustBe Left(InvalidSection(List("")))
      }
    }
  }

  "validateIndex" when {
    "the section is OtherBusinessInvolvements" must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(validFullOtherBusinessInvolvement)
        val res  = await(Service.validateIndex(OtherBusinessInvolvementsSectionId, data))

        res mustBe Right(ValidSection(data))
      }

      "return InvalidSection when the data is invalid" in {
        val data = Json.toJson("hasVrn" -> "notBoolean")
        val res  = await(Service.validateIndex(OtherBusinessInvolvementsSectionId, data))

        res mustBe Left(InvalidSection(List("")))
      }
    }
    "the section is Entities"                  must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(
          Entity(
            details = Some(testSoleTraderEntity),
            partyType = Individual,
            isLeadPartner = Some(true),
            address = Some(testAddress),
            email = Some(testEmail),
            telephoneNumber = Some(testTelephone)
          )
        )
        val res  = await(Service.validateIndex(EntitiesSectionId, data))

        res mustBe Right(ValidSection(data))
      }

      "return InvalidSection when the data is invalid" in {
        val data = Json.obj()
        val res  = await(Service.validateIndex(EntitiesSectionId, data))

        res mustBe Left(InvalidSection(List("/partyType")))
      }
    }
  }

}
