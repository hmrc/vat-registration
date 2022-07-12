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
import models.api.vatapplication.{AASDetails, Annual, BankGIRO, ConditionalValue, JanDecStagger, JanuaryStagger, Monthly, MonthlyPayment, MonthlyStagger, NIPCompliance, OverseasCompliance, PaymentFrequency, PaymentMethod, Quarterly, ReturnsFrequency, Stagger, StoringGoodsForDispatch, StoringWithinUk, VatApplication}
import play.api.libs.json._

import java.time.LocalDate

class VatApplicationSpec extends BaseSpec with JsonFormatValidation {

  val testTurnover = 10000.5
  val testDate: LocalDate = LocalDate.now()
  val testWarehouseNumber = "test12345678"
  val testWarehouseName = "testWarehouseName"
  val testNorthernIrelandProtocol: NIPCompliance = NIPCompliance(
    ConditionalValue(answer = true, Some(testTurnover)),
    ConditionalValue(answer = false, None)
  )

  val testMonthlyVatApplication: VatApplication = VatApplication(
    Some(true),
    Some(true),
    Some(testTurnover),
    None,
    Some(testTurnover),
    claimVatRefunds = Some(true),
    Some(Monthly),
    Some(MonthlyStagger),
    Some(testDate),
    None,
    None,
    Some(testNorthernIrelandProtocol),
    Some(false)
  )

  val testQuarterlyVatApplication: VatApplication = VatApplication(
    Some(true),
    Some(true),
    Some(testTurnover),
    None,
    Some(testTurnover),
    claimVatRefunds = Some(false),
    Some(Quarterly),
    Some(JanuaryStagger),
    Some(testDate),
    None,
    None,
    Some(testNorthernIrelandProtocol),
    None
  )

  val testAnnualVatApplication: VatApplication = VatApplication(
    Some(true),
    Some(true),
    Some(testTurnover),
    None,
    Some(testTurnover),
    claimVatRefunds = Some(false),
    Some(Annual),
    Some(JanDecStagger),
    Some(testDate),
    Some(AASDetails(BankGIRO, MonthlyPayment)),
    None,
    Some(testNorthernIrelandProtocol),
    None
  )

  val testOverseasVatApplication: VatApplication = testQuarterlyVatApplication.copy(overseasCompliance = Some(OverseasCompliance(
    goodsToOverseas = true,
    goodsToEu = Some(true),
    storingGoodsForDispatch = StoringWithinUk,
    usingWarehouse = Some(true),
    fulfilmentWarehouseNumber = Some(testWarehouseNumber),
    fulfilmentWarehouseName = Some(testWarehouseName)
  )))

  val validMonthlyVatApplicationJson: JsObject = Json.obj(
    "eoriRequested" -> true,
    "tradeVatGoodsOutsideUk" -> true,
    "turnoverEstimate" -> testTurnover,
    "zeroRatedSupplies" -> testTurnover,
    "claimVatRefunds" -> true,
    "returnsFrequency" -> Json.toJson[ReturnsFrequency](Monthly),
    "staggerStart" -> Json.toJson[Stagger](MonthlyStagger),
    "startDate" -> testDate,
    "northernIrelandProtocol" -> Json.obj(
      "goodsToEU" -> Json.obj(
        "answer" -> true,
        "value" -> testTurnover
      ),
      "goodsFromEU" -> Json.obj(
        "answer" -> false
      )
    ),
    "hasTaxRepresentative" -> false
  )

  val validQuarterlyVatApplicationJson: JsObject = Json.obj(
    "eoriRequested" -> true,
    "tradeVatGoodsOutsideUk" -> true,
    "turnoverEstimate" -> testTurnover,
    "zeroRatedSupplies" -> testTurnover,
    "claimVatRefunds" -> false,
    "returnsFrequency" -> Json.toJson[ReturnsFrequency](Quarterly),
    "staggerStart" -> Json.toJson[Stagger](JanuaryStagger),
    "startDate" -> testDate,
    "northernIrelandProtocol" -> Json.obj(
      "goodsToEU" -> Json.obj(
        "answer" -> true,
        "value" -> testTurnover
      ),
      "goodsFromEU" -> Json.obj(
        "answer" -> false
      )
    )
  )

  def validAnnualVatApplicationJson(startDate: Option[LocalDate] = Some(testDate)): JsObject =
    Json.obj(
      "eoriRequested" -> true,
      "tradeVatGoodsOutsideUk" -> true,
      "turnoverEstimate" -> testTurnover,
      "zeroRatedSupplies" -> testTurnover,
      "claimVatRefunds" -> false,
      "returnsFrequency" -> Json.toJson[ReturnsFrequency](Annual),
      "staggerStart" -> Json.toJson[Stagger](JanDecStagger),
      "startDate" -> startDate,
      "annualAccountingDetails" -> Json.obj(
        "paymentMethod" -> Json.toJson[PaymentMethod](BankGIRO),
        "paymentFrequency" -> Json.toJson[PaymentFrequency](MonthlyPayment)
      ),
      "northernIrelandProtocol" -> Json.obj(
        "goodsToEU" -> Json.obj(
          "answer" -> true,
          "value" -> testTurnover
        ),
        "goodsFromEU" -> Json.obj(
          "answer" -> false
        )
      )
    )

  val invalidVatApplicationJson: JsObject = Json.obj(
    "eoriRequested" -> true,
    "tradeVatGoodsOutsideUk" -> true,
    "turnoverEstimate" -> testTurnover,
    "zeroRatedSupplies" -> testTurnover,
    "returnsFrequency" -> "invalidFrequency",
    "staggerStart" -> "invalidStagger",
    "startDate" -> testDate,
    "annualAccountingDetails" -> Json.obj(
      "paymentMethod" -> Json.toJson[PaymentMethod](BankGIRO),
      "paymentFrequency" -> Json.toJson[PaymentFrequency](MonthlyPayment)
    ),
    "northernIrelandProtocol" -> Json.obj(
      "goodsToEU" -> Json.obj(
        "answer" -> true,
        "value" -> testTurnover
      ),
      "goodsFromEU" -> Json.obj(
        "answer" -> false
      )
    )
  )

  val validOverseasJson: JsObject = Json.obj(
    "eoriRequested" -> true,
    "tradeVatGoodsOutsideUk" -> true,
    "turnoverEstimate" -> testTurnover,
    "zeroRatedSupplies" -> testTurnover,
    "claimVatRefunds" -> false,
    "returnsFrequency" -> Json.toJson[ReturnsFrequency](Quarterly),
    "staggerStart" -> Json.toJson[Stagger](JanuaryStagger),
    "startDate" -> testDate,
    "overseasCompliance" -> Json.obj(
      "goodsToOverseas" -> true,
      "goodsToEu" -> true,
      "storingGoodsForDispatch" -> Json.toJson[StoringGoodsForDispatch](StoringWithinUk),
      "usingWarehouse" -> true,
      "fulfilmentWarehouseNumber" -> testWarehouseNumber,
      "fulfilmentWarehouseName" -> testWarehouseName
    ),
    "northernIrelandProtocol" -> Json.obj(
      "goodsToEU" -> Json.obj(
        "answer" -> true,
        "value" -> testTurnover
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
        Json.fromJson[VatApplication](validAnnualVatApplicationJson(None)) mustBe JsSuccess(testAnnualVatApplication.copy(startDate = None))
      }

      "full overseas json is present" in {
        Json.fromJson[VatApplication](validOverseasJson) mustBe JsSuccess(testOverseasVatApplication)
      }
    }

    "fails" when {
      "json is invalid" in {
        val result = Json.fromJson[VatApplication](invalidVatApplicationJson)
        result shouldHaveErrors(
          __ \ "returnsFrequency" -> JsonValidationError("Could not parse payment frequency"),
          __ \ "staggerStart" -> JsonValidationError("Could not parse Stagger")
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