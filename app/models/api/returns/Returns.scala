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

package models.api.returns

import play.api.libs.json._
import utils.JsonUtilities

import java.time.LocalDate

case class Returns(zeroRatedSupplies: Option[BigDecimal],
                   reclaimVatOnMostReturns: Boolean,
                   returnsFrequency: ReturnsFrequency,
                   staggerStart: Stagger,
                   startDate: Option[LocalDate],
                   annualAccountingDetails: Option[AASDetails],
                   overseasCompliance: Option[OverseasCompliance],
                   northernIrelandProtocol: Option[NIPCompliance])

object Returns extends JsonUtilities {
  implicit val format: Format[Returns] = Json.format[Returns]
}

case class AASDetails(paymentMethod: PaymentMethod,
                      paymentFrequency: PaymentFrequency)

object AASDetails {
  implicit val format: Format[AASDetails] = Json.format[AASDetails]
}

case class OverseasCompliance(goodsToOverseas: Boolean,
                              goodsToEu: Option[Boolean],
                              storingGoodsForDispatch: StoringGoodsForDispatch,
                              usingWarehouse: Option[Boolean],
                              fulfilmentWarehouseNumber: Option[String],
                              fulfilmentWarehouseName: Option[String])

object OverseasCompliance {
  implicit val format: Format[OverseasCompliance] = Json.format[OverseasCompliance]
}

sealed trait StoringGoodsForDispatch
case object StoringWithinUk extends StoringGoodsForDispatch
case object StoringOverseas extends StoringGoodsForDispatch

object StoringGoodsForDispatch {
  val statusMap: Map[StoringGoodsForDispatch, String] = Map(
    StoringWithinUk -> "UK",
    StoringOverseas -> "OVERSEAS"
  )
  val inverseMap: Map[String, StoringGoodsForDispatch] = statusMap.map(_.swap)

  def fromString(value: String): StoringGoodsForDispatch = inverseMap(value)
  def toJsString(value: StoringGoodsForDispatch): JsString = JsString(statusMap(value))

  val writes: Writes[StoringGoodsForDispatch] = Writes[StoringGoodsForDispatch] { storingGoods =>
    toJsString(storingGoods)
  }
  val reads: Reads[StoringGoodsForDispatch] = Reads[StoringGoodsForDispatch] { storingGoods =>
    storingGoods.validate[String] map fromString
  }
  implicit val format: Format[StoringGoodsForDispatch] = Format(reads, writes)
}