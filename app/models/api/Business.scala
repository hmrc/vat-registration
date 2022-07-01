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

import play.api.libs.functional.syntax._
import play.api.libs.json.{Json, OFormat, Reads, __}

case class Business(ppobAddress: Option[Address],
                    email: Option[String],
                    telephoneNumber: Option[String],
                    hasWebsite: Option[Boolean],
                    website: Option[String],
                    contactPreference: Option[ContactPreference],
                    hasLandAndProperty: Option[Boolean],
                    businessDescription: Option[String],
                    businessActivities: Option[List[SicCode]],
                    mainBusinessActivity: Option[SicCode],
                    labourCompliance: Option[ComplianceLabour],
                    otherBusinessInvolvement: Option[Boolean]) {

  lazy val otherBusinessActivities: List[SicCode] =
    businessActivities.getOrElse(Nil).diff(mainBusinessActivity.toList)
}


object Business {

  val tempReads: Reads[Business] = (
    (__ \ "businessContact" \ "ppob").readNullable[Address] and
      (__ \ "businessContact" \ "email").readNullable[String] and
      (__ \ "businessContact" \ "telephoneNumber").readNullable[String] and
      (__ \ "businessContact" \ "hasWebsite").readNullable[Boolean] and
      (__ \ "businessContact" \ "website").readNullable[String] and
      (__ \ "businessContact" \ "contactPreference").readNullable[ContactPreference] and
      (__ \ "sicAndCompliance" \ "hasLandAndProperty").readNullable[Boolean] and
      (__ \ "sicAndCompliance" \ "businessDescription").readNullable[String] and
      (__ \ "sicAndCompliance" \ "businessActivities").readNullable[List[SicCode]] and
      (__ \ "sicAndCompliance" \ "mainBusinessActivity").readNullable[SicCode] and
      (__ \ "sicAndCompliance" \ "labourCompliance").readNullable[ComplianceLabour] and
      (__ \ "sicAndCompliance" \ "otherBusinessInvolvement").readNullable[Boolean]
    ) (Business.apply _)

  implicit val format: OFormat[Business] = Json.format[Business]

}