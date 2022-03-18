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
import models.api.OtherBusinessInvolvement
import play.api.libs.json._

class OtherBusinessInvolvementSpec extends BaseSpec with VatRegistrationFixture with JsonFormatValidation {

  val testFullObiModel: OtherBusinessInvolvement = OtherBusinessInvolvement(testCompanyName, hasVrn = true, Some(testVrn), Some(true), Some(testUtr), stillTrading = true)
  val testNoUtrModel: OtherBusinessInvolvement = OtherBusinessInvolvement(testCompanyName, hasVrn = true, Some(testVrn), Some(false), None, stillTrading = true)
  val testEmptyObiModel: OtherBusinessInvolvement = OtherBusinessInvolvement(testCompanyName, hasVrn = false, None, Some(false), None, stillTrading = false)

  val fullObiJson: JsValue = Json.obj(
    "businessName" -> s"$testCompanyName",
    "hasVrn" -> true,
    "vrn" -> s"$testVrn",
    "hasUtr" -> true,
    "utr" -> s"$testUtr",
    "stillTrading" -> true
  )

  val noUtrJson: JsValue = Json.obj(
    "businessName" -> s"$testCompanyName",
    "hasVrn" -> true,
    "vrn" -> s"$testVrn",
    "hasUtr" -> false,
    "stillTrading" -> true
  )

  val emptyObiJson: JsValue = Json.obj(
    "businessName" -> s"$testCompanyName",
    "hasVrn" -> false,
    "hasUtr" -> false,
    "stillTrading" -> false
  )

  "Creating a OtherBusinessInvolvement model from Json" should {
    "complete successfully from full Json" in {
      Json.fromJson[OtherBusinessInvolvement](fullObiJson) mustBe JsSuccess(testFullObiModel)
    }
    "complete successfully without a company name" in {
      Json.fromJson[OtherBusinessInvolvement](noUtrJson) mustBe JsSuccess(testNoUtrModel)
    }
    "complete successfully when json is without any details" in {
      Json.fromJson[OtherBusinessInvolvement](emptyObiJson) mustBe JsSuccess(testEmptyObiModel)
    }
  }

  "Parsing a OtherBusinessInvolvement model to Json" should {
    "complete successfully with full details" in {
      Json.toJson[OtherBusinessInvolvement](testFullObiModel) mustBe fullObiJson
    }
    "complete successfully without a UTR" in {
      Json.toJson[OtherBusinessInvolvement](testNoUtrModel) mustBe noUtrJson
    }
    "complete successfully without a UTR or VRN" in {
      Json.toJson[OtherBusinessInvolvement](testEmptyObiModel) mustBe emptyObiJson
    }
  }
}
