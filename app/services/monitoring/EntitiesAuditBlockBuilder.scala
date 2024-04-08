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

package services.monitoring

import models._
import models.api.{Address, Entity, VatScheme}
import models.submission._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Request
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils.{jsonObject, optional, required}
import utils.{LoggingUtils, StringNormaliser}

import javax.inject.{Inject, Singleton}

@Singleton
class EntitiesAuditBlockBuilder @Inject() extends LoggingUtils {

  private val addPartnerAction = "1"

  // scalastyle:off
  def buildEntitiesAuditBlock(vatScheme: VatScheme)(implicit request: Request[_]): Option[JsValue] = {
    val business         = vatScheme.business.getOrElse {
      errorLog(
        "[EntitiesAuditBlockBuilder][buildEntitiesAuditBlock] - Attempted to build entities block without business"
      )
      throw new InternalServerException("Attempted to build entities block without business")
    }
    val applicantDetails = vatScheme.applicantDetails.getOrElse {
      errorLog(
        "[EntitiesAuditBlockBuilder][buildEntitiesAuditBlock] - Attempted to build entities block without applicant details"
      )
      throw new InternalServerException("Attempted to build entities block without applicant details")
    }
    val regReason        = vatScheme.eligibilitySubmissionData.map(_.registrationReason).getOrElse {
      errorLog(
        "[EntitiesAuditBlockBuilder][buildEntitiesAuditBlock] - Attempted to build entities block without reg reason"
      )
      throw new InternalServerException("Attempted to build entities block without reg reason")
    }

    val entities = vatScheme.entities match {
      case Some(entityList)                            => entityList.filter(entity => entity.details.isDefined)
      case None if regReason.equals(GroupRegistration) =>
        List(
          Entity(
            details = applicantDetails.entity,
            partyType = UkCompany,
            isLeadPartner = Some(true),
            address = None,
            email = None,
            telephoneNumber = None
          )
        )
      case _                                           => Nil
    }

    entities match {
      case entities if entities.nonEmpty =>
        Some(Json.toJson(entities.map { partner =>
          jsonObject(
            "action"                 -> addPartnerAction,
            "entityType"             -> Json.toJson[EntitiesArrayType](
              regReason match {
                case GroupRegistration => GroupRepMemberEntity
                case _                 => PartnerEntity
              }
            ),
            "tradersPartyType"       -> Json.toJson[PartyType](partner.partyType match {
              case NETP      => Individual
              case partyType => partyType
            }),
            "customerIdentification" -> {
              partner.details match {
                case Some(details) if details.bpSafeId.isDefined =>
                  jsonObject("primeBPSafeID" -> details.bpSafeId)
                case Some(details)                               =>
                  jsonObject(
                    "customerID" -> Json.toJson(details.identifiers)
                  ) ++ {
                    details match {
                      case SoleTraderIdEntity(firstName, lastName, dateOfBirth, _, _, _, _, _, _, _, _) =>
                        jsonObject(
                          "name"        -> jsonObject(
                            "firstName" -> firstName,
                            "lastName"  -> lastName
                          ),
                          "dateOfBirth" -> dateOfBirth
                        )
                      case IncorporatedEntity(companyName, _, _, _, _, _, _, _, _, _)                   => orgNameJson(companyName, None)
                      case MinorEntity(companyName, _, _, _, _, _, _, _, _, _, _)                       => orgNameJson(companyName, None)
                      case PartnershipIdEntity(_, _, companyName, _, _, _, _, _, _, _)                  => orgNameJson(companyName, None)
                    }
                  }
              }
            },
            "businessContactDetails" -> {
              partner.isLeadPartner match {
                case Some(true) =>
                  jsonObject(
                    "address"     -> business.ppobAddress.map(formatAddress),
                    "commDetails" -> {
                      regReason match {
                        case GroupRegistration =>
                          jsonObject(
                            optional("telephone" -> applicantDetails.contact.tel),
                            optional("email"     -> applicantDetails.contact.email)
                          )
                        case _                 =>
                          jsonObject(
                            required("telephone" -> business.telephoneNumber),
                            required("email"     -> business.email)
                          )
                      }
                    }
                  )
                case _          =>
                  jsonObject(
                    "address"     -> partner.address.map(formatAddress),
                    "commDetails" -> {
                      jsonObject(
                        optional("telephone" -> partner.telephoneNumber),
                        optional("email"     -> partner.email)
                      )
                    }
                  )
              }
            }
          )
        }))
      case _                             =>
        None
    }
  }

  private def formatAddress(address: Address): JsObject = jsonObject(
    "line1" -> address.line1,
    optional("line2"       -> address.line2),
    optional("line3"       -> address.line3),
    optional("line4"       -> address.line4),
    optional("line5"       -> address.line5),
    optional("postCode"    -> address.postcode),
    optional("countryCode" -> address.country.flatMap(_.code))
  )

  private def orgNameJson(orgName: Option[String], optShortOrgName: Option[String])(implicit
    request: Request[_]
  ): JsObject =
    (orgName.map(StringNormaliser.normaliseString), optShortOrgName.map(StringNormaliser.normaliseString)) match {
      case (Some(orgName), Some(shortOrgName)) =>
        jsonObject(
          "shortOrgName"     -> shortOrgName,
          "organisationName" -> orgName
        )
      case (Some(orgName), None)               =>
        jsonObject(
          "shortOrgName"     -> orgName,
          "organisationName" -> orgName
        )
      case _                                   =>
        errorLog(
          "[EntitiesAuditBlockBuilder][orgNameJson] - missing organisation name for a partyType that requires it"
        )
        throw new InternalServerException(
          "[EntitiesBlockBuilder] missing organisation name for a partyType that requires it"
        )
    }

}
