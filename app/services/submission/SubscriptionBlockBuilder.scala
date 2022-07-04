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

import models._
import models.api.VatScheme
import models.api.returns.NIPCompliance
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.{Inject, Singleton}

// scalastyle:off
@Singleton
class SubscriptionBlockBuilder @Inject()() {

  def buildSubscriptionBlock(vatScheme: VatScheme): JsObject =
    (vatScheme.eligibilitySubmissionData, vatScheme.returns, vatScheme.applicantDetails, vatScheme.business, vatScheme.otherBusinessInvolvements.getOrElse(Nil)) match {
      case (Some(eligibilityData), Some(returns), Some(applicantDetails), Some(business), otherBusinessInvolvements) => jsonObject(
        "reasonForSubscription" -> jsonObject(
          "registrationReason" -> eligibilityData.registrationReason.key,
          "relevantDate" -> {
            eligibilityData.registrationReason match {
              case Voluntary | SuppliesOutsideUk | GroupRegistration | IntendingTrader => returns.startDate
              case BackwardLook => eligibilityData.threshold.thresholdInTwelveMonths
              case ForwardLook => Some(eligibilityData.threshold.earliestDate)
              case NonUk => eligibilityData.threshold.thresholdOverseas
              case TransferOfAGoingConcern => eligibilityData.togcCole.map(_.dateOfTransfer)
            }
          },
          conditional(returns.startDate.exists(date => !eligibilityData.calculatedDate.contains(date)))(
            "voluntaryOrEarlierDate" -> returns.startDate
          ),
          "exemptionOrException" -> VatScheme.exceptionOrExemption(eligibilityData, returns)
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
          required("description" -> business.businessDescription),
          "SICCodes" -> jsonObject(
            required("primaryMainCode" -> business.mainBusinessActivity.map(_.id)),
            optional("mainCode2" -> business.otherBusinessActivities.headOption.map(_.id)),
            optional("mainCode3" -> business.otherBusinessActivities.lift(1).map(_.id)),
            optional("mainCode4" -> business.otherBusinessActivities.lift(2).map(_.id))
          ),
          optional("goodsToOverseas" -> returns.overseasCompliance.map(_.goodsToOverseas)),
          optional("goodsToCustomerEU" -> returns.overseasCompliance.flatMap(_.goodsToEu)),
          optional("storingGoodsForDispatch" -> returns.overseasCompliance.map(_.storingGoodsForDispatch)),
          optional("fulfilmentWarehouse" -> returns.overseasCompliance.flatMap(_.usingWarehouse)),
          optional("FHDDSWarehouseNumber" -> returns.overseasCompliance.flatMap(_.fulfilmentWarehouseNumber)),
          optional("nameOfWarehouse" -> returns.overseasCompliance.flatMap(_.fulfilmentWarehouseName))
        ),
        "yourTurnover" -> (
          jsonObject(
            required("turnoverNext12Months" -> returns.turnoverEstimate),
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
        optional("schemes" -> vatScheme.flatRateScheme.flatMap { flatRateScheme =>
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
        }),
        optional("takingOver" -> eligibilityData.togcCole.map { togcData =>
          jsonObject(
            "prevOwnerName" -> togcData.previousBusinessName,
            "prevOwnerVATNumber" -> togcData.vatRegistrationNumber,
            "keepPrevOwnerVATNo" -> togcData.wantToKeepVatNumber,
            "acceptTsAndCsForTOGCOrCOLE" -> (if (togcData.wantToKeepVatNumber) {
              togcData.agreedWithTermsForKeepingVat.getOrElse(throw new InternalServerException("TOGC user wants to keep VRN but did not answer T&C"))
            } else {
              false
            })
          )
        }),
        conditional(business.otherBusinessInvolvement.contains(true) && otherBusinessInvolvements.nonEmpty)(
          "otherBusinessActivities" -> otherBusinessInvolvements.map { involvement =>
            jsonObject(
              "businessName" -> involvement.businessName,
              optional("idType" -> involvement.optIdType),
              optional("idValue" -> involvement.optIdValue),
              "stillTrading" -> involvement.stillTrading
            )
          }
        )
      )
      case _ =>
        throw new InternalServerException(
          "[SubscriptionBlockBuilder] Could not build subscription block for submission because some of the data is missing: " +
            s"ApplicantDetails found - ${vatScheme.applicantDetails.isDefined}, " +
            s"EligibilitySubmissionData found - ${vatScheme.eligibilitySubmissionData.isDefined}, " +
            s"Returns found - ${vatScheme.returns.isDefined}, " +
            s"Business found - ${vatScheme.business.isDefined}."
        )
    }
}