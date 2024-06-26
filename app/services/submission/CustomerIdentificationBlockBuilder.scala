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

package services.submission

import models._
import models.api.{EligibilitySubmissionData, VatScheme}
import models.submission.{Individual, NETP, TaxGroups}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._
import utils.{LoggingUtils, StringNormaliser}

import javax.inject.{Inject, Singleton}

// scalastyle:off
@Singleton
class CustomerIdentificationBlockBuilder @Inject() () extends LoggingUtils {

  def buildCustomerIdentificationBlock(vatScheme: VatScheme)(implicit request: Request[_]): JsObject =
    (vatScheme.eligibilitySubmissionData, vatScheme.applicantDetails, vatScheme.business) match {
      case (Some(eligibilityData), Some(applicantDetails), Some(business)) =>
        val entity = applicantDetails.entity.getOrElse {
          errorLog("[CustomerIdentificationBlockBuilder][buildCustomerIdentificationBlock] - missing applicant entity")
          throw new InternalServerException("[CustomerIdentificationBlockBuilder] missing applicant entity")
        }

        jsonObject(
          "tradersPartyType" -> {
            eligibilityData match {
              case EligibilitySubmissionData(_, _, _, GroupRegistration, _, _, _, _) => TaxGroups
              case EligibilitySubmissionData(_, _, NETP, _, _, _, _, _)              => Individual
              case EligibilitySubmissionData(_, _, partyType, _, _, _, _, _)         => partyType
            }
          },
          optional("tradingName" -> business.tradingName.map(StringNormaliser.normaliseString))
        ) ++ {
          entity.bpSafeId match {
            case _ if eligibilityData.registrationReason.equals(GroupRegistration) =>
              jsonObject()
            case Some(bpSafeId)                                                    =>
              jsonObject("primeBPSafeID" -> bpSafeId)
            case None if entity.identifiers.nonEmpty                               =>
              jsonObject("customerID" -> Json.toJson(entity.identifiers))
            case _                                                                 =>
              jsonObject()
          }
        } ++ {
          val shortOrgName = business.shortOrgName.map(StringNormaliser.normaliseString)
          entity match {
            case SoleTraderIdEntity(firstName, lastName, dateOfBirth, _, _, _, bpSafeId, _, _, _, _)
                if bpSafeId.isEmpty =>
              jsonObject(
                "name"        -> jsonObject(
                  "firstName" -> firstName,
                  "lastName"  -> lastName
                ),
                "dateOfBirth" -> dateOfBirth
              )
            case IncorporatedEntity(companyName, _, _, _, bpSafeId, _, _, _, _, _) if bpSafeId.isEmpty  =>
              orgNameJson(companyName, shortOrgName)
            case MinorEntity(companyName, _, _, _, _, _, _, _, _, bpSafeId, _) if bpSafeId.isEmpty      =>
              orgNameJson(companyName, shortOrgName)
            case PartnershipIdEntity(_, _, companyName, _, _, _, bpSafeId, _, _, _) if bpSafeId.isEmpty =>
              orgNameJson(companyName, shortOrgName)
            case _                                                                                      => jsonObject() // Don't send company name when safeId is present
          }
        }
      case (None, _, _)                                                    =>
        errorLog(
          "[CustomerIdentificationBlockBuilder][buildCustomerIdentificationBlock] - Could not retrieve VAT scheme"
        )
        throw new InternalServerException("Could not retrieve VAT scheme")
      case (_, None, _)                                                    =>
        errorLog(
          "[CustomerIdentificationBlockBuilder][buildCustomerIdentificationBlock] - Could not build customer identification block for submission due to missing applicant details data"
        )
        throw new InternalServerException(
          "Could not build customer identification block for submission due to missing applicant details data"
        )
      case (_, _, None)                                                    =>
        errorLog(
          "[CustomerIdentificationBlockBuilder][buildCustomerIdentificationBlock] - Could not build customer identification block for submission due to missing business details data"
        )
        throw new InternalServerException(
          "Could not build customer identification block for submission due to missing business details data"
        )
      case _                                                               =>
        errorLog(
          "[CustomerIdentificationBlockBuilder][buildCustomerIdentificationBlock] - Could not build customer identification block for submission due to missing data from applicant and trading details"
        )
        throw new InternalServerException(
          "Could not build customer identification block for submission due to missing data from applicant and trading details"
        )
    }

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
          "[CustomerIdentificationBlockBuilder][buildCustomerIdentificationBlock] - missing organisation name for a partyType that requires it"
        )
        throw new InternalServerException(
          "[EntitiesBlockBuilder] missing organisation name for a partyType that requires it"
        )
    }
}
