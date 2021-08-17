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

package models.api

import models.{BusinessEntity, PartnershipIdEntity, IncorporatedIdEntity, SoleTraderIdEntity}
import models.submission.{Individual, Partnership, PartyType, UkCompany}
import play.api.libs.json.{JsError, JsPath, JsSuccess, JsValue, Json, JsonValidationError, Reads, Writes}

case class Partner(details: BusinessEntity,
                   partyType: PartyType,
                   isLeadPartner: Boolean)

object Partner {
  private val partyTypeKey = "partyType"
  private val detailsKey = "details"
  private val leadPartnerKey = "isLeadPartner"

  implicit val reads: Reads[Partner] = Reads[Partner] { json =>
    val optPartyType = (json \ partyTypeKey).validate[PartyType].asOpt
    val optDetails = optPartyType match {
      case Some(Individual) => (json \ detailsKey).validate[SoleTraderIdEntity].asOpt
      case Some(UkCompany) => (json \ detailsKey).validate[IncorporatedIdEntity].asOpt
      case Some(Partnership) => (json \ detailsKey).validate[PartnershipIdEntity].asOpt
      case _ => None
    }
    val optIsLeadPartner = (json \ leadPartnerKey).validate[Boolean].asOpt

    (optPartyType, optDetails, optIsLeadPartner) match {
      case (Some(partyType), Some(details), Some(isLeadPartner)) =>
        JsSuccess(Partner(details, partyType, isLeadPartner))
      case _ =>
        JsError(
          errors = Map(
            partyTypeKey -> optPartyType.isEmpty,
            detailsKey -> optDetails.isEmpty,
            leadPartnerKey -> optIsLeadPartner.isEmpty
          ).collect {
            case (key, true) => (JsPath \ key, Seq(JsonValidationError(key)))
          }.toSeq
        )
    }
  }

  implicit val writes: Writes[Partner] = Writes[Partner] { partner =>
    val details: JsValue = partner match {
      case Partner(details, Individual, _) =>
        Json.toJson(details.asInstanceOf[SoleTraderIdEntity])(SoleTraderIdEntity.format)
      case Partner(details, UkCompany, _) =>
        Json.toJson(details.asInstanceOf[IncorporatedIdEntity])(IncorporatedIdEntity.format)
      case Partner(details, Partnership, _) =>
        Json.toJson(details.asInstanceOf[PartnershipIdEntity])(PartnershipIdEntity.format)
    }

    Json.obj(
      detailsKey -> details,
      partyTypeKey -> partner.partyType,
      leadPartnerKey -> partner.isLeadPartner
    )
  }

}
