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

package models.api.vatapplication

import utils.JsonUtilities
import play.api.libs.json._
import play.api.libs.functional.syntax._

import java.time.LocalDate

case class VatApplication(eoriRequested: Option[Boolean],
                          tradeVatGoodsOutsideUk: Option[Boolean],
                          turnoverEstimate: Option[BigDecimal],
                          appliedForExemption: Option[Boolean],
                          zeroRatedSupplies: Option[BigDecimal],
                          claimVatRefunds: Option[Boolean],
                          returnsFrequency: Option[ReturnsFrequency],
                          staggerStart: Option[Stagger],
                          startDate: Option[LocalDate],
                          annualAccountingDetails: Option[AASDetails],
                          overseasCompliance: Option[OverseasCompliance],
                          northernIrelandProtocol: Option[NIPCompliance],
                          hasTaxRepresentative: Option[Boolean])

object VatApplication extends JsonUtilities {

  val tempReads: Reads[VatApplication] = (
    (__ \ "tradingDetails" \ "eoriRequested").readNullable[Boolean] and
      (__ \ "tradingDetails" \ "tradeVatGoodsOutsideUk").readNullable[Boolean] and
      (__ \ "returns" \ "turnoverEstimate").readNullable[BigDecimal] and
      (__ \ "returns" \ "appliedForExemption").readNullable[Boolean] and
      (__ \ "returns" \ "zeroRatedSupplies").readNullable[BigDecimal] and
      (__ \ "returns" \ "reclaimVatOnMostReturns").readNullable[Boolean] and
      (__ \ "returns" \ "returnsFrequency").readNullable[ReturnsFrequency] and
      (__ \ "returns" \ "staggerStart").readNullable[Stagger] and
      (__ \ "returns" \ "startDate").readNullable[LocalDate] and
      (__ \ "returns" \ "annualAccountingDetails").readNullable[AASDetails] and
      (__ \ "returns" \ "overseasCompliance").readNullable[OverseasCompliance] and
      (__ \ "returns" \ "northernIrelandProtocol").readNullable[NIPCompliance] and
      (__ \ "returns" \ "hasTaxRepresentative").readNullable[Boolean]
    ) (VatApplication.apply _)

  implicit val format: Format[VatApplication] = Json.format[VatApplication]
}