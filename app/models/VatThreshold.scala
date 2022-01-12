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

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JsString, Reads, Writes, __}

case class VatThreshold(date: DateTime, amount: String)

object VatThreshold {

  val dateFormat: Format[DateTime] = Format[DateTime](
    Reads[DateTime](js =>
      js.validate[String].map(
        DateTime.parse(_, DateTimeFormat.forPattern("yyyy-MM-dd"))
      )
    ),
    Writes[DateTime] { dt =>
       JsString(dt.toString("yyyy-MM-dd"))
    }
  )

  val reads: Reads[VatThreshold] = (
      (__ \ "date").read[DateTime](dateFormat) and
      (__ \ "amount").read[String]
    )(VatThreshold.apply _)

  val writes: Writes[VatThreshold] = (
      (__ \ "since").write(dateFormat) and
        (__ \ "taxable-threshold").write[String]
    )(unlift(VatThreshold.unapply))

  implicit val format: Format[VatThreshold] = Format(reads, writes)
}