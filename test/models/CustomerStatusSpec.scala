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

import models.api._
import play.api.libs.json._
import utils.EligibilityDataJsonUtils

class CustomerStatusSpec extends JsonFormatValidation {

  "eligibilityDataJsonReads" must {
    def parser(json: JsValue): JsResult[CustomerStatus] =
      Json.fromJson[CustomerStatus](EligibilityDataJsonUtils.toJsObject(json))(CustomerStatus.eligibilityDataJsonReads)


    "successfully parse valid json into the right type for MTDfB" in {
      val json: JsValue = Json.parse(
        s"""{
           |  "sections": [
           |   {
           |     "title": "testTitle",
           |     "data": [
           |       {"questionId": "voluntaryInformation","question": "testQuestion", "answer": "testAnswer", "answerValue": true}
           |     ]
           |   }
           | ]
           | }""".stripMargin
      )

      parser(json) mustBe JsSuccess(MTDfB)
    }

    "successfully parse valid json into the right type for MTDfB with no data" in {
      val json: JsValue = Json.parse(
        s"""{
           |  "sections": [
           |   {
           |     "title": "testTitle",
           |     "data": [
           |     ]
           |   }
           | ]
           | }""".stripMargin
      )

      parser(json) mustBe JsSuccess(MTDfB)
    }

    "successfully parse valid json into the right type for NonMTDfB" in {
      val json: JsValue = Json.parse(
        s"""{
           |  "sections": [
           |   {
           |     "title": "testTitle",
           |     "data": [
           |       {"questionId": "voluntaryInformation","question": "testQuestion", "answer": "testAnswer", "answerValue": false}
           |     ]
           |   }
           | ]
           | }""".stripMargin
      )

      parser(json) mustBe JsSuccess(NonMTDfB)
    }
  }

  "reads" must {
    "successfully parse valid json into the right type" in {
      CustomerStatus.format.reads(JsString(MTDfBExempt.value)) mustBe JsSuccess(MTDfBExempt)
      CustomerStatus.format.reads(JsString(MTDfB.value)) mustBe JsSuccess(MTDfB)
      CustomerStatus.format.reads(JsString(NonMTDfB.value)) mustBe JsSuccess(NonMTDfB)
      CustomerStatus.format.reads(JsString(NonDigital.value)) mustBe JsSuccess(NonDigital)
    }
  }

  "writes" must {
    "return the correct status value" in {
      CustomerStatus.format.writes(MTDfBExempt) mustBe JsString(MTDfBExempt.value)
      CustomerStatus.format.writes(MTDfB) mustBe JsString(MTDfB.value)
      CustomerStatus.format.writes(NonMTDfB) mustBe JsString(NonMTDfB.value)
      CustomerStatus.format.writes(NonDigital) mustBe JsString(NonDigital.value)
    }
  }
}
