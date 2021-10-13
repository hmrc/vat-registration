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

package models.submission

import play.api.libs.json.{Format, JsString, Reads, Writes}

sealed trait RoleInBusiness

case object OwnerProprietor extends RoleInBusiness
case object PartnerRole extends RoleInBusiness
case object Director extends RoleInBusiness
case object CompanySecretary extends RoleInBusiness
case object Trustee extends RoleInBusiness
case object BoardMember extends RoleInBusiness
case object AccountantAgent extends RoleInBusiness
case object Representative extends RoleInBusiness
case object AuthorisedEmployee extends RoleInBusiness
case object Other extends RoleInBusiness
case object HmrcOfficer extends RoleInBusiness

object RoleInBusiness {

  val stati: Map[RoleInBusiness, String] = Map[RoleInBusiness, String] (
    OwnerProprietor -> "01",
    PartnerRole -> "02",
    Director -> "03",
    CompanySecretary -> "04",
    Trustee -> "05",
    BoardMember -> "06",
    AccountantAgent -> "07",
    Representative -> "08",
    AuthorisedEmployee -> "09",
    Other -> "10",
    HmrcOfficer -> "11"
  )

  val inverseStati: Map[String, RoleInBusiness] = stati.map(_.swap)

  def fromString(value: String): RoleInBusiness = inverseStati(value)
  def toJsString(value: RoleInBusiness): JsString = JsString(stati(value))

  implicit val format: Format[RoleInBusiness] = Format[RoleInBusiness](
    Reads[RoleInBusiness] { json => json.validate[String] map fromString },
    Writes[RoleInBusiness] (toJsString)
  )

}
