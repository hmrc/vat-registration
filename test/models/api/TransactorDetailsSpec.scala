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

import fixtures.VatRegistrationFixture
import helpers.BaseSpec
import models.JsonFormatValidation
import play.api.libs.json._

class TransactorDetailsSpec extends BaseSpec with JsonFormatValidation with VatRegistrationFixture {

  private def writeAndRead[T](t: T)(implicit fmt: Format[T]) = fmt.reads(Json.toJson(fmt.writes(t)))

  "Creating a Json from a valid TransactorDetails model" should {
    "parse successfully" in {
      writeAndRead(validTransactorDetails) resultsIn validTransactorDetails
    }
  }

  "Parsing an invalid json should" should {
    "fail with a JsonValidationError" in {
      Json.fromJson[TransactorDetails](Json.obj(
        "isPartOfOrganisation" -> "notBoolean"
      )).isError mustBe true
    }
  }
}