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
import models.api.VatScheme
import play.api.libs.json.JsObject
import play.api.mvc.Request
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._
import utils.LoggingUtils

import javax.inject.{Inject, Singleton}

// scalastyle:off
@Singleton
class SubscriptionBlockBuilder @Inject()() extends LoggingUtils{

  def buildSubscriptionBlock(vatScheme: VatScheme)(implicit request: Request[_]): JsObject =
    (vatScheme.eligibilitySubmissionData, vatScheme.vatApplication, vatScheme.applicantDetails, vatScheme.business, vatScheme.otherBusinessInvolvements.getOrElse(Nil)) match {
      case (Some(eligibilityData), Some(vatApplication), Some(applicantDetails), Some(business), otherBusinessInvolvements) => jsonObject(
        "reasonForSubscription" -> jsonObject(
          "registrationReason" -> {
            if (vatApplication.currentlyTrading.contains(false) && eligibilityData.registrationReason.equals(Voluntary)) IntendingTrader.key
            else eligibilityData.registrationReason.key
          },
          "relevantDate" -> {
            eligibilityData.registrationReason match {
              case Voluntary | SuppliesOutsideUk | GroupRegistration | IntendingTrader => vatApplication.startDate
              case BackwardLook => eligibilityData.threshold.thresholdInTwelveMonths
              case ForwardLook => Some(eligibilityData.threshold.earliestDate)
              case NonUk => eligibilityData.threshold.thresholdOverseas
              case TransferOfAGoingConcern => eligibilityData.togcCole.map(_.dateOfTransfer)
            }
          },
          conditional(vatApplication.startDate.exists(date => !eligibilityData.calculatedDate.contains(date)))(
            "voluntaryOrEarlierDate" -> vatApplication.startDate
          ),
          "exemptionOrException" -> VatScheme.exceptionOrExemption(eligibilityData, vatApplication)
        ),
        optional("corporateBodyRegistered" -> applicantDetails.entity.collect {
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
          optional("goodsToOverseas" -> vatApplication.overseasCompliance.flatMap(_.goodsToOverseas)),
          optional("goodsToCustomerEU" -> vatApplication.overseasCompliance.flatMap(_.goodsToEu)),
          optional("storingGoodsForDispatch" -> vatApplication.overseasCompliance.flatMap(_.storingGoodsForDispatch)),
          optional("fulfilmentWarehouse" -> vatApplication.overseasCompliance.flatMap(_.usingWarehouse)),
          optional("FHDDSWarehouseNumber" -> vatApplication.overseasCompliance.flatMap(_.fulfilmentWarehouseNumber)),
          optional("nameOfWarehouse" -> vatApplication.overseasCompliance.flatMap(_.fulfilmentWarehouseName))
        ),
        "yourTurnover" -> (
          jsonObject(
            required("turnoverNext12Months" -> vatApplication.turnoverEstimate),
            "zeroRatedSupplies" -> vatApplication.zeroRatedSupplies,
            "VATRepaymentExpected" -> vatApplication.claimVatRefunds
          ) ++ {
            vatApplication.northernIrelandProtocol.flatMap(_.goodsFromEU) match {
              case Some(conditionalValue) => jsonObject(
                conditional(conditionalValue.answer)("goodsFromOtherEU" -> conditionalValue.value)
              )
              case None => jsonObject()
            }
          } ++ {
            vatApplication.northernIrelandProtocol.flatMap(_.goodsToEU) match {
              case Some(conditionalValue) => jsonObject(
                conditional(conditionalValue.answer)("goodsSoldToOtherEU" -> conditionalValue.value)
              )
              case None => jsonObject()
            }
          }),
        optional("schemes" -> vatScheme.flatRateScheme.flatMap { flatRateScheme =>
          (flatRateScheme.joinFrs, flatRateScheme.categoryOfBusiness, flatRateScheme.percent, flatRateScheme.frsStart, flatRateScheme.limitedCostTrader) match {
            case (Some(true), optCategoryOfBusiness, Some(percent), Some(frsStartDate), Some(limitedCostTrader)) =>
              Some(jsonObject(
                optional("FRSCategory" -> optCategoryOfBusiness),
                "FRSPercentage" -> percent,
                "startDate" -> frsStartDate,
                "limitedCostTrader" -> limitedCostTrader
              ))
            case (Some(false), _, _, _, _) => None
            case _ =>
              errorLog("[SubscriptionBlockBuilder][buildSubscriptionBlock] - FRS scheme data missing when joinFrs is true")
              throw new InternalServerException("[SubscriptionBlockBuilder] FRS scheme data missing when joinFrs is true")
          }
        }),
        optional("takingOver" -> eligibilityData.togcCole.map { togcData =>
          jsonObject(
            "prevOwnerName" -> togcData.previousBusinessName,
            "prevOwnerVATNumber" -> togcData.vatRegistrationNumber,
            "keepPrevOwnerVATNo" -> togcData.wantToKeepVatNumber,
            "acceptTsAndCsForTOGCOrCOLE" -> (if (togcData.wantToKeepVatNumber) {
              togcData.agreedWithTermsForKeepingVat.getOrElse{
                errorLog("[SubscriptionBlockBuilder][buildSubscriptionBlock] - TOGC user wants to keep VRN but did not answer T&C")
                throw new InternalServerException("TOGC user wants to keep VRN but did not answer T&C")
              }
            } else {
              false
            })
          )
        }),
        conditional(business.otherBusinessInvolvement.contains(true) && otherBusinessInvolvements.nonEmpty)(
          "otherBusinessActivities" -> otherBusinessInvolvements.map { involvement =>
            jsonObject(
              required("businessName" -> involvement.businessName),
              optional("idType" -> involvement.optIdType),
              optional("idValue" -> involvement.optIdValue),
              required("stillTrading" -> involvement.stillTrading)
            )
          }
        )
      )
      case _ =>
        errorLog("[SubscriptionBlockBuilder][buildSubscriptionBlock] - Could not build subscription block for submission because some of the data is missing")
        throw new InternalServerException(
          "[SubscriptionBlockBuilder] Could not build subscription block for submission because some of the data is missing: " +
            s"ApplicantDetails found - ${vatScheme.applicantDetails.isDefined}, " +
            s"EligibilitySubmissionData found - ${vatScheme.eligibilitySubmissionData.isDefined}, " +
            s"VatApplication found - ${vatScheme.vatApplication.isDefined}, " +
            s"Business found - ${vatScheme.business.isDefined}."
        )
    }
}