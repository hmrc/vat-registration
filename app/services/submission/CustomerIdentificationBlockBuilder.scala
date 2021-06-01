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

import models.LimitedCompany
import play.api.libs.json.{JsObject, Json}
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// scalastyle:off
@Singleton
class CustomerIdentificationBlockBuilder @Inject()(registrationMongoRepository: RegistrationMongoRepository
                                                  )(implicit ec: ExecutionContext) {

  def buildCustomerIdentificationBlock(regId: String): Future[JsObject] = for {
    optVatScheme <- registrationMongoRepository.retrieveVatScheme(regId)
    optApplicantDetails = optVatScheme.flatMap(_.applicantDetails)
    optTradingDetails = optVatScheme.flatMap(_.tradingDetails)
  } yield (optVatScheme, optApplicantDetails, optTradingDetails) match {
    case (Some(vatScheme), Some(applicantDetails), Some(tradingDetails)) =>
      jsonObject(
        "tradersPartyType" -> vatScheme.eligibilitySubmissionData.map(_.partyType),
        optional("shortOrgName" -> Option(applicantDetails.entity).collect {
          case LimitedCompany(companyName, _, _, _, _, _, _, _, _) => companyName
        }),
        optional("tradingName" -> tradingDetails.tradingName)
      ) ++ {
        applicantDetails.entity.bpSafeId match {
          case Some(bpSafeId) =>
            Json.obj("primeBPSafeID" -> bpSafeId)
          case None if applicantDetails.entity.identifiers.nonEmpty =>
            Json.obj("customerID" -> Json.toJson(applicantDetails.entity.identifiers))
          case _ =>
            Json.obj()
        }
      }
    case (None, _, _) =>
      throw new InternalServerException("Could not retrieve VAT scheme")
    case (_, None, _) =>
      throw new InternalServerException("Could not build customer identification block for submission due to missing applicant details data")
    case (_, _, None) =>
      throw new InternalServerException("Could not build customer identification block for submission due to missing trading details data")
    case _ =>
      throw new InternalServerException("Could not build customer identification block for submission due to missing data from applicant and trading details")
  }

}
