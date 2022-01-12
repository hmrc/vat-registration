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

package models.submission

import play.api.libs.json.{Format, JsString, Reads, Writes}

sealed trait IdVerificationStatus

case object IdVerified extends IdVerificationStatus

case object IdUnverifiable extends IdVerificationStatus

case object IdVerificationFailed extends IdVerificationStatus


object IdVerificationStatus {

  val stati: Map[IdVerificationStatus, String] = Map(
    IdVerified -> "1",
    IdUnverifiable -> "2",
    IdVerificationFailed -> "3"
  )

  val inverseStati = stati.map(_.swap)

  def fromString(value: String): IdVerificationStatus = inverseStati(value)
  def toJsString(value: IdVerificationStatus): JsString = JsString(stati(value))

  implicit val writes = Writes[IdVerificationStatus] { status => toJsString(status) }
  implicit val reads = Reads[IdVerificationStatus] { status => status.validate[String] map fromString }
  implicit val format = Format[IdVerificationStatus](reads, writes)

}
