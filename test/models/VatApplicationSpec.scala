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

import helpers.BaseSpec
import models.api.vatapplication._
import play.api.libs.json._

import java.time.LocalDate

class VatApplicationSpec extends BaseSpec with JsonFormatValidation {

  lazy val testStandardRateSupplies: Int        = 1000
  lazy val testReducedRateSupplies: Int         = 2000
  lazy val testZeroRateSupplies: Int            = 500
  lazy val testTurnover: Double                 = 10000.5
  lazy val testAcceptTurnoverEstimate: Boolean  = true
  val testDate: LocalDate                       = LocalDate.now()
  val testWarehouseNumber                       = "test12345678"
  val testWarehouseName                         = "testWarehouseName"
  val testNorthernIrelandProtocol: NIPCompliance = NIPCompliance(
    Some(ConditionalValue(answer = true, Some(testTurnover))),
    Some(ConditionalValue(answer = false, None))
  )

  val testMonthlyVatApplication: VatApplication = VatApplication(
    Some(true),
    Some(true),
    standardRateSupplies = Some(testTurnover),
    reducedRateSupplies = None,
    zeroRatedSupplies = None,
    turnoverEstimate = Some(testTurnover),
    acceptTurnOverEstimate = Some(true),
    None,
    claimVatRefunds = Some(true),
    Some(Monthly),
    Some(MonthlyStagger),
    Some(testDate),
    None,
    None,
    Some(testNorthernIrelandProtocol),
    Some(false),
    Some(false)
  )

  val testQuarterlyVatApplication: VatApplication = VatApplication(
    Some(true),
    Some(true),
    standardRateSupplies = Some(testTurnover),
    reducedRateSupplies = None,
    zeroRatedSupplies = None,
    turnoverEstimate = Some(testTurnover),
    acceptTurnOverEstimate = Some(true),
    None,
    claimVatRefunds = Some(false),
    Some(Quarterly),
    Some(JanuaryStagger),
    Some(testDate),
    None,
    None,
    Some(testNorthernIrelandProtocol),
    None,
    None
  )

  val testAnnualVatApplication: VatApplication = VatApplication(
    Some(true),
    Some(true),
    standardRateSupplies = Some(testTurnover),
    reducedRateSupplies = None,
    zeroRatedSupplies = None,
    turnoverEstimate = Some(testTurnover),
    acceptTurnOverEstimate = Some(true),
    None,
    claimVatRefunds = Some(false),
    Some(Annual),
    Some(JanDecStagger),
    Some(testDate),
    Some(AASDetails(Some(BankGIRO), Some(MonthlyPayment))),
    None,
    Some(testNorthernIrelandProtocol),
    None,
    None
  )

  val testOverseasVatApplication: VatApplication = testQuarterlyVatApplication.copy(overseasCompliance =
    Some(
      OverseasCompliance(
        goodsToOverseas = Some(true),
        goodsToEu = Some(true),
        storingGoodsForDispatch = Some(StoringWithinUk),
        usingWarehouse = Some(true),
        fulfilmentWarehouseNumber = Some(testWarehouseNumber),
        fulfilmentWarehouseName = Some(testWarehouseName)
      )
    )
  )

  val validMonthlyVatApplicationJson: JsObject = Json.obj(
    "eoriRequested"           -> true,
    "tradeVatGoodsOutsideUk"  -> true,
    "standardRateSupplies"    -> Some(testTurnover),
    "turnoverEstimate"        -> Some(testTurnover),
    "acceptTurnOverEstimate"  -> true,
    "claimVatRefunds"         -> true,
    "returnsFrequency"        -> Json.toJson[ReturnsFrequency](Monthly),
    "staggerStart"            -> Json.toJson[Stagger](MonthlyStagger),
    "startDate"               -> testDate,
    "northernIrelandProtocol" -> Json.obj(
      "goodsToEU"   -> Json.obj(
        "answer" -> true,
        "value"  -> testTurnover
      ),
      "goodsFromEU" -> Json.obj(
        "answer" -> false
      )
    ),
    "hasTaxRepresentative"    -> false,
    "currentlyTrading"        -> false
  )

  val validQuarterlyVatApplicationJson: JsObject = Json.obj(
    "eoriRequested"           -> true,
    "tradeVatGoodsOutsideUk"  -> true,
    "standardRateSupplies"    -> Some(testTurnover),
    "turnoverEstimate"        -> Some(testTurnover),
    "acceptTurnOverEstimate"  -> true,
    "claimVatRefunds"         -> false,
    "returnsFrequency"        -> Json.toJson[ReturnsFrequency](Quarterly),
    "staggerStart"            -> Json.toJson[Stagger](JanuaryStagger),
    "startDate"               -> testDate,
    "northernIrelandProtocol" -> Json.obj(
      "goodsToEU"   -> Json.obj(
        "answer" -> true,
        "value"  -> testTurnover
      ),
      "goodsFromEU" -> Json.obj(
        "answer" -> false
      )
    )
  )

  def validAnnualVatApplicationJson(startDate: Option[LocalDate] = Some(testDate)): JsObject =
    Json.obj(
      "eoriRequested"           -> true,
      "tradeVatGoodsOutsideUk"  -> true,
      "standardRateSupplies"    -> Some(testTurnover),
      "turnoverEstimate"        -> Some(testTurnover),
      "acceptTurnOverEstimate"  -> true,
      "claimVatRefunds"         -> false,
      "returnsFrequency"        -> Json.toJson[ReturnsFrequency](Annual),
      "staggerStart"            -> Json.toJson[Stagger](JanDecStagger),
      "startDate"               -> startDate,
      "annualAccountingDetails" -> Json.obj(
        "paymentMethod"    -> Json.toJson[PaymentMethod](BankGIRO),
        "paymentFrequency" -> Json.toJson[PaymentFrequency](MonthlyPayment)
      ),
      "northernIrelandProtocol" -> Json.obj(
        "goodsToEU"   -> Json.obj(
          "answer" -> true,
          "value"  -> testTurnover
        ),
        "goodsFromEU" -> Json.obj(
          "answer" -> false
        )
      )
    )

  val invalidVatApplicationJson: JsObject = Json.obj(
    "eoriRequested"           -> true,
    "tradeVatGoodsOutsideUk"  -> true,
    "standardRateSupplies"    -> testTurnover,
    "turnoverEstimate"        -> testTurnover,
    "acceptTurnOverEstimate"  -> true,
    "returnsFrequency"        -> "invalidFrequency",
    "staggerStart"            -> "invalidStagger",
    "startDate"               -> testDate,
    "annualAccountingDetails" -> Json.obj(
      "paymentMethod"    -> Json.toJson[PaymentMethod](BankGIRO),
      "paymentFrequency" -> Json.toJson[PaymentFrequency](MonthlyPayment)
    ),
    "northernIrelandProtocol" -> Json.obj(
      "goodsToEU"   -> Json.obj(
        "answer" -> true,
        "value"  -> testTurnover
      ),
      "goodsFromEU" -> Json.obj(
        "answer" -> false
      )
    )
  )

  val validOverseasJson: JsObject = Json.obj(
    "eoriRequested"           -> true,
    "tradeVatGoodsOutsideUk"  -> true,
    "standardRateSupplies"    -> testTurnover,
    "turnoverEstimate"        -> testTurnover,
    "acceptTurnOverEstimate"  -> true,
    "claimVatRefunds"         -> false,
    "returnsFrequency"        -> Json.toJson[ReturnsFrequency](Quarterly),
    "staggerStart"            -> Json.toJson[Stagger](JanuaryStagger),
    "startDate"               -> testDate,
    "overseasCompliance"      -> Json.obj(
      "goodsToOverseas"           -> true,
      "goodsToEu"                 -> true,
      "storingGoodsForDispatch"   -> Json.toJson[StoringGoodsForDispatch](StoringWithinUk),
      "usingWarehouse"            -> true,
      "fulfilmentWarehouseNumber" -> testWarehouseNumber,
      "fulfilmentWarehouseName"   -> testWarehouseName
    ),
    "northernIrelandProtocol" -> Json.obj(
      "goodsToEU"   -> Json.obj(
        "answer" -> true,
        "value"  -> testTurnover
      ),
      "goodsFromEU" -> Json.obj(
        "answer" -> false
      )
    )
  )

  "Parsing VatApplication" should {
    "succeed" when {
      "full monthly json is present" in {
        Json.fromJson[VatApplication](validMonthlyVatApplicationJson) mustBe JsSuccess(testMonthlyVatApplication)
      }

      "full quarterly json is present" in {
        Json.fromJson[VatApplication](validQuarterlyVatApplicationJson) mustBe JsSuccess(testQuarterlyVatApplication)
      }

      "full annual json is present" in {
        Json.fromJson[VatApplication](validAnnualVatApplicationJson()) mustBe JsSuccess(testAnnualVatApplication)
      }

      "full annual json is present without startDate" in {
        Json.fromJson[VatApplication](validAnnualVatApplicationJson(None)) mustBe JsSuccess(
          testAnnualVatApplication.copy(startDate = None)
        )
      }

      "full overseas json is present" in {
        Json.fromJson[VatApplication](validOverseasJson) mustBe JsSuccess(testOverseasVatApplication)
      }
    }

    "fails" when {
      "json is invalid" in {
        val result = Json.fromJson[VatApplication](invalidVatApplicationJson)
        result shouldHaveErrors (
          __ \ "returnsFrequency" -> JsonValidationError("Could not parse payment frequency"),
          __ \ "staggerStart"     -> JsonValidationError("Could not parse Stagger")
        )
      }
    }
  }

  "VatApplication model to json" should {
    "succeed" when {
      "everything is present" in {
        Json.toJson[VatApplication](testAnnualVatApplication) mustBe validAnnualVatApplicationJson()
      }
    }
  }
}
