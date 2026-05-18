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

package models.api

import enums.VatRegStatus
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.Voluntary
import models.submission._
import play.api.libs.json._
import models.api.vatapplication.VatApplication
import models.registration._
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate

class VatSchemeSpec extends VatRegSpec with VatRegistrationFixture {

  "exceptionOrExemption" must {
    "return exceptionKey" when {
      "eligibilitySubmissionData has appliedForException flag true" in {
        val result = VatScheme.exceptionOrExemption(
          testEligibilitySubmissionData.copy(appliedForException = Some(true)),
          testVatApplicationDetails
        )
        result mustBe VatScheme.exceptionKey
      }
    }
    "return exemptionKey" when {
      "Returns has appliedForExemption flag true" in {
        val result = VatScheme.exceptionOrExemption(
          testEligibilitySubmissionData,
          testVatApplicationDetails.copy(appliedForExemption = Some(true))
        )
        result mustBe VatScheme.exemptionKey
      }
    }
    "return nonExceptionOrExemptionKey" when {
      "both exception and exemption flags are false" in {
        val result = VatScheme.exceptionOrExemption(
          testEligibilitySubmissionData.copy(appliedForException = Some(false)),
          testVatApplicationDetails.copy(appliedForExemption = Some(false))
        )
        result mustBe VatScheme.nonExceptionOrExemptionKey
      }
      "both exception and exemption flags are not present" in {
        val result = VatScheme.exceptionOrExemption(
          testEligibilitySubmissionData.copy(appliedForException = None),
          testVatApplicationDetails.copy(appliedForExemption = None)
        )
        result mustBe VatScheme.nonExceptionOrExemptionKey
      }
    }
    "returns error" when {
      "both exception and exemption flags are true" in {
        intercept[InternalServerException] {
          VatScheme.exceptionOrExemption(
            testEligibilitySubmissionData.copy(appliedForException = Some(true)),
            testVatApplicationDetails.copy(appliedForExemption = Some(true))
          )
        }
      }
    }
  }

  private val baseVatSchema = VatScheme("regId", "internalId", LocalDate.of(2020, 2, 2), VatRegStatus.draft)

  def eligibilitySubmissionData(partyType: PartyType) =
    EligibilitySubmissionData(Threshold(true), None, partyType = partyType, Voluntary, None, true, None, true)

  "partyType" should {
    "return the eligibilitySubmissionData.partyType in a Some when present" in {
      val testModelIndividual = baseVatSchema.copy(eligibilitySubmissionData = Some(eligibilitySubmissionData(Individual)))
      val testModelAdmin = baseVatSchema.copy(eligibilitySubmissionData = Some(eligibilitySubmissionData(AdminDivision)))

      testModelIndividual.partyType mustBe Some(Individual)
      testModelAdmin.partyType mustBe Some(AdminDivision)
    }
    "return None when there is no eligibilitySubmissionData data" in {
      val testModelWithNoEligibilitySubmissionData = baseVatSchema.copy(eligibilitySubmissionData = None)

      testModelWithNoEligibilitySubmissionData.partyType mustBe None
    }
  }

  "partyTypeIsIndividualOrNonUkNonEstablished" should {
    "return 'true' when the eligibilitySubmissionData.partyType in present" when {
      "partyType 'Individual'" in {
        val testModelIndividual = baseVatSchema.copy(eligibilitySubmissionData = Some(eligibilitySubmissionData(Individual)))

        testModelIndividual.partyTypeIsIndividualOrNonUkNonEstablished mustBe true
      }
      "partyType 'NonUkNonEstablished'" in {
        val testModelNonUkNonEstablished = baseVatSchema.copy(eligibilitySubmissionData = Some(eligibilitySubmissionData(NonUkNonEstablished)))

        testModelNonUkNonEstablished.partyTypeIsIndividualOrNonUkNonEstablished mustBe true
      }
    }

    "return 'false'" when {
      "there is no eligibilitySubmissionData" in {
        val testModelWithNoEligibilitySubmissionData = baseVatSchema.copy(eligibilitySubmissionData = None)

        testModelWithNoEligibilitySubmissionData.partyTypeIsIndividualOrNonUkNonEstablished mustBe false
      }
      "partyType is not 'Individual' or 'NonUkNonEstablished'" in {
        val testModelRegSociety = baseVatSchema.copy(eligibilitySubmissionData = Some(eligibilitySubmissionData(RegSociety)))
        val testModelGovOrg = baseVatSchema.copy(eligibilitySubmissionData = Some(eligibilitySubmissionData(GovOrg)))

        testModelRegSociety.partyTypeIsIndividualOrNonUkNonEstablished mustBe false
        testModelGovOrg.partyTypeIsIndividualOrNonUkNonEstablished mustBe false
      }
    }
  }

  private val baseDate = LocalDate.of(2020, 2, 2)

  /** Minimal JSON that satisfies the reads when no partyType is present */
  private def baseJson(extraFields: (String, JsValue)*): JsObject =
    Json.obj(
      "registrationId" -> "registrationId",
      "internalId" -> "internalId",
      "createdDate" -> "2020-02-02",
      "status" -> "draft"
    ) ++ JsObject(extraFields)

  /** Minimal JSON with an eligibility block that contains a partyType */
  private def baseJsonWithPartyType(partyType: PartyType, extraFields: (String, JsValue)*): JsObject =
    baseJson(extraFields: _*) ++ Json.obj(
      EligibilitySectionId.repoKey -> Json.obj("partyType" -> Json.toJson(partyType))
    )

  "VatScheme" must {
    "read from Json" when {
      "eligibilitySubmissionData is null, so no model is created" in {
        val minimalJson = Json.obj(
          "registrationId" -> "registrationId",
          "internalId" -> "internalId",
          "status" -> "draft"
        )

        minimalJson.as[VatScheme](VatScheme.reads()) mustBe null
      }

      "given the minimal Json for a VatScheme with no partyType and no createdDate, creating a default LocalDate.MIN" in {
        val minimalJson = Json.obj(
          "registrationId" -> "registrationId",
          "internalId" -> "internalId",
          "status" -> "draft"
        )
        val expectedModel = VatScheme("registrationId", "internalId", LocalDate.MIN, VatRegStatus.draft)

        minimalJson.as[VatScheme](VatScheme.reads()) mustBe expectedModel
      }

      "return a JsError" when {
        "registrationId is missing" in {
          val json = Json.obj(
            "internalId" -> "internalId",
            StatusSectionId.repoKey -> Json.toJson(VatRegStatus.draft)
          )
          Json.fromJson[VatScheme](json)(VatScheme.reads()) mustBe a[JsError]
        }
        "internalId is missing" in {
          val json = Json.obj(
            "registrationId" -> "registrationId",
            StatusSectionId.repoKey -> Json.toJson(VatRegStatus.draft)
          )
          Json.fromJson[VatScheme](json)(VatScheme.reads()) mustBe a[JsError]
        }
        "status is missing" in {
          val json = Json.obj(
            "registrationId" -> "registrationId",
            "internalId" -> "internalId"
          )
          Json.fromJson[VatScheme](json)(VatScheme.reads()) mustBe a[JsError]
        }
      }
    }


    "write to Json" when {
      "VatScheme model has the minimum fields" in {
        val expectedMinimalJson = Json.obj(
          "registrationId" -> "registrationId",
          "internalId" -> "internalId",
          "status" -> "draft",
          "createdDate" -> "2022-02-02"
        )

        Json.toJson(baseVatSchema)(VatScheme.writes()) mustBe expectedMinimalJson
      }
    }
  }

}
