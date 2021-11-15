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

import models.api.returns.NIPCompliance
import models._
import play.api.libs.json.JsObject
import repositories.VatSchemeRepository
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// scalastyle:off
@Singleton
class SubscriptionBlockBuilder @Inject()(registrationMongoRepository: VatSchemeRepository)(implicit ec: ExecutionContext) {

  def buildSubscriptionBlock(regId: String): Future[JsObject] = for {
    optEligibilityData <- registrationMongoRepository.fetchEligibilitySubmissionData(regId)
    optReturns <- registrationMongoRepository.fetchReturns(regId)
    partyType = optEligibilityData.map(_.partyType).getOrElse(
      throw new InternalServerException("[SubscriptionBlockBuilder] Could not build subscription block due to missing party type")
    )
    optApplicantDetails <- registrationMongoRepository.getApplicantDetails(regId, partyType)
    optSicAndCompliance <- registrationMongoRepository.fetchSicAndCompliance(regId)
    optFlatRateScheme <- registrationMongoRepository.fetchFlatRateScheme(regId)
  } yield (optEligibilityData, optReturns, optApplicantDetails, optSicAndCompliance, optFlatRateScheme) match {
    case (Some(eligibilityData), Some(returns), Some(applicantDetails), Some(sicAndCompliance), optFlatRateScheme) => jsonObject(
      "reasonForSubscription" -> jsonObject(
        "registrationReason" -> eligibilityData.registrationReason.key,
        "relevantDate" -> {
          eligibilityData.registrationReason match {
            case Voluntary | SuppliesOutsideUk => returns.startDate
            case BackwardLook => eligibilityData.threshold.thresholdInTwelveMonths
            case ForwardLook => Some(eligibilityData.threshold.earliestDate)
            case NonUk => eligibilityData.threshold.thresholdOverseas
          }
        },
        optional("voluntaryOrEarlierDate" -> returns.startDate),
        "exemptionOrException" -> eligibilityData.exceptionOrExemption
      ),
      optional("corporateBodyRegistered" -> Option(applicantDetails.entity).collect {
        case IncorporatedEntity(_, companyNumber, dateOfIncorporation, _, _, countryOfIncorporation, _, _, _, _) =>
          Some(jsonObject(
            "companyRegistrationNumber" -> companyNumber,
            optional("dateOfIncorporation" -> dateOfIncorporation),
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
        ),
        optional("goodsToOverseas" -> returns.overseasCompliance.map(_.goodsToOverseas)),
        optional("goodsToCustomerEU" -> returns.overseasCompliance.flatMap(_.goodsToEu)),
        optional("storingGoodsForDispatch" -> returns.overseasCompliance.map(_.storingGoodsForDispatch)),
        optional("fulfilmentWarehouse" -> returns.overseasCompliance.flatMap(_.usingWarehouse)),
        optional("FHDDSWarehouseNumber" -> returns.overseasCompliance.flatMap(_.fulfilmentWarehouseNumber)),
        optional("nameOfWarehouse" -> returns.overseasCompliance.flatMap(_.fulfilmentWarehouseName))
      ),
      "yourTurnover" -> (jsonObject(
        "turnoverNext12Months" -> eligibilityData.estimates.turnoverEstimate,
        "zeroRatedSupplies" -> returns.zeroRatedSupplies,
        "VATRepaymentExpected" -> returns.reclaimVatOnMostReturns
      ) ++ {
        returns.northernIrelandProtocol match {
          case Some(NIPCompliance(goodsToEU, goodsFromEU)) => jsonObject(
            conditional(goodsFromEU.answer)("goodsFromOtherEU" -> goodsFromEU.value),
            conditional(goodsToEU.answer)("goodsSoldToOtherEU" -> goodsToEU.value)
          )
          case None => jsonObject()
        }
      }),
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