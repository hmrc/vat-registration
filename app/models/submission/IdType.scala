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

sealed trait IdType

case object NinoIdType extends IdType

case object UtrIdType extends IdType

case object CrnIdType extends IdType

case object TempNinoIDType extends IdType

case object CharityRefIdType extends IdType

case object CascIdType extends IdType

case object VrnIdType extends IdType

case object EmpRefIdType extends IdType

case object OtherIdType extends IdType


object IdType {

  val stati: Map[IdType, String] = Map(
    NinoIdType -> "NINO",
    UtrIdType -> "UTR",
    CrnIdType -> "CRN",
    TempNinoIDType -> "TEMPNI",
    CharityRefIdType -> "CHRN",
    CascIdType -> "CASC",
    VrnIdType -> "VRN",
    EmpRefIdType -> "EMPREF",
    OtherIdType -> "OTHER"
  )

  val inverseStati = stati.map(_.swap)

  def fromString(value: String): IdType = inverseStati(value)
  def toJsString(value: IdType): JsString = JsString(stati(value))

  implicit val writes = Writes[IdType] { idType => toJsString(idType) }
  implicit val reads = Reads[IdType] { idType => idType.validate[String] map fromString }
  implicit val format = Format[IdType](reads, writes)

}
