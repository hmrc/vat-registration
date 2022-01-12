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

package models.api

import play.api.libs.json._
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate

case class Threshold(mandatoryRegistration: Boolean,
                     thresholdPreviousThirtyDays: Option[LocalDate] = None,
                     thresholdInTwelveMonths: Option[LocalDate] = None,
                     thresholdNextThirtyDays: Option[LocalDate] = None,
                     thresholdOverseas: Option[LocalDate] = None) {

  def earliestDate: LocalDate = Seq(
    thresholdPreviousThirtyDays,
    thresholdInTwelveMonths.map(_.withDayOfMonth(1).plusMonths(2)),
    thresholdNextThirtyDays
  ).flatten.minBy(date => date.toEpochDay)

}

object Threshold {

  val eligibilityDataJsonReads: Reads[Threshold] = Reads { json =>
    (
      (json \ "voluntaryRegistration").validateOpt[Boolean],
      (json \ "thresholdPreviousThirtyDays-optionalData").validateOpt[LocalDate],
      (json \ "thresholdInTwelveMonths-optionalData").validateOpt[LocalDate],
      (json \ "thresholdNextThirtyDays-optionalData").validateOpt[LocalDate],
      (json \ "thresholdTaxableSupplies-value").validateOpt[LocalDate]
    ) match {
      case (JsSuccess(voluntaryRegistration, _), JsSuccess(thresholdPreviousThirtyDays, _),
      JsSuccess(thresholdInTwelveMonths, _), JsSuccess(thresholdNextThirtyDays, _), JsSuccess(thresholdOverseas, _)) =>
        val isMandatory = voluntaryRegistration match {
          case None if Seq(thresholdInTwelveMonths, thresholdNextThirtyDays, thresholdPreviousThirtyDays, thresholdOverseas).flatten.isEmpty =>
            false // SuppliesOutsideUk journey
          case None =>
            true // Mandatory journey
          case Some(isVoluntary) =>
            !isVoluntary // Will only ever come back as false in this case as otherwise voluntary answer not provided
        }

        if (thresholdOverseas.nonEmpty & Seq(thresholdInTwelveMonths, thresholdNextThirtyDays, thresholdPreviousThirtyDays).flatten.nonEmpty) {
          throw new InternalServerException("[Threshold][eligibilityDataJsonReads] overseas user has more than one threshold date")
        }

        JsSuccess(Threshold(
          isMandatory,
          thresholdPreviousThirtyDays,
          thresholdInTwelveMonths,
          thresholdNextThirtyDays,
          thresholdOverseas
        ))
      case (voluntaryRegistration, thresholdPreviousThirtyDays, thresholdInTwelveMonths, thresholdNextThirtyDays, thresholdOverseas) =>
        val seqErrors = voluntaryRegistration.fold(identity, _ => Seq.empty) ++
          thresholdPreviousThirtyDays.fold(identity, _ => Seq.empty) ++
          thresholdInTwelveMonths.fold(identity, _ => Seq.empty) ++
          thresholdNextThirtyDays.fold(identity, _ => Seq.empty) ++
          thresholdOverseas.fold(identity, _ => Seq.empty)

        JsError(seqErrors)
    }
  }

  implicit val format: OFormat[Threshold] = Json.format

}
