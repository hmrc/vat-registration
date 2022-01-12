/*
 * Copyright 2022 HM Revenue & Customs
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

import helpers.BaseSpec
import models.api.DigitalContactOptional
import play.api.libs.json._

class DigitalContactOptionalSpec extends BaseSpec with JsonFormatValidation {


  "Creating a DigitalContactOptional model from Json" should {
    "complete successfully" when {
      "from full Json" in {
        val json = Json.parse(
          s"""
             |{
             |  "email":"test@test.com",
             |  "tel":"12345678910",
             |  "mobile":"12345678910"
             |}
        """.stripMargin)
        val tstVatDigitalContact = DigitalContactOptional(Some("test@test.com"), Some("12345678910"), Some("12345678910"))

        Json.fromJson[DigitalContactOptional](json) mustBe JsSuccess(tstVatDigitalContact)
      }

      "from partial Json" in {
        val json = Json.parse(
          s"""
             |{
             |  "tel":"12345678910"
             |}
        """.stripMargin)
        val tstVatDigitalContact = DigitalContactOptional(None, Some("12345678910"), None)

        Json.fromJson[DigitalContactOptional](json) mustBe JsSuccess(tstVatDigitalContact)
      }
    }

    "fail to read" when {
      "Json is empty" in {
        val json = Json.parse(
          s"""
             |{}
        """.stripMargin)

        val result = Json.fromJson[DigitalContactOptional](json)
        result shouldHaveErrors (JsPath() -> JsonValidationError("error.path.missing.atLeast.oneValue"))
      }
    }
  }
}
