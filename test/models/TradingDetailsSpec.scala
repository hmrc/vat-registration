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
import models.api.TradingDetails
import play.api.libs.json._

class TradingDetailsSpec extends BaseSpec with JsonFormatValidation {

  val testTradingName = "testTradingName"
  val testShortOrgName = "testShortOrgName"

  val fullJson: JsValue = Json.obj(
    "tradingName" -> s"$testTradingName",
    "shortOrgName" -> s"$testShortOrgName"
  )
  val fullModel: TradingDetails = TradingDetails(Some(testTradingName), Some(testShortOrgName))

  val emptyJson: JsValue = Json.obj()
  val emptyModel: TradingDetails = TradingDetails(None, None)

  "Creating a TradingDetails model from Json" should {
    "complete successfully from full Json" in {
      Json.fromJson[TradingDetails](fullJson) mustBe JsSuccess(fullModel)
    }
    "complete successfully when json is without any details" in {
      Json.fromJson[TradingDetails](emptyJson) mustBe JsSuccess(emptyModel)
    }
  }

  "Parsing a TradingDetails model to Json" should {
    "complete successfully with full details" in {
      Json.toJson[TradingDetails](fullModel) mustBe fullJson
    }
    "complete successfully without a trading name" in {
      Json.toJson[TradingDetails](emptyModel) mustBe emptyJson
    }
  }
}