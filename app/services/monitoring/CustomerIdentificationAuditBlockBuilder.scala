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

package services.monitoring

import featureswitch.core.config.{FeatureSwitching, ShortOrgName}
import models.api.{ApplicantDetails, VatScheme}
import models.{IncorporatedEntity, MinorEntity, PartnershipIdEntity, SoleTraderIdEntity}
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.Singleton

// scalastyle:off
@Singleton
class CustomerIdentificationAuditBlockBuilder extends FeatureSwitching {

  def buildCustomerIdentificationBlock(vatScheme: VatScheme): JsValue = {
    (vatScheme.applicantDetails, vatScheme.tradingDetails) match {
      case (Some(applicantDetails), Some(tradingDetails)) =>
        jsonObject(
          "tradersPartyType" -> vatScheme.partyType,
          optional("identifiers" -> {
            applicantDetails.entity match {
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
          optionalRequiredIf(applicantDetails.personalDetails.arn.isEmpty)("dateOfBirth" -> applicantDetails.personalDetails.dateOfBirth),
          optional("tradingName" -> tradingDetails.tradingName)
        ) ++ {
          (tradingDetails.shortOrgName, getCompanyName(applicantDetails)) match {
            case (Some(shortOrgName), Some(companyName)) if isEnabled(ShortOrgName) => jsonObject(
              "shortOrgName" -> shortOrgName,
              "organisationName" -> companyName
            )
            case (None, optCompanyName) if isEnabled(ShortOrgName) => jsonObject(
              optional("shortOrgName" -> optCompanyName),
              optional("organisationName" -> optCompanyName)
            )
            case (_, optCompanyName) => jsonObject(optional("shortOrgName" -> optCompanyName))
          }
        }
      case (None, _) =>
        throw new InternalServerException("[CustomerIdentificationBlockBuilder][Audit] Could not build customerIdentification block due to missing Applicant details data")
      case (_, None) =>
        throw new InternalServerException("[CustomerIdentificationBlockBuilder][Audit] Could not build customerIdentification block due to missing Trading details data")
    }
  }


  def getCompanyName(applicantDetails: ApplicantDetails): Option[String] = {
    applicantDetails.entity match {
      case IncorporatedEntity(companyName, _, _, _, _, _, _, _, _, _) => companyName
      case MinorEntity(companyName, _, _, _, _, _, _, _, _, _, _) => companyName
      case PartnershipIdEntity(_, _, companyName, _, _, _, _, _, _, _) => companyName
      case _ => None
    }
  }
}
