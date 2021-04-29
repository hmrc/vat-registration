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

import play.api.libs.json._

sealed trait PaymentFrequency

case object Monthly extends PaymentFrequency

case object Quarterly extends PaymentFrequency

object PaymentFrequency {

  val monthly: String = "M"
  val quarterly: String = "Q"

  val reads: Reads[PaymentFrequency] = Reads[PaymentFrequency] {
      case JsString(`monthly`) => JsSuccess(Monthly)
      case JsString(`quarterly`) => JsSuccess(Quarterly)
      case _ => JsError("Could not parse payment frequency")
    }

  val writes: Writes[PaymentFrequency] = Writes[PaymentFrequency] {
    case Monthly => JsString(monthly)
    case Quarterly => JsString(quarterly)
  }

  implicit val format: Format[PaymentFrequency] = Format(reads, writes)

}
