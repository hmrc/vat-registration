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

package models

import fixtures.VatRegistrationFixture
import helpers.BaseSpec
import models.api.FlatRateScheme
import play.api.libs.json._

class FlatRateSchemeSpec extends BaseSpec with JsonFormatValidation with VatRegistrationFixture {

  val completeFlatRateSchemeJson: JsObject = Json.obj(
    "joinFrs"                  -> true,
    "overBusinessGoods"        -> true,
    "estimateTotalSales"       -> BigDecimal(1234567891),
    "overBusinessGoodsPercent" -> true,
    "useThisRate"              -> true,
    "frsStart"                 -> testDate,
    "categoryOfBusiness"       -> "testCategory",
    "percent"                  -> 15,
    "limitedCostTrader"        -> false
  )

  val incompleteFlatRateSchemeJson: JsObject = Json.obj(
    "joinFrs" -> false
  )

  "Creating a FlatRateScheme model from Json" should {
    "complete successfully" when {
      "complete json provided" in {
        Json.fromJson[FlatRateScheme](completeFlatRateSchemeJson) mustBe JsSuccess(validFullFlatRateScheme)
      }
      "incomplete json provided" in {
        Json.fromJson[FlatRateScheme](incompleteFlatRateSchemeJson) mustBe JsSuccess(validEmptyFlatRateScheme)
      }
    }
  }

  "Parsing FlatRateScheme to Json" should {
    "succeed" when {
      "FlatRateScheme is full" in {
        Json.toJson[FlatRateScheme](validFullFlatRateScheme) mustBe completeFlatRateSchemeJson
      }
      "FlatRateScheme is incomplete" in {
        Json.toJson[FlatRateScheme](validEmptyFlatRateScheme) mustBe incompleteFlatRateSchemeJson
      }
    }
  }
}
