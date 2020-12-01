/*
 * Copyright 2020 HM Revenue & Customs
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

  val fullJson: JsValue = Json.parse(
    """
      |{
      |  "tradingName":"test-name",
      |  "eoriRequested":true
      |}
         """.stripMargin
  )
  val fullModel: TradingDetails = TradingDetails(Some("test-name"), true)

  val noNameJson: JsValue = Json.parse(
    """
      |{
      |   "eoriRequested":true
      |}
    """.stripMargin
  )
  val noNameModel: TradingDetails = TradingDetails(None, true)

  val noEoriJson: JsValue = Json.parse(
    """
      |{
      |   "tradingName":"test-name"
      |}
    """.stripMargin
  )

  val emptyJson: JsValue = Json.parse(
    """
      |{
      |
      |}
    """.stripMargin
  )

  "Creating a TradingDetails model from Json" should {
    "complete successfully from full Json" in {
      Json.fromJson[TradingDetails](fullJson) mustBe JsSuccess(fullModel)
    }
    "complete successfully without a trading name" in {
      Json.fromJson[TradingDetails](noNameJson) mustBe JsSuccess(noNameModel)
    }
    "be unsuccessful" when {
      "json is without eori-requested" in {
        Json.fromJson[TradingDetails](noEoriJson) shouldHaveErrors (JsPath() \ "eoriRequested" -> JsonValidationError("error.path.missing"))
      }
      "json is without any details" in {
        Json.fromJson[TradingDetails](emptyJson) shouldHaveErrors (JsPath() \ "eoriRequested" -> JsonValidationError("error.path.missing"))
      }
    }
  }

  "Parsing a TradingDetails model to Json" should {
    "complete successfully with full details" in {
      Json.toJson[TradingDetails](fullModel) mustBe fullJson
    }
    "complete successfully without a trading name" in {
      Json.toJson[TradingDetails](noNameModel) mustBe noNameJson
    }
  }

}
