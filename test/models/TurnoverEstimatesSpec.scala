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
import models.api.TurnoverEstimates
import play.api.libs.json.{JsSuccess, Json}
import utils.EligibilityDataJsonUtils

class TurnoverEstimatesSpec extends BaseSpec with JsonFormatValidation {

  "TurnoverEstimates mongoReads" must {
    "return model successfully when turnoverEstimate exists" in {
      val json = Json.parse(
        s"""{
           |  "sections": [
           |   {
           |     "title": "Foo bar",
           |     "data": [
           |       {"questionId":"turnoverEstimate","question": "VAT start date", "answer": "£123456" , "answerValue": 123456}
           |     ]
           |   }
           | ]
           | }""".stripMargin)
      val expected = TurnoverEstimates(turnoverEstimate = 123456)

      val result = Json.fromJson[TurnoverEstimates](EligibilityDataJsonUtils.toJsObject(json))(TurnoverEstimates.eligibilityDataJsonReads)
      result mustBe JsSuccess(expected)
    }

    "return empty model successfully" in {
      val json = Json.parse(
        s"""
           |[
           |   {
           |     "title": "Foo bar",
           |     "data": [
           |       {"questionId":"wrongId","question": "VAT start date", "answer": "The date the company is registered with Companies House" , "answerValue": 10000}
           |     ]
           |   },
           |   {
           |     "title": "Director details",
           |     "data": [
           |       {"questionId":"fooDirectorDetails2","question": "Former name", "answer": "Dan Swales", "answerValue": true},
           |       {"questionId":"fooDirectorDetails3","question": "Date of birth", "answer": "1 January 2000", "answerValue": true}
           |     ]
           |   }
           |]
        """.stripMargin)

      val result = Json.fromJson[TurnoverEstimates](EligibilityDataJsonUtils.toJsObject(json))(TurnoverEstimates.eligibilityDataJsonReads)
      result.isError mustBe true
    }
  }
}
