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
import models.api.{ComplianceLabour, SicAndCompliance, SicCode}
import play.api.libs.json.{JsObject, JsSuccess, Json}

class SicAndComplianceSpec extends BaseSpec {

  val testBusinessDescription = "testBusinessDescription"
  val testDesc = "testDesc"
  val testDetails = "testDetails"
  val testSic1 = "00998"
  val testSic2 = "12345"
  val testSic3 = "00889"

  lazy val businessActivities: List[SicCode] = List(
    SicCode(testSic1, testDesc, testDetails),
    SicCode(testSic2, testDesc, testDetails),
    SicCode(testSic3, testDesc, testDetails)
  )

  lazy val sicAndCompliance: SicAndCompliance = SicAndCompliance(
    testBusinessDescription,
    Some(ComplianceLabour(numOfWorkersSupplied = Some(1000), intermediaryArrangement = Some(true), supplyWorkers = true)),
    SicCode(testSic2, testDesc, testDetails),
    businessActivities,
    Some(false),
    Some(false)
  )

  lazy val sicJson: JsObject = Json.obj(
    "businessDescription" -> testBusinessDescription,
    "labourCompliance" -> Json.obj(
      "numOfWorkersSupplied" -> 1000,
      "intermediaryArrangement" -> true,
      "supplyWorkers" -> true
    ),
    "mainBusinessActivity" -> Json.obj(
      "code" -> testSic2,
      "desc" -> testDesc,
      "indexes" -> testDetails
    ),
    "businessActivities" -> Json.arr(
      Json.obj(
        "code" -> testSic1,
        "desc" -> testDesc,
        "indexes" -> testDetails
      ), Json.obj(
        "code" -> testSic2,
        "desc" -> testDesc,
        "indexes" -> testDetails
      ), Json.obj(
        "code" -> testSic3,
        "desc" -> testDesc,
        "indexes" -> testDetails
      )
    ),
    "hasLandAndProperty" -> false,
    "otherBusinessInvolvement" -> false
  )

  "format" must {
    "read the json back into a model" in {
      SicAndCompliance.format.reads(sicJson) mustBe JsSuccess(sicAndCompliance)
    }

    "write the model into a json" in {
      SicAndCompliance.format.writes(sicAndCompliance) mustBe sicJson
    }
  }
}
