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
import models.api.vatapplication.NIPCompliance
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.Singleton

// scalastyle:off
@Singleton
class SubscriptionAuditBlockBuilder {

  def buildSubscriptionBlock(vatScheme: VatScheme): JsObject =
    (vatScheme.eligibilitySubmissionData, vatScheme.vatApplication, vatScheme.business, vatScheme.flatRateScheme, vatScheme.otherBusinessInvolvements.getOrElse(Nil)) match {
      case (Some(eligibilityData), Some(vatApplication), Some(business), optFlatRateScheme, otherBusinessInvolvements) => jsonObject(
        "overThresholdIn12MonthPeriod" -> eligibilityData.threshold.thresholdInTwelveMonths.isDefined,
        optional("overThresholdIn12MonthDate" -> eligibilityData.threshold.thresholdInTwelveMonths),
        "overThresholdInPreviousMonth" -> eligibilityData.threshold.thresholdPreviousThirtyDays.isDefined,
        optional("overThresholdInPreviousMonthDate" -> eligibilityData.threshold.thresholdPreviousThirtyDays),
        "overThresholdInNextMonth" -> eligibilityData.threshold.thresholdNextThirtyDays.isDefined,
        optional("overThresholdInNextMonthDate" -> eligibilityData.threshold.thresholdNextThirtyDays),
        "reasonForSubscription" -> jsonObject(
          conditional(vatApplication.startDate.exists(date => !eligibilityData.calculatedDate.contains(date)))(
            "voluntaryOrEarlierDate" -> vatApplication.startDate
          ),
          "exemptionOrException" -> VatScheme.exceptionOrExemption(eligibilityData, vatApplication)
        ),
        "businessActivities" -> jsonObject(
          "description" -> business.businessDescription,
          "sicCodes" -> jsonObject(
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
        "yourTurnover" -> (jsonObject(
          "turnoverNext12Months" -> vatApplication.turnoverEstimate,
          "zeroRatedSupplies" -> vatApplication.zeroRatedSupplies,
          "vatRepaymentExpected" -> vatApplication.claimVatRefunds
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
        optional("schemes" -> optFlatRateScheme.flatMap { flatRateScheme =>
          (flatRateScheme.joinFrs, flatRateScheme.categoryOfBusiness, flatRateScheme.percent, flatRateScheme.frsStart, flatRateScheme.limitedCostTrader) match {
            case (Some(true), optCategoryOfBusiness, Some(percent), Some(frsStartDate), Some(limitedCostTrader)) =>
              Some(jsonObject(
                optional("flatRateSchemeCategory" -> optCategoryOfBusiness),
                "flatRateSchemePercentage" -> percent,
                "startDate" -> frsStartDate,
                "limitedCostTrader" -> limitedCostTrader
              ))
            case (Some(false), _, _, _, _) => None
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
              required("businessName" -> involvement.businessName),
              optional("idType" -> involvement.optIdType),
              optional("idValue" -> involvement.optIdValue),
              required("stillTrading" -> involvement.stillTrading)
            )
          }
        )
      )
      case _ =>
        throw new InternalServerException(
          "[SubscriptionBlockBuilder] Could not build subscription block for submission because some of the data is missing: " +
            s"EligibilitySubmissionData found - ${vatScheme.eligibilitySubmissionData.isDefined}, " +
            s"VatApplication found - ${vatScheme.vatApplication.isDefined}, " +
            s"Business found - ${vatScheme.business.isDefined}."
        )
    }
}