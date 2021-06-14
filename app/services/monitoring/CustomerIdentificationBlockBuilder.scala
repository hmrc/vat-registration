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

package services.monitoring

import models.api.VatScheme
import models.{LimitedCompany, SoleTrader}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.Singleton

@Singleton
class CustomerIdentificationBlockBuilder {

  def buildCustomerIdentificationBlock(vatScheme: VatScheme): JsValue = {
    (vatScheme.applicantDetails, vatScheme.tradingDetails) match {
      case (Some(applicantDetails), Some(tradingDetails)) =>
        jsonObject(
          "tradersPartyType" -> vatScheme.partyType,
          "identifiers" -> {
            applicantDetails.entity match {
              case LimitedCompany(_, companyNumber, _, ctutr, _, _, _, _, _) =>
                Json.obj(
                  "companyRegistrationNumber" -> companyNumber,
                  "ctUTR" -> ctutr
                )
              case SoleTrader(utr, _, _, _, _) =>
                Json.obj(
                  "saUTR" -> utr
                )
            }
          },
          optional("shortOrgName" -> {
            applicantDetails.entity match {
              case LimitedCompany(companyName, _, _, _, _, _, _, _, _) => Some(companyName)
              case _ => None
            }
          }),
          "dateOfBirth" -> applicantDetails.transactor.dateOfBirth,
          optional("tradingName" -> tradingDetails.tradingName)
        )
      case (None, _) =>
        throw new InternalServerException("[CustomerIdentificationBlockBuilder][Audit] Could not build customerIdentification block due to missing Applicant details data")
      case (_, None) =>
        throw new InternalServerException("[CustomerIdentificationBlockBuilder][Audit] Could not build customerIdentification block due to missing Trading details data")
    }
  }


}
