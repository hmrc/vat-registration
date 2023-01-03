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
import models.api.vatapplication.Annual
import play.api.libs.json.JsObject
import utils.JsonUtils.{jsonObject, required}

import javax.inject.{Inject, Singleton}

@Singleton
class AnnualAccountingBlockBuilder @Inject()() {

  def buildAnnualAccountingBlock(vatScheme: VatScheme): Option[JsObject] =
    (vatScheme.vatApplication, vatScheme.eligibilitySubmissionData) match {
      case (Some(vatApplication), Some(eligibilitySubmissionData)) if vatApplication.returnsFrequency.contains(Annual) =>
        Some(jsonObject(
          "submissionType" -> "1",
          "customerRequest" -> vatApplication.annualAccountingDetails.map { details =>
            jsonObject(
              required("paymentMethod" -> details.paymentMethod),
              "annualStagger" -> vatApplication.staggerStart,
              required("paymentFrequency" -> details.paymentFrequency),
              required("estimatedTurnover" -> vatApplication.turnoverEstimate),
              "reqStartDate" -> {
                eligibilitySubmissionData.registrationReason match {
                  case Voluntary | SuppliesOutsideUk | IntendingTrader => vatApplication.startDate
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
