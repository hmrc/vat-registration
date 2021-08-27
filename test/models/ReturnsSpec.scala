/*
 * Copyright 2021 HM Revenue & Customs
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
import models.api.returns._
import play.api.libs.json._

import java.time.LocalDate

class ReturnsSpec extends BaseSpec with JsonFormatValidation {

  val testZeroRatedSupplies = 10000.5
  val testDate: LocalDate = LocalDate.now()
  val testWarehouseNumber = "test12345678"

  val testMonthlyReturns: Returns = Returns(
    Some(testZeroRatedSupplies),
    reclaimVatOnMostReturns = true,
    Monthly,
    MonthlyStagger,
    Some(testDate),
    None,
    None
  )

  val testQuarterlyReturns: Returns = Returns(
    Some(testZeroRatedSupplies),
    reclaimVatOnMostReturns = false,
    Quarterly,
    JanuaryStagger,
    Some(testDate),
    None,
    None
  )

  val testAnnualReturns: Returns = Returns(
    Some(testZeroRatedSupplies),
    reclaimVatOnMostReturns = false,
    Annual,
    JanDecStagger,
    Some(testDate),
    Some(AASDetails(BankGIRO, MonthlyPayment)),
    None
  )

  val testOverseasReturns: Returns = testQuarterlyReturns.copy(overseasCompliance = Some(OverseasCompliance(
    goodsToOverseas = true,
    goodsToEu = Some(true),
    storingGoodsForDispatch = StoringWithinUk,
    usingWarehouse = Some(true),
    fulfilmentWarehouseNumber = Some(testWarehouseNumber)
  )))

  val validMonthlyReturnsJson: JsObject = Json.obj(
    "zeroRatedSupplies" -> testZeroRatedSupplies,
    "reclaimVatOnMostReturns" -> true,
    "returnsFrequency" -> Json.toJson[ReturnsFrequency](Monthly),
    "staggerStart" -> Json.toJson[Stagger](MonthlyStagger),
    "startDate" -> testDate
  )

  val validQuarterlyReturnsJson: JsObject = Json.obj(
    "zeroRatedSupplies" -> testZeroRatedSupplies,
    "reclaimVatOnMostReturns" -> false,
    "returnsFrequency" -> Json.toJson[ReturnsFrequency](Quarterly),
    "staggerStart" -> Json.toJson[Stagger](JanuaryStagger),
    "startDate" -> testDate
  )

  def validAnnualReturnsJson(startDate: Option[LocalDate] = Some(testDate)): JsObject =
    Json.obj(
      "zeroRatedSupplies" -> testZeroRatedSupplies,
      "reclaimVatOnMostReturns" -> false,
      "returnsFrequency" -> Json.toJson[ReturnsFrequency](Annual),
      "staggerStart" -> Json.toJson[Stagger](JanDecStagger),
      "startDate" -> startDate,
      "annualAccountingDetails" -> Json.obj(
        "paymentMethod" -> Json.toJson[PaymentMethod](BankGIRO),
        "paymentFrequency" -> Json.toJson[PaymentFrequency](MonthlyPayment)
      )
    )

  val invalidReturnsJson: JsObject = Json.obj(
    "zeroRatedSupplies" -> testZeroRatedSupplies,
    "returnsFrequency" -> "invalidFrequency",
    "staggerStart" -> "invalidStagger",
    "startDate" -> testDate,
    "annualAccountingDetails" -> Json.obj(
      "paymentMethod" -> Json.toJson[PaymentMethod](BankGIRO),
      "paymentFrequency" -> Json.toJson[PaymentFrequency](MonthlyPayment)
    )
  )

  val validOverseasJson: JsObject = Json.obj(
    "zeroRatedSupplies" -> testZeroRatedSupplies,
    "reclaimVatOnMostReturns" -> false,
    "returnsFrequency" -> Json.toJson[ReturnsFrequency](Quarterly),
    "staggerStart" -> Json.toJson[Stagger](JanuaryStagger),
    "startDate" -> testDate,
    "overseasCompliance" -> Json.obj(
      "goodsToOverseas" -> true,
      "goodsToEu" -> true,
      "storingGoodsForDispatch" -> Json.toJson[StoringGoodsForDispatch](StoringWithinUk),
      "usingWarehouse" -> true,
      "fulfilmentWarehouseNumber" -> testWarehouseNumber
    ))

  "Parsing Returns" should {
    "succeed" when {
      "full monthly json is present" in {
        Json.fromJson[Returns](validMonthlyReturnsJson) mustBe JsSuccess(testMonthlyReturns)
      }

      "full quarterly json is present" in {
        Json.fromJson[Returns](validQuarterlyReturnsJson) mustBe JsSuccess(testQuarterlyReturns)
      }

      "full annual json is present" in {
        Json.fromJson[Returns](validAnnualReturnsJson()) mustBe JsSuccess(testAnnualReturns)
      }

      "full annual json is present without startDate" in {
        Json.fromJson[Returns](validAnnualReturnsJson(None)) mustBe JsSuccess(testAnnualReturns.copy(startDate = None))
      }

      "full overseas json is present" in {
        Json.fromJson[Returns](validOverseasJson) mustBe JsSuccess(testOverseasReturns)
      }
    }

    "fails" when {
      "json is invalid" in {
        val result = Json.fromJson[Returns](invalidReturnsJson)
        result shouldHaveErrors(
          __ \ "reclaimVatOnMostReturns" -> JsonValidationError("error.path.missing"),
          __ \ "returnsFrequency" -> JsonValidationError("Could not parse payment frequency"),
          __ \ "staggerStart" -> JsonValidationError("Could not parse Stagger")
        )
      }
    }
  }

  "Returns model to json" should {
    "succeed" when {
      "everything is present" in {
        Json.toJson[Returns](testAnnualReturns) mustBe validAnnualReturnsJson()
      }
    }
  }
}