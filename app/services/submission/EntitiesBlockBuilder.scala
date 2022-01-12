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

package services.submission

import featureswitch.core.config.{FeatureSwitching, ShortOrgName}
import models.api.Address
import models.submission.{EntitiesArrayType, Individual, NETP, PartnerEntity, PartyType}
import models.{IncorporatedEntity, MinorEntity, PartnershipIdEntity, SoleTraderIdEntity}
import play.api.libs.json.{JsObject, JsValue, Json}
import services.{BusinessContactService, PartnersService}
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils.{jsonObject, optional}
import utils.StringNormaliser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EntitiesBlockBuilder @Inject()(partnersService: PartnersService,
                                     businessContactService: BusinessContactService)
                                    (implicit ec: ExecutionContext)
  extends FeatureSwitching {

  private val addPartnerAction = "1"

  // scalastyle:off
  def buildEntitiesBlock(regId: String): Future[Option[JsValue]] =
    for {
      optPartners <- partnersService.getPartners(regId)
      businessContact <- businessContactService.getBusinessContact(regId).map(
        _.getOrElse(throw new InternalServerException("[EntitiesBlockBuilder] Attempted to build entities block without business contact"))
      )
    } yield optPartners match {
      case Some(section) if section.partners.nonEmpty =>
        Some(Json.toJson(section.partners.map { partner =>
          jsonObject(
            "action" -> addPartnerAction,
            "entityType" -> Json.toJson[EntitiesArrayType](PartnerEntity),
            "tradersPartyType" -> Json.toJson[PartyType](partner.partyType match {
              case NETP => Individual
              case partyType => partyType
            }),
            "customerIdentification" -> {
              partner.details match {
                case _ if partner.details.bpSafeId.isDefined =>
                  jsonObject("primeBPSafeID" -> partner.details.bpSafeId)
                case _ =>
                  jsonObject(
                    "customerID" -> Json.toJson(partner.details.identifiers)
                  ) ++ {
                    partner.details match {
                      case SoleTraderIdEntity(firstName, lastName, dateOfBirth, _, _, _, _, _, _, _, _) =>
                        jsonObject(
                          "name" -> jsonObject(
                            "firstName" -> firstName,
                            "lastName" -> lastName
                          ),
                          "dateOfBirth" -> dateOfBirth
                        )
                      case IncorporatedEntity(companyName, _, _, _, _, _, _, _, _, _) => orgNameJson(companyName, None)
                      case MinorEntity(companyName, _, _, _, _, _, _, _, _, _, _) => orgNameJson(companyName, None)
                      case PartnershipIdEntity(_, _, companyName, _, _, _, _, _, _, _) => orgNameJson(companyName, None)
                    }
                  }
              }
            },
            "businessContactDetails" -> jsonObject(
              "address" -> formatAddress(businessContact.ppob),
              "commDetails" -> jsonObject(
                optional("telephone" -> businessContact.digitalContact.tel),
                optional("mobileNumber" -> businessContact.digitalContact.mobile),
                "email" -> businessContact.digitalContact.email
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

  private def orgNameJson(orgName: Option[String], optShortOrgName: Option[String]): JsObject =
    (orgName.map(StringNormaliser.normaliseString), optShortOrgName.map(StringNormaliser.normaliseString)) match {
      case (Some(orgName), Some(shortOrgName)) if isEnabled(ShortOrgName) => jsonObject(
        "shortOrgName" -> shortOrgName,
        "organisationName" -> orgName
      )
      case (Some(orgName), None) if isEnabled(ShortOrgName) => jsonObject(
        "shortOrgName" -> orgName,
        "organisationName" -> orgName
      )
      case (Some(orgName), _) => jsonObject("shortOrgName" -> orgName)
      case _ => throw new InternalServerException("[EntitiesBlockBuilder] missing organisation name for a partyType that requires it")
    }

}
