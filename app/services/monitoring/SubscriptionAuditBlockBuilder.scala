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
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.Singleton

@Singleton
class SubscriptionAuditBlockBuilder {

  def buildSubscriptionBlock(vatScheme: VatScheme): JsObject =
    (vatScheme.eligibilitySubmissionData, vatScheme.returns, vatScheme.sicAndCompliance, vatScheme.flatRateScheme) match {
      case (Some(eligibilityData), Some(returns), Some(sicAndCompliance), optFlatRateScheme) => jsonObject(
        "overThresholdIn12MonthPeriod" -> eligibilityData.threshold.thresholdInTwelveMonths.isDefined,
        optional("overThresholdIn12MonthDate" -> eligibilityData.threshold.thresholdInTwelveMonths),
        "overThresholdInPreviousMonth" -> eligibilityData.threshold.thresholdPreviousThirtyDays.isDefined,
        optional("overThresholdInPreviousMonthDate" -> eligibilityData.threshold.thresholdPreviousThirtyDays),
        "overThresholdInNextMonth" -> eligibilityData.threshold.thresholdNextThirtyDays.isDefined,
        optional("overThresholdInNextMonthDate" -> eligibilityData.threshold.thresholdNextThirtyDays),
        "reasonForSubscription" -> jsonObject(
          optional("voluntaryOrEarlierDate" -> returns.startDate),
          "exemptionOrException" -> eligibilityData.exceptionOrExemption
        ),
        "businessActivities" -> jsonObject(
          "description" -> sicAndCompliance.businessDescription,
          "sicCodes" -> jsonObject(
            "primaryMainCode" -> sicAndCompliance.mainBusinessActivity.id,
            optional("mainCode2" -> sicAndCompliance.otherBusinessActivities.headOption.map(_.id)),
            optional("mainCode3" -> sicAndCompliance.otherBusinessActivities.lift(1).map(_.id)),
            optional("mainCode4" -> sicAndCompliance.otherBusinessActivities.lift(2).map(_.id))
          )
        ),
        "yourTurnover" -> jsonObject(
          "turnoverNext12Months" -> eligibilityData.estimates.turnoverEstimate,
          "zeroRatedSupplies" -> returns.zeroRatedSupplies,
          "vatRepaymentExpected" -> returns.reclaimVatOnMostReturns
        ),
        optional("schemes" -> optFlatRateScheme.flatMap { flatRateScheme =>
          (flatRateScheme.joinFrs, flatRateScheme.frsDetails) match {
            case (true, Some(details)) =>
              Some(jsonObject(
                optional("flatRateSchemeCategory" -> details.categoryOfBusiness),
                "flatRateSchemePercentage" -> details.percent,
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
            s"EligibilitySubmissionData found - ${vatScheme.eligibilitySubmissionData.isDefined}, " +
            s"Returns found - ${vatScheme.returns.isDefined}, " +
            s"SicAndCompliance found - ${vatScheme.sicAndCompliance.isDefined}."
        )
    }
}