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

package models.api

import models.submission.{PartyType, ScotPartnership}
import models.{BusinessEntity, PartnershipIdEntity}
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Entity(
  details: Option[BusinessEntity],
  partyType: PartyType,
  isLeadPartner: Option[Boolean],
  optScottishPartnershipName: Option[String] = None,
  address: Option[Address],
  email: Option[String],
  telephoneNumber: Option[String]
)

object Entity {
  private val partyTypeKey                  = "partyType"
  private val detailsKey                    = "details"
  private val leadPartnerKey                = "isLeadPartner"
  private val optScottishPartnershipNameKey = "optScottishPartnershipName"
  private val addressKey                    = "address"
  private val emailKey                      = "email"
  private val telephoneNumberKey            = "telephoneNumber"

  val reads: Reads[Entity] =
    (__ \ partyTypeKey).read[PartyType].flatMap { partyType =>
      (
        (__ \ detailsKey).readNullable[BusinessEntity](BusinessEntity.reads(partyType)) and
          (__ \ leadPartnerKey).readNullable[Boolean] and
          (__ \ optScottishPartnershipNameKey).readNullable[String] and
          (__ \ addressKey).readNullable[Address] and
          (__ \ emailKey).readNullable[String] and
          (__ \ telephoneNumberKey).readNullable[String]
      ) { (optDetails, optIsLeadPartner, optScottishPartnershipName, optAddress, optEmail, optTelephoneNumber) =>
        val updatedDetails = optDetails.map {
          case details: PartnershipIdEntity if partyType.equals(ScotPartnership) =>
            details.copy(companyName = optScottishPartnershipName)
          case notScottishPartnership                                            => notScottishPartnership
        }

        Entity(
          updatedDetails,
          partyType,
          optIsLeadPartner,
          optScottishPartnershipName,
          optAddress,
          optEmail,
          optTelephoneNumber
        )
      }
    }

  val writes: OWrites[Entity] = Json.writes[Entity]

  implicit val format: Format[Entity] = Format(reads, writes)
}
