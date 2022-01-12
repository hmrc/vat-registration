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

import models.BusinessEntity
import models.registration.RegistrationSection
import models.submission._
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.JsonUtilities

case class ApplicantDetails(personalDetails: PersonalDetails,
                            entity: BusinessEntity,
                            currentAddress: Address,
                            previousAddress: Option[Address] = None,
                            contact: DigitalContactOptional,
                            changeOfName: Option[FormerName] = None,
                            roleInBusiness: RoleInTheBusiness) extends RegistrationSection[ApplicantDetails] {

  override def isComplete: ApplicantDetails => Boolean = {
    _ => true
  }

}

object ApplicantDetails extends VatApplicantDetailsValidator
  with JsonUtilities {

  //TODO Remove transactor .orElse a few weeks after this commit goes live as it's there just to support old journeys
  def reads(partyType: PartyType): Reads[ApplicantDetails] = (
    (__ \ "personalDetails").read[PersonalDetails].orElse((__ \ "transactor").read[PersonalDetails]) and
      (__ \ "entity").read[BusinessEntity](BusinessEntity.reads(partyType)) and
      (__ \ "currentAddress").read[Address] and
      (__ \ "previousAddress").readNullable[Address] and
      (__ \ "contact").read[DigitalContactOptional] and
      (__ \ "changeOfName").readNullable[FormerName] and
      (__ \ "roleInTheBusiness").read[RoleInTheBusiness]
    ) (ApplicantDetails.apply _)

  implicit val writes: Writes[ApplicantDetails] = (
    (__ \ "personalDetails").write[PersonalDetails] and
      (__ \ "entity").write[BusinessEntity](BusinessEntity.writes) and
      (__ \ "currentAddress").write[Address] and
      (__ \ "previousAddress").writeNullable[Address] and
      (__ \ "contact").write[DigitalContactOptional] and
      (__ \ "changeOfName").writeNullable[FormerName] and
      (__ \ "roleInTheBusiness").write[RoleInTheBusiness]
    ) (unlift(ApplicantDetails.unapply))

}
