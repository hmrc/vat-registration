/*
 * Copyright 2017 HM Revenue & Customs
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

package models.api

import common.{RegistrationId, TransactionId}
import enums.VatRegStatus
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class VatScheme(id: RegistrationId,
                     transactionId: Option[TransactionId] = None,
                     tradingDetails: Option[VatTradingDetails] = None,
                     lodgingOfficer: Option[LodgingOfficer] = None,
                     financials: Option[VatFinancials] = None,
                     vatSicAndCompliance: Option[VatSicAndCompliance] = None,
                     vatContact: Option[VatContact] = None,
                     vatEligibility: Option[VatServiceEligibility] = None,
                     eligibility: Option[Eligibility] = None,
                     threshold: Option[Threshold] = None,
                     acknowledgementReference: Option[String] = None,
                     vatFlatRateScheme: Option[VatFlatRateScheme] = None,
                     status: VatRegStatus.Value)

object VatScheme {

  def reads(implicit r: Reads[VatFinancials]): Reads[VatScheme] = (
    (__ \ "registrationId").read[RegistrationId] and
    (__ \ "transactionId").readNullable[TransactionId] and
    (__ \ "tradingDetails").readNullable[VatTradingDetails] and
    (__ \ "lodgingOfficer").readNullable[LodgingOfficer] and
    (__ \ "financials").readNullable[VatFinancials](r) and
    (__ \ "vatSicAndCompliance").readNullable[VatSicAndCompliance] and
    (__ \ "vatContact").readNullable[VatContact] and
    (__ \ "vatEligibility").readNullable[VatServiceEligibility] and
    (__ \ "eligibility").readNullable[Eligibility] and
    (__ \ "threshold").readNullable[Threshold] and
    (__ \ "acknowledgementReference").readNullable[String] and
    (__ \ "vatFlatRateScheme").readNullable[VatFlatRateScheme] and
    (__ \ "status").read[VatRegStatus.Value]
  )(VatScheme.apply _)

  def writes(implicit w: Writes[VatFinancials]): OWrites[VatScheme] = (
    (__ \ "registrationId").write[RegistrationId] and
    (__ \ "transactionId").writeNullable[TransactionId] and
    (__ \ "tradingDetails").writeNullable[VatTradingDetails] and
    (__ \ "lodgingOfficer").writeNullable[LodgingOfficer] and
    (__ \ "financials").writeNullable[VatFinancials](w) and
    (__ \ "vatSicAndCompliance").writeNullable[VatSicAndCompliance] and
    (__ \ "vatContact").writeNullable[VatContact] and
    (__ \ "vatEligibility").writeNullable[VatServiceEligibility] and
    (__ \ "eligibility").writeNullable[Eligibility] and
    (__ \ "threshold").writeNullable[Threshold] and
    (__ \ "acknowledgementReference").writeNullable[String] and
    (__ \ "vatFlatRateScheme").writeNullable[VatFlatRateScheme] and
    (__ \ "status").write[VatRegStatus.Value]
  )(unlift(VatScheme.unapply))

  implicit def format(implicit f: OFormat[VatFinancials]): OFormat[VatScheme] = OFormat(reads(f), writes(f))
}
