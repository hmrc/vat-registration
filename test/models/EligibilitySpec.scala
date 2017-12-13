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

import models.api.Eligibility
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}

class EligibilitySpec extends JsonFormatValidation {
  "Eligibility model" should {
    "successfully read from valid json" in {
      val json = Json.parse(
        s"""
           |{
           |  "version": 1,
           |  "result": "test result"
           |}
         """.stripMargin)

      val expectedResult = Eligibility(
        version = 1,
        result = "test result"
      )

      Json.fromJson[Eligibility](json)(Eligibility.format) shouldBe JsSuccess(expectedResult)
    }

    "fail read from json if version is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "result": "test result"
           |}
         """.stripMargin)

      val result = Json.fromJson[Eligibility](json)(Eligibility.format)
      result shouldHaveErrors (JsPath() \ "version" -> ValidationError("error.path.missing"))
    }

    "fail read from json if result is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "version": 1
           |}
         """.stripMargin)

      val result = Json.fromJson[Eligibility](json)(Eligibility.format)
      result shouldHaveErrors (JsPath() \ "result" -> ValidationError("error.path.missing"))
    }
  }
}
