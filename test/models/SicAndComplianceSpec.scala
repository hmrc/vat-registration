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
import play.api.libs.json.{JsValue, Json}

class SicAndComplianceSpec extends BaseSpec {

  lazy val businessActivities: List[SicCode] = List(
    SicCode("00998", "testDesc", "testDetails"),
    SicCode("12345", "testDesc", "testDetails"),
    SicCode("00889", "testDesc", "testDetails")
  )

  lazy val sicAndCompliance: SicAndCompliance = SicAndCompliance(
    "test business description",
    Some(ComplianceLabour(numOfWorkersSupplied = Some(1000), intermediaryArrangement = Some(true), supplyWorkers = true)),
    SicCode("12345", "testDesc", "testDetails"),
    businessActivities
  )

  val validFullSubmissionJson: JsValue = Json.obj(
    "subscription" -> Json.obj(
      "businessActivities" -> Json.obj(
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> "12345",
          "mainCode2" -> "00998",
          "mainCode3" -> "00889"
        ),
        "description" -> "test business description"
      )
    ),
    "compliance" -> Json.obj(
      "numOfWorkersSupplied" -> 1000,
      "intermediaryArrangement" -> true,
      "supplyWorkers" -> true
    )
  )

  val validSubmissionJson: JsValue = Json.obj(
    "subscription" -> Json.obj(
      "businessActivities" -> Json.obj(
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> "12345"
        ),
        "description" -> "test business description"
      )
    )
  )
}
