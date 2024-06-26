/*
 * Copyright 2023 HM Revenue & Customs
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

import models.BusinessEntity
import models.submission._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.JsonUtilities

case class ApplicantDetails(
  personalDetails: Option[PersonalDetails] = None,
  entity: Option[BusinessEntity] = None,
  currentAddress: Option[Address] = None,
  noPreviousAddress: Option[Boolean] = None,
  previousAddress: Option[Address] = None,
  contact: Contact = Contact(),
  changeOfName: FormerName = FormerName(),
  roleInTheBusiness: Option[RoleInTheBusiness] = None,
  otherRoleInTheBusiness: Option[String] = None
)

object ApplicantDetails extends JsonUtilities {

  def reads(partyType: PartyType): Reads[ApplicantDetails] = (
    (__ \ "personalDetails").readNullable[PersonalDetails] and
      (__ \ "entity").readNullable[BusinessEntity](BusinessEntity.reads(partyType)) and
      (__ \ "currentAddress").readNullable[Address] and
      (__ \ "noPreviousAddress").readNullable[Boolean] and
      (__ \ "previousAddress").readNullable[Address] and
      (__ \ "contact").readWithDefault[Contact](Contact()) and
      (__ \ "changeOfName").readWithDefault[FormerName](FormerName()) and
      (__ \ "roleInTheBusiness").readNullable[RoleInTheBusiness] and
      (__ \ "otherRoleInTheBusiness").readNullable[String]
  )(ApplicantDetails.apply _)

  implicit val writes: Writes[ApplicantDetails] = (
    (__ \ "personalDetails").writeNullable[PersonalDetails] and
      (__ \ "entity").writeNullable[BusinessEntity] and
      (__ \ "currentAddress").writeNullable[Address] and
      (__ \ "noPreviousAddress").writeNullable[Boolean] and
      (__ \ "previousAddress").writeNullable[Address] and
      (__ \ "contact").write[Contact] and
      (__ \ "changeOfName").write[FormerName] and
      (__ \ "roleInTheBusiness").writeNullable[RoleInTheBusiness] and
      (__ \ "otherRoleInTheBusiness").writeNullable[String]
  )(unlift(ApplicantDetails.unapply))

}
