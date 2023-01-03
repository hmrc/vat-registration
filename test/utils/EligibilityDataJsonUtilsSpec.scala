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

package utils

import models.JsonFormatValidation
import play.api.libs.json.Json

class EligibilityDataJsonUtilsSpec extends JsonFormatValidation {
  "toJsObject" must {
    "return correct JsObject" in {
      val json = Json.parse(
        """
          |[
          |   {
          |     "title": "VAT details",
          |     "data": [
          |       {"questionId":"fooVatDetails1","question": "VAT taxable sales ...", "answer": "Yes", "answerValue": true},
          |       {"questionId":"fooVatDetails2","question": "VAT start date", "answer": "The date the company is registered with Companies House" , "answerValue": "foo string"},
          |       {"questionId":"fooVatDetails3","question": "Other trading name", "answer": "Its a Mighty Fine Company", "answerValue": "2018-06-20"}
          |     ]
          |   },
          |   {
          |     "title": "Director details",
          |     "data": [
          |       {"questionId":"fooDirectorDetails1","question": "Person registering the company for VAT", "answer": "Bob Bimbly Bobblous Bobbings", "answerValue": 1000},
          |       {"questionId":"fooDirectorDetails2","question": "Former name", "answer": "Dan Swales", "answerValue": true},
          |       {"questionId":"fooDirectorDetails3","question": "Date of birth", "answer": "1 January 2000", "answerValue": true}
          |     ]
          |   }
          |]
        """.stripMargin)

      val expectedResult = Json.obj(
        "fooVatDetails1" -> true,
        "fooVatDetails2" -> "foo string",
        "fooVatDetails3" -> "2018-06-20",
        "fooDirectorDetails1" -> 1000,
        "fooDirectorDetails2" -> true,
        "fooDirectorDetails3" -> true)

      EligibilityDataJsonUtils.toJsObject(json) mustBe expectedResult
    }
  }
}
