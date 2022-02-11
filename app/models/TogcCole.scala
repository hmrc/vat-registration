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

package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

case class TogcCole(dateOfTransfer: LocalDate,
                    previousBusinessName: String,
                    vatRegistrationNumber: String,
                    wantToKeepVatNumber: Boolean,
                    agreedWithTermsForKeepingVat: Option[Boolean])

object TogcCole {
  implicit val format: Format[TogcCole] = Json.format[TogcCole]
  val eligibilityDataJsonReads: Reads[TogcCole] = (
    (__ \ "dateOfBusinessTransfer-value").read[LocalDate] and
    (__ \ "previousBusinessName-value").read[String] and
    (__ \ "vatNumber-value").read[String] and
    (__ \ "keepVatNumber-value").read[Boolean] and
    (__ \ "vatTermsAndConditions-value").readNullable[Boolean]
  )(TogcCole.apply _)
}
