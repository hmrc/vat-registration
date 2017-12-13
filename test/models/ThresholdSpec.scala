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

import models.api.Threshold
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}

class ThresholdSpec extends JsonFormatValidation {
  "Threshold model" should {
    "successfully read from valid json" in {
      val json = Json.parse(
        s"""
           |{
           |  "mandatoryRegistration": true
           |}
         """.stripMargin)

      val expectedResult = Threshold(
        mandatoryRegistration = true,
        voluntaryReason = None,
        overThresholdDate = None,
        expectedOverThresholdDate = None
      )

      Json.fromJson[Threshold](json)(Threshold.format) shouldBe JsSuccess(expectedResult)
    }

    "successfully read from full valid json" in {
      val json = Json.parse(
        s"""
           |{
           |  "mandatoryRegistration": false,
           |  "voluntaryReason": "test reason",
           |  "overThresholdDate": "2017-12-30",
           |  "expectedOverThresholdDate": "2017-01-21"
           |}
         """.stripMargin)

      val expectedResult = Threshold(
        mandatoryRegistration = false,
        voluntaryReason = Some("test reason"),
        overThresholdDate = Some(LocalDate.of(2017, 12, 30)),
        expectedOverThresholdDate = Some(LocalDate.of(2017, 1, 21))
      )

      Json.fromJson[Threshold](json)(Threshold.format) shouldBe JsSuccess(expectedResult)
    }

    "fail read from json if mandatoryRegistration is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "voluntaryReason": "test reason",
           |  "overThresholdDate": "2017-12-30",
           |  "expectedOverThresholdDate": "2017-01-21"
           |}
         """.stripMargin)

      val result = Json.fromJson[Threshold](json)(Threshold.format)
      result shouldHaveErrors (JsPath() \ "mandatoryRegistration" -> ValidationError("error.path.missing"))
    }
  }
}
