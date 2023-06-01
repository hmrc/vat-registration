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

import models.api.VatScheme
import models._
import play.api.libs.json.JsValue
import play.api.mvc.Request
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._
import utils.LoggingUtils

import javax.inject.Singleton

// scalastyle:off
@Singleton
class CustomerIdentificationAuditBlockBuilder extends LoggingUtils{

  def buildCustomerIdentificationBlock(vatScheme: VatScheme)(implicit request: Request[_]): JsValue = {
    (vatScheme.applicantDetails, vatScheme.business) match {
      case (Some(applicantDetails), Some(business)) =>
        val entity = applicantDetails.entity.getOrElse{
          errorLog("[CustomerIdentificationAuditBlockBuilder][buildCustomerIdentificationBlock] - missing applicant entity")
          throw new InternalServerException("[CustomerIdentificationBlockBuilder] missing applicant entity")
        }

        jsonObject(
          "tradersPartyType" -> vatScheme.partyType,
          optional("identifiers" -> {
            entity match {
              case IncorporatedEntity(_, companyNumber, _, optCtutr, _, _, _, _, _, optChrn) =>
                Some(jsonObject(
                  "companyRegistrationNumber" -> companyNumber,
                  optional("ctUTR" -> optCtutr),
                  optional("CHRN" -> optChrn)
                ))
              case SoleTraderIdEntity(_, _, _, optNino, optUtr, _, _, _, _, _, _) =>
                Some(jsonObject(
                  optional("NINO" -> optNino),
                  optional("saUTR" -> optUtr)
                ))
              case PartnershipIdEntity(optUtr, _, _, _, _, optChrn, _, _, _, _) =>
                Some(jsonObject(
                  optional("saUTR" -> optUtr),
                  optional("CHRN" -> optChrn)
                ))
              case _ =>
                None
            }
          }),
          optionalRequiredIf(applicantDetails.personalDetails.exists(_.arn.isEmpty))(
            "dateOfBirth" -> applicantDetails.personalDetails.flatMap(_.dateOfBirth)
          ),
          optional("tradingName" -> business.tradingName)
        ) ++ {
          (business.shortOrgName, getCompanyName(entity)) match {
            case (Some(shortOrgName), Some(companyName)) => jsonObject(
              "shortOrgName" -> shortOrgName,
              "organisationName" -> companyName
            )
            case (None, optCompanyName) => jsonObject(
              optional("shortOrgName" -> optCompanyName),
              optional("organisationName" -> optCompanyName)
            )
          }
        }
      case (None, _) =>
        errorLog("[CustomerIdentificationAuditBlockBuilder][buildCustomerIdentificationBlock] - Could not build customerIdentification block due to missing Applicant details data")
        throw new InternalServerException("[CustomerIdentificationBlockBuilder][Audit] Could not build customerIdentification block due to missing Applicant details data")
      case (_, None) =>
        errorLog("[CustomerIdentificationAuditBlockBuilder][buildCustomerIdentificationBlock] - Could not build customerIdentification block due to missing Business details data")
        throw new InternalServerException("[CustomerIdentificationBlockBuilder][Audit] Could not build customerIdentification block due to missing Business details data")
    }
  }


  def getCompanyName(entity: BusinessEntity): Option[String] = {
    entity match {
      case IncorporatedEntity(companyName, _, _, _, _, _, _, _, _, _) => companyName
      case MinorEntity(companyName, _, _, _, _, _, _, _, _, _, _) => companyName
      case PartnershipIdEntity(_, _, companyName, _, _, _, _, _, _, _) => companyName
      case _ => None
    }
  }
}
