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

import auth.CryptoSCRS
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.MockRegistrationService
import models.api.{ApplicantDetails, Attachments, EmailMethod}
import models.registration._
import models.submission.{PartyType, UkCompany}
import play.api.libs.json.{Format, Json}
import play.api.test.Helpers._

import scala.concurrent.Future

class SectionValidationServiceSpec extends VatRegSpec
  with MockRegistrationService
  with VatRegistrationFixture {

  object Service extends SectionValidationService(mockRegistrationService)

  val crypto = app.injector.instanceOf[CryptoSCRS]

  "validate" when {
    "the section is Applicant" must {
      "return ValidSection when the data is valid" in {
        mockGetAnswer[PartyType](testInternalId, testRegId, EligibilitySectionId, "partyType")(Future.successful(Some(UkCompany)))
        val data = Json.toJson(validApplicantDetails)(Format[ApplicantDetails](ApplicantDetails.reads(UkCompany), ApplicantDetails.writes))
        val res = await(Service.validate(testInternalId, testRegId, ApplicantSectionId, data))

        res mustBe Right(ValidSection(data, true))
      }
    }
    "the section is Attachments" must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(Attachments(EmailMethod))
        val res = await(Service.validate(testInternalId, testRegId, AttachmentsSectionId, data))

        res mustBe Right(ValidSection(data, true))
      }
    }
    "the section is BankAccount" must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(testBankAccount)
        val res = await(Service.validate(testInternalId, testRegId, BankAccountSectionId, data))

        res mustBe Right(ValidSection(data, true))
      }
    }
    "the section is BusinessContact (legacy)" must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(testBusinessContact)
        val res = await(Service.validate(testInternalId, testRegId, BusinessContactSectionId, data))

        res mustBe Right(ValidSection(data, true))
      }
    }
    "the section is Compliance" must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(testSicAndCompliance)
        val res = await(Service.validate(testInternalId, testRegId, ComplianceSectionId, data))

        res mustBe Right(ValidSection(data, true))
      }
    }
    "the section is Eligibility" must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(testEligibilitySubmissionData)
        val res = await(Service.validate(testInternalId, testRegId, EligibilitySectionId, data))

        res mustBe Right(ValidSection(data, true))
      }
      "return InvalidSection when required fields is missing" in {
        val res = await(Service.validate(testInternalId, testRegId, EligibilitySectionId, Json.obj()))


        res mustBe Left(InvalidSection(Seq("/exceptionOrExemption", "/registrationReason", "/threshold", "/estimates", "/customerStatus", "/partyType")))
      }
    }
    "the section is FlatRateScheme (legacy)" must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(validFullFlatRateScheme)
        val res = await(Service.validate(testInternalId, testRegId, FlatRateSchemeSectionId, data))

        res mustBe Right(ValidSection(data, true))
      }
    }
    "the section is Returns" must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(testReturns)
        val res = await(Service.validate(testInternalId, testRegId, ReturnsSectionId, data))

        res mustBe Right(ValidSection(data, true))
      }
    }
    "the section is Transactor" must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(validTransactorDetails)
        val res = await(Service.validate(testInternalId, testRegId, TransactorSectionId, data))

        res mustBe Right(ValidSection(data, true))
      }
    }
    "the section is TradingDetails (legacy)" must {
      "return ValidSection when the data is valid" in {
        val data = Json.toJson(validFullTradingDetails)
        val res = await(Service.validate(testInternalId, testRegId, TradingDetailsSectionId, data))

        res mustBe Right(ValidSection(data, true))
      }
    }
  }

}