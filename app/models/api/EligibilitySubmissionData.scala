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

package models.api

import models.api.EligibilitySubmissionData._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate

case class EligibilitySubmissionData(threshold: Threshold,
                                     exceptionOrExemption: String,
                                     estimates: TurnoverEstimates,
                                     customerStatus: CustomerStatus) {

  def earliestDate: LocalDate = Seq(
    threshold.thresholdPreviousThirtyDays,
    threshold.thresholdInTwelveMonths.map(_.withDayOfMonth(1).plusMonths(2)),
    threshold.thresholdNextThirtyDays
  ).flatten.minBy(date => date.toEpochDay)

  def reasonForRegistration(humanReadable: Boolean = false): String = threshold match {
    case Threshold(false, _, _, _) =>
      if (humanReadable) voluntaryHumanReadable else voluntaryKey
    case Threshold(true, forwardLook1, _, forwardLook2) if forwardLook1.contains(earliestDate) || forwardLook2.contains(earliestDate) =>
      if (humanReadable) forwardLookHumanReadable else forwardLookKey
    case _ =>
      if (humanReadable) backwardLookHumanReadable else backwardLookKey
  }
}

object EligibilitySubmissionData {
  val voluntaryKey = "0018"
  val forwardLookKey = "0016"
  val backwardLookKey = "0015"

  val voluntaryHumanReadable = "Voluntary"
  val forwardLookHumanReadable = "Forward Look"
  val backwardLookHumanReadable = "Backward Look"

  val exceptionKey = "2"
  val exemptionKey = "1"
  val nonExceptionOrExemptionKey = "0"

  val eligibilityReads: Reads[EligibilitySubmissionData] = Reads { json =>
    (
      json.validate[Threshold](Threshold.eligibilityDataJsonReads) and
        (
          (json \ "vatRegistrationException").validateOpt[Boolean] and
            (json \ "vatExemption").validateOpt[Boolean]
          ) ((exception, exemption) =>
          (exception.contains(true), exemption.contains(true)) match {
            case (excepted, exempt) if !excepted && !exempt => nonExceptionOrExemptionKey
            case (excepted, _) if excepted => exceptionKey
            case (_, exempt) if exempt => exemptionKey
            case (_, _) =>
              throw new InternalServerException("[EligibilitySubmissionData][eligibilityReads] eligibility returned invalid exception/exemption data")
          }
        ) and
        json.validate[TurnoverEstimates](TurnoverEstimates.eligibilityDataJsonReads) and
        json.validate[CustomerStatus](CustomerStatus.eligibilityDataJsonReads)
      ) (EligibilitySubmissionData.apply _)
  }

  implicit val format: Format[EligibilitySubmissionData] = Json.format[EligibilitySubmissionData]
}