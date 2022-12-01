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
import models.api.Contact
import play.api.libs.json._

class ContactSpec extends BaseSpec with JsonFormatValidation {


  "Creating a Contact model from Json" should {
    "complete successfully" when {
      "from full Json" in {
        val json = Json.parse(
          s"""
             |{
             |  "email":"test@test.com",
             |  "tel":"12345678910"
             |}
        """.stripMargin)
        val tstVatDigitalContact = Contact(Some("test@test.com"), Some("12345678910"))

        Json.fromJson[Contact](json) mustBe JsSuccess(tstVatDigitalContact)
      }

      "from partial Json" in {
        val json = Json.parse(
          s"""
             |{
             |  "tel":"12345678910"
             |}
        """.stripMargin)
        val tstVatDigitalContact = Contact(None, Some("12345678910"))

        Json.fromJson[Contact](json) mustBe JsSuccess(tstVatDigitalContact)
      }

      "Json is empty" in {
        val json = Json.parse(
          s"""
             |{}
        """.stripMargin)

        val result = Json.fromJson[Contact](json)

        Json.fromJson[Contact](json) mustBe JsSuccess(Contact())
      }
    }
  }
}
