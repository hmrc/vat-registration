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

package services.submission

import models.api.Address
import models.submission.{EntitiesArrayType, PartnerEntity}
import play.api.libs.json.{JsObject, JsValue, Json}
import services.{BusinessContactService, PartnersService}
import utils.JsonUtils.{conditional, jsonObject, optional}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EntitiesBlockBuilder @Inject()(partnersService: PartnersService,
                                     businessContactService: BusinessContactService)
                                    (implicit ec: ExecutionContext) {

  private val addPartnerAction = "1"

  // scalastyle:off
  def buildEntitiesBlock(regId: String): Future[Option[JsValue]] =
    for{
      optPartners <- partnersService.getPartners(regId)
      businessContact <- businessContactService.getBusinessContact(regId)
      optAddress = businessContact.map(_.ppob)
      optTelephone = businessContact.flatMap(_.digitalContact.tel)
    } yield optPartners match {
      case Some(partners) if partners.nonEmpty =>
        Some(Json.toJson(partners.map { partner =>
          jsonObject(
            "action" -> addPartnerAction,
            "entityType" -> Json.toJson[EntitiesArrayType](PartnerEntity),
            "tradersPartyType" -> partner.partyType,
            optional("customerIdentification" -> {
              partner.details match {
                case _ if partner.details.bpSafeId.isDefined =>
                  Some(Json.obj("primeBPSafeID" -> partner.details.bpSafeId))
                case _ if partner.details.identifiers.nonEmpty =>
                  Some(Json.obj("customerID" -> Json.toJson(partner.details.identifiers)))
                case _ =>
                  None
              }
            }),
            conditional(optAddress.isDefined || optTelephone.isDefined)(
              "businessContactDetails" -> jsonObject(
                optional("address" -> optAddress.map(formatAddress)),
                optional("commDetails" -> optTelephone.map(tel => Json.obj("telephone" -> tel)))
              )
            )
          )
        }))
      case _ =>
        None
    }

  private def formatAddress(address: Address): JsObject = jsonObject(
    "line1" -> address.line1,
    optional("line2" -> address.line2),
    optional("line3" -> address.line3),
    optional("line4" -> address.line4),
    optional("line5" -> address.line5),
    optional("postCode" -> address.postcode),
    optional("countryCode" -> address.country.flatMap(_.code))
  )

}
