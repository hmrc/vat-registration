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

package models.api.vatapplication

import play.api.libs.json._
import utils.JsonUtilities

import java.time.LocalDate

case class VatApplication(
  eoriRequested: Option[Boolean],
  tradeVatGoodsOutsideUk: Option[Boolean],
  standardRateSupplies: Option[BigDecimal] = None,
  reducedRateSupplies: Option[BigDecimal] = None,
  zeroRatedSupplies: Option[BigDecimal] = None,
  turnoverEstimate: Option[BigDecimal] = None,
  acceptTurnOverEstimate: Option[Boolean] = None,
  appliedForExemption: Option[Boolean],
  claimVatRefunds: Option[Boolean],
  returnsFrequency: Option[ReturnsFrequency],
  staggerStart: Option[Stagger],
  startDate: Option[LocalDate],
  annualAccountingDetails: Option[AASDetails],
  overseasCompliance: Option[OverseasCompliance],
  northernIrelandProtocol: Option[NIPCompliance],
  hasTaxRepresentative: Option[Boolean],
  currentlyTrading: Option[Boolean]
)

object VatApplication extends JsonUtilities {
  implicit val format: Format[VatApplication] = Json.format[VatApplication]
}
