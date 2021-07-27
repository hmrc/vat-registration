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

import models.IncorporatedEntity
import models.api.EligibilitySubmissionData._
import play.api.libs.json.JsObject
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// scalastyle:off
@Singleton
class SubscriptionBlockBuilder @Inject()(registrationMongoRepository: RegistrationMongoRepository)(implicit ec: ExecutionContext) {

  def buildSubscriptionBlock(regId: String): Future[JsObject] = for {
    optEligibilityData <- registrationMongoRepository.fetchEligibilitySubmissionData(regId)
    optReturns <- registrationMongoRepository.fetchReturns(regId)
    optApplicantDetails <- registrationMongoRepository.getApplicantDetails(regId)
    optSicAndCompliance <- registrationMongoRepository.fetchSicAndCompliance(regId)
    optFlatRateScheme <- registrationMongoRepository.fetchFlatRateScheme(regId)
  } yield (optEligibilityData, optReturns, optApplicantDetails, optSicAndCompliance, optFlatRateScheme) match {
    case (Some(eligibilityData), Some(returns), Some(applicantDetails), Some(sicAndCompliance), optFlatRateScheme) => jsonObject(
      "reasonForSubscription" -> jsonObject(
        "registrationReason" -> eligibilityData.reasonForRegistration(),
        "relevantDate" -> {
          eligibilityData.reasonForRegistration() match {
            case `voluntaryKey` => returns.startDate
            case `backwardLookKey` => eligibilityData.threshold.thresholdInTwelveMonths
            case `forwardLookKey` => Some(eligibilityData.earliestDate)
          }
        },
        optional("voluntaryOrEarlierDate" -> returns.startDate),
        "exemptionOrException" -> eligibilityData.exceptionOrExemption
      ),
      optional("corporateBodyRegistered" -> Option(applicantDetails.entity).collect {
        case IncorporatedEntity(_, companyNumber, dateOfIncorporation, _, _, countryOfIncorporation, _, _, _, _) =>
          Some(jsonObject(
            "companyRegistrationNumber" -> companyNumber,
            "dateOfIncorporation" -> dateOfIncorporation,
            "countryOfIncorporation" -> countryOfIncorporation
          ))
      }),
      "businessActivities" -> jsonObject(
        "description" -> sicAndCompliance.businessDescription,
        "SICCodes" -> jsonObject(
          "primaryMainCode" -> sicAndCompliance.mainBusinessActivity.id,
          optional("mainCode2" -> sicAndCompliance.otherBusinessActivities.headOption.map(_.id)),
          optional("mainCode3" -> sicAndCompliance.otherBusinessActivities.lift(1).map(_.id)),
          optional("mainCode4" -> sicAndCompliance.otherBusinessActivities.lift(2).map(_.id))
        )
      ),
      "yourTurnover" -> jsonObject(
        "turnoverNext12Months" -> eligibilityData.estimates.turnoverEstimate,
        "zeroRatedSupplies" -> returns.zeroRatedSupplies,
        "VATRepaymentExpected" -> returns.reclaimVatOnMostReturns
      ),
      optional("schemes" -> optFlatRateScheme.flatMap { flatRateScheme =>
        (flatRateScheme.joinFrs, flatRateScheme.frsDetails) match {
          case (true, Some(details)) =>
            Some(jsonObject(
              optional("FRSCategory" -> details.categoryOfBusiness),
              "FRSPercentage" -> details.percent,
              optional("startDate" -> details.startDate),
              optional("limitedCostTrader" -> details.limitedCostTrader)
            ))
          case (false, _) => None
          case _ => throw new InternalServerException("[SubscriptionBlockBuilder] FRS scheme data missing when joinFrs is true")
        }
      })
    )
    case _ =>
      throw new InternalServerException(
        "[SubscriptionBlockBuilder] Could not build subscription block for submission because some of the data is missing: " +
          s"ApplicantDetails found - ${optApplicantDetails.isDefined}, " +
          s"EligibilitySubmissionData found - ${optEligibilityData.isDefined}, " +
          s"Returns found - ${optReturns.isDefined}, " +
          s"SicAndCompliance found - ${optSicAndCompliance.isDefined}."
      )
  }
}