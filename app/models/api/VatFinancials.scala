/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time.LocalDate

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class VatFinancials(bankAccount: Option[VatBankAccount] = None,
                         turnoverEstimate: Long,
                         zeroRatedTurnoverEstimate: Option[Long] = None)

object VatFinancials {
  implicit def format(implicit f: OFormat[VatBankAccount]): OFormat[VatFinancials] = (
    (__ \ "bankAccount").formatNullable[VatBankAccount](f) and
    (__ \ "turnoverEstimate").format[Long] and
    (__ \ "zeroRatedTurnoverEstimate").formatNullable[Long]
  )(VatFinancials.apply, unlift(VatFinancials.unapply))
}

case class StartDate(date: Option[LocalDate])

object StartDate {
  implicit val format: OFormat[StartDate] = Json.format[StartDate]
}

case class Returns(reclaimVatOnMostReturns: Boolean,
                   frequency: String,
                   staggerStart: Option[String],
                   start: StartDate)

object Returns extends VatAccountingPeriodValidator {
  implicit val format: OFormat[Returns] = (
    (__ \ "reclaimVatOnMostReturns").format[Boolean] and
    (__ \ "frequency").format[String](frequencyValidator) and
    (__ \ "staggerStart").formatNullable[String](staggerStartValidator) and
    (__ \ "start").format[StartDate]
  )(Returns.apply, unlift(Returns.unapply))
}

case class TurnoverEstimates(vatTaxable: Long)

object TurnoverEstimates {
  implicit val format: OFormat[TurnoverEstimates] = Json.format[TurnoverEstimates]
}
