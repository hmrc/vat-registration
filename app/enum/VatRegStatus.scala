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

package enums

import play.api.libs.json._

object VatRegStatus extends Enumeration {
  val invalid      = Value
  val draft        = Value
  val locked       = Value
  val held         = Value
  val submitted    = Value
  val acknowledged = Value
  val rejected     = Value
  val cancelled    = Value

  implicit val format = Format(
    Reads[VatRegStatus.Value] { json => json.validate[String].map(VatRegStatus.withName) },
    Writes[VatRegStatus.Value] { value => JsString(value.toString) }
  )

}
