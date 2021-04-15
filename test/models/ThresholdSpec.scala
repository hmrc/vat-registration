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

import models.api.Threshold
import play.api.libs.json.{JsPath, JsSuccess, Json, JsonValidationError}

import java.time.LocalDate

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
        mandatoryRegistration = true
      )

      Json.fromJson[Threshold](json)(Threshold.format) mustBe JsSuccess(expectedResult)
    }

    "successfully read from full valid json" in {
      val json = Json.parse(
        s"""
           |{
           |  "mandatoryRegistration": false,
           |  "thresholdPreviousThirtyDays": "2017-12-30",
           |  "thresholdInTwelveMonths": "2017-06-15",
           |  "thresholdNextThirtyDays": "2017-01-21"
           |}
         """.stripMargin)

      val expectedResult = Threshold(
        mandatoryRegistration = false,
        thresholdPreviousThirtyDays = Some(LocalDate.of(2017, 12, 30)),
        thresholdInTwelveMonths = Some(LocalDate.of(2017, 6, 15)),
        thresholdNextThirtyDays = Some(LocalDate.of(2017, 1, 21))
      )

      Json.fromJson[Threshold](json)(Threshold.format) mustBe JsSuccess(expectedResult)
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
      result shouldHaveErrors (JsPath() \ "mandatoryRegistration" -> JsonValidationError("error.path.missing"))
    }

    "eligibilityDataJsonReads read successfully from full json" when {
      "the registration is voluntary" in {
        val json = Json.parse(
          s"""{
             |  "voluntaryRegistration": true,
             |  "fooDirectorDetails2": true,
             |  "fooDirectorDetails3": true
             |}
        """.stripMargin)

        val expectedResult = Threshold(
          mandatoryRegistration = false
        )

        val result = Json.fromJson[Threshold](json)(Threshold.eligibilityDataJsonReads)
        result mustBe JsSuccess(expectedResult)
      }

      "the registration is mandatory" in {
        val thresholdPreviousThirtyDays = "2017-01-02"
        val thresholdInTwelveMonths = "2017-01-04"
        val json = Json.parse(
          s"""{
             |  "thresholdPreviousThirtyDays-optionalData": "$thresholdPreviousThirtyDays",
             |  "thresholdInTwelveMonths-optionalData": "$thresholdInTwelveMonths",
             |  "fooDirectorDetails2": true,
             |  "fooDirectorDetails3": true
             |}
        """.stripMargin)

        val expectedResult = Threshold(
          mandatoryRegistration = true,
          thresholdPreviousThirtyDays = Some(LocalDate.parse(thresholdPreviousThirtyDays)),
          thresholdInTwelveMonths = Some(LocalDate.parse(thresholdInTwelveMonths))
        )

        val result = Json.fromJson[Threshold](json)(Threshold.eligibilityDataJsonReads)
        result mustBe JsSuccess(expectedResult)
      }
    }

    "the threshold model has a value for next thirty days" in {
      val thresholdPreviousThirtyDays = "2017-01-02"
      val thresholdInTwelveMonths = "2017-01-04"
      val thresholdNextThirtyDays = "2017-01-10"
      val json = Json.parse(
        s"""{
           |  "thresholdPreviousThirtyDays-optionalData": "$thresholdPreviousThirtyDays",
           |  "thresholdInTwelveMonths-optionalData": "$thresholdInTwelveMonths",
           |  "thresholdNextThirtyDays-optionalData": "$thresholdNextThirtyDays",
           |  "fooDirectorDetails2": true,
           |  "fooDirectorDetails3": true
           |}
        """.stripMargin)

      val expectedResult = Threshold(
        mandatoryRegistration = true,
        thresholdPreviousThirtyDays = Some(LocalDate.parse(thresholdPreviousThirtyDays)),
        thresholdInTwelveMonths = Some(LocalDate.parse(thresholdInTwelveMonths)),
        thresholdNextThirtyDays = Some(LocalDate.parse(thresholdNextThirtyDays))
      )

      val result = Json.fromJson[Threshold](json)(Threshold.eligibilityDataJsonReads)
      result mustBe JsSuccess(expectedResult)
    }

    "eligibilityDataJsonReads fails from incorrect json" in {
      val thresholdInTwelveMonths = "2017-01-04"
      val json = Json.parse(
        s"""
           |{
           |  "thresholdNextThirtyDays": false,
           |  "thresholdPreviousThirtyDays-optionalData": "5345435",
           |  "thresholdInTwelveMonths-optionalData": "$thresholdInTwelveMonths",
           |  "fooDirectorDetails2": true,
           |  "fooDirectorDetails3": true
           |}
        """.stripMargin)

      val result = Json.fromJson[Threshold](json)(Threshold.eligibilityDataJsonReads)
      result.isError mustBe true
    }
  }
}