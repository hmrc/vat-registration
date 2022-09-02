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

import fixtures.VatRegistrationFixture
import helpers.BaseSpec
import models.api.{BusinessGoods, FRSDetails, FlatRateScheme}
import play.api.libs.json.{JsPath, JsSuccess, Json, JsonValidationError}

class FlatRateSchemeSpec extends BaseSpec with JsonFormatValidation with VatRegistrationFixture {

  "Creating a FlatRateScheme model from Json" should {
    "complete successfully" when {
      "complete json provided" in {
        Json.fromJson[FlatRateScheme](validFullFlatRateSchemeJson) mustBe JsSuccess(validFullFlatRateScheme)
      }
      "frsDetails is missing" in {
        Json.fromJson[FlatRateScheme](validEmptyFlatRateSchemeJson) mustBe JsSuccess(validEmptyFlatRateScheme)
      }
    }
    "fail" when {
      "joinFrs is missing" in {
        val json = Json.parse("{}")
        val result = Json.fromJson[FlatRateScheme](json)
        result shouldHaveErrors (JsPath() \ "joinFrs" -> JsonValidationError("error.path.missing"))
      }
    }
  }

  "Creating a FRSDetails model from Json" should {
    "complete successfully" when {
      "optional fields not present" in {
        Json.fromJson[FRSDetails](validFRSDetailsJsonWithoutOptionals) mustBe
          JsSuccess(validFullFRSDetails.copy(businessGoods = None, startDate = None, categoryOfBusiness = None))
      }
    }
    "fail" when {
      "percent is missing" in {
        val json = Json.parse(
          s"""
             |{
             |  "startDate":"$testDate",
             |  "categoryOfBusiness":"testCategory"
             |}
         """.stripMargin)
        val result = Json.fromJson[FRSDetails](json)
        result shouldHaveErrors (JsPath() \ "percent" -> JsonValidationError("error.path.missing"))
      }
    }
  }

  "Creating a BusinessGoods model from Json" should {
    "fail" when {
      "estimatedTotalSales is missing" in {
        val json = Json.parse(
          s"""
             |{
             |  "overTurnover": true
             |}
         """.stripMargin)
        val result = Json.fromJson[BusinessGoods](json)
        result shouldHaveErrors (JsPath() \ "estimatedTotalSales" -> JsonValidationError("error.path.missing"))
      }
      "overTurnover is missing" in {
        val json = Json.parse(
          s"""
             |{
             |  "estimatedTotalSales": 1234567891
             |}
         """.stripMargin)
        val result = Json.fromJson[BusinessGoods](json)
        result shouldHaveErrors (JsPath() \ "overTurnover" -> JsonValidationError("error.path.missing"))
      }
    }
  }

  "Parsing FlatRateScheme to Json" should {
    "succeed" when {
      "FlatRateScheme is full" in {
        Json.toJson[FlatRateScheme](validFullFlatRateScheme) mustBe validFullFlatRateSchemeJson
      }
      "frsDetails is missing" in {
        Json.toJson[FlatRateScheme](FlatRateScheme(joinFrs = true, frsDetails = None)) mustBe (validFullFlatRateSchemeJson - "frsDetails")
      }
    }
  }

  "Parsing FRSDetails to Json" should {
    "succeed" when {
      "FRSDetails is missing any optional field" in {
        Json.toJson[FRSDetails](FRSDetails(None, None, Some("testCategory"), 15.00, None)) mustBe
          (validFullFRSDetailsJsonWithOptionals - "businessGoods" - "startDate" - "limitedCostTrader")
      }
    }
  }
}