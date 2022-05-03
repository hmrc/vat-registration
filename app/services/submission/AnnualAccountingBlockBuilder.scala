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

import models.api.VatScheme
import models.api.returns.Annual
import models._
import play.api.libs.json.JsObject
import utils.JsonUtils.jsonObject

import javax.inject.{Inject, Singleton}

@Singleton
class AnnualAccountingBlockBuilder @Inject()() {

  def buildAnnualAccountingBlock(vatScheme: VatScheme): Option[JsObject] =
    (vatScheme.returns, vatScheme.eligibilitySubmissionData) match {
      case (Some(returns), Some(eligibilitySubmissionData)) if returns.returnsFrequency.equals(Annual) =>
        Some(jsonObject(
          "submissionType" -> "1",
          "customerRequest" -> returns.annualAccountingDetails.map { details =>
            jsonObject(
              "paymentMethod" -> details.paymentMethod,
              "annualStagger" -> returns.staggerStart,
              "paymentFrequency" -> details.paymentFrequency,
              "estimatedTurnover" -> eligibilitySubmissionData.estimates.turnoverEstimate,
              "reqStartDate" -> {
                eligibilitySubmissionData.registrationReason match {
                  case Voluntary | SuppliesOutsideUk => returns.startDate
                  case BackwardLook => eligibilitySubmissionData.threshold.thresholdInTwelveMonths
                  case ForwardLook => Some(eligibilitySubmissionData.threshold.earliestDate)
                  case NonUk => eligibilitySubmissionData.threshold.thresholdOverseas
                  case TransferOfAGoingConcern => eligibilitySubmissionData.togcCole.map(_.dateOfTransfer)
                }
              }
            )
          }
        ))
      case _ => None
    }
}
