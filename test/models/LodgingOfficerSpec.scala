/*
 * Copyright 2017 HM Revenue & Customs
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

import java.time.LocalDate

import fixtures.VatRegistrationFixture
import models.api._
import play.api.data.validation.ValidationError
import play.api.libs.json.{Format, JsPath, Json}

class LodgingOfficerSpec extends JsonFormatValidation with VatRegistrationFixture {

  private def writeAndRead[T](t: T)(implicit fmt: Format[T]) = fmt.reads(Json.toJson(fmt.writes(t)))

  implicit val format = LodgingOfficer.format

  val vatLodgingOfficer = LodgingOfficer(
    currentAddress           = Some(scrsAddress),
    dob                      = LocalDate.of(1990, 1, 1),
    nino                     = "NB686868C",
    role                     = "director",
    name                     = name,
    changeOfName             = Some(changeOfName),
    currentOrPreviousAddress = Some(currentOrPreviousAddress),
    contact                  = Some(contact),
    details                  = None
  )

  "Creating a Json from a valid VatLodgingOfficer model" should {
    "complete successfully" in {
      writeAndRead(vatLodgingOfficer) resultsIn vatLodgingOfficer
    }
  }

  "Creating a Json from an invalid VatLodgingOfficer model" should {
    "fail with a ValidationError" when {
      "NINO is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(nino = "NB888")
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "nino" -> ValidationError("error.pattern"))
      }

      "Role is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(role = "magician")
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "role" -> ValidationError("error.pattern"))
      }

      "Name is invalid" in {
        val name = Name(first = "$%@$%^@#%@$^@$^$%@#$%@#$", middle = None, last = None, forename = Some("$%@$%^@#%@$^@$^$%@#$%@#$"))
        val lodgingOfficer = vatLodgingOfficer.copy(name = name)
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "name" \ "forename" -> ValidationError("error.pattern"))
      }

      "Contact email is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(contact = Some(OfficerContactDetails(Some("£$%^&&*"), None, None)))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "contact" \ "email" -> ValidationError("error.pattern"))
      }

      "Contact tel is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(contact = Some(OfficerContactDetails(None, Some("£$%^&&*"), None)))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "contact" \ "tel" -> ValidationError("error.pattern"))
      }

      "Contact mob is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(contact = Some(OfficerContactDetails(None, None, Some("£$%^&&*"))))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "contact" \ "mobile" -> ValidationError("error.pattern"))
      }
    }
  }
}
