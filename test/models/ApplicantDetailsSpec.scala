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

package models

import fixtures.VatRegistrationFixture
import helpers.BaseSpec
import models.api._
import models.submission._
import play.api.libs.json._

class ApplicantDetailsSpec extends BaseSpec with JsonFormatValidation with VatRegistrationFixture {

  private def writeAndRead[T](t: T)(implicit fmt: Format[T]) = fmt.reads(Json.toJson(fmt.writes(t)))
  private def format(partyType: PartyType) = Format(ApplicantDetails.reads(partyType), ApplicantDetails.writes)

  "Creating a Json from a valid VatApplicantDetails model" should {
    "parse successfully if the entity is a Ltd Co" in {
      implicit val fmt = format(UkCompany)
      writeAndRead(validApplicantDetails) resultsIn validApplicantDetails
    }

    "parse successfully if the entity is a Sole Trader" in {
      implicit val fmt = format(Individual)
      val soleTraderAppDetails = validApplicantDetails.copy(
        entity = testSoleTraderEntity
      )
      writeAndRead(soleTraderAppDetails) resultsIn soleTraderAppDetails
    }

    "parse successfully if the entity is a General Partnership" in {
      implicit val fmt = format(Partnership)
      val generalPartnershipAppDetails = validApplicantDetails.copy(
        entity = testGeneralPartnershipEntity
      )
      writeAndRead(generalPartnershipAppDetails) resultsIn generalPartnershipAppDetails
    }

    "parse successfully if the entity is a Trust" in {
      implicit val fmt = format(Trust)
      val trustAppDetails = validApplicantDetails.copy(
        entity = testTrustEntity
      )
      writeAndRead(trustAppDetails) resultsIn trustAppDetails
    }
  }

  "Creating a Json from an invalid VatApplicantDetails model" ignore {
    "fail with a JsonValidationError" when {
      "NINO is invalid" in {
        implicit val fmt = format(UkCompany)
        val applicantDetails = validApplicantDetails.copy(transactor = validApplicantDetails.transactor.copy(nino = Some("NB888")))
        writeAndRead(applicantDetails) shouldHaveErrors (JsPath() \ "transactor" \ "nino" -> JsonValidationError("error.pattern"))
      }

      "Name is invalid" in {
        implicit val fmt = format(UkCompany)
        val name = Name(first = Some("$%@$%^@#%@$^@$^$%@#$%@#$"), middle = None, last = "valid name")
        val applicantDetails = validApplicantDetails.copy(
          transactor = validApplicantDetails.transactor.copy(name = name)
        )
        writeAndRead(applicantDetails) shouldHaveErrors (JsPath() \ "transactor" \ "firstName" -> JsonValidationError("error.pattern"))
      }
    }
  }
}