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

import models.BusinessEntity
import models.submission.{CustomerId, IdVerified, NinoIdType, RoleInBusiness}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import utils.JsonUtilities

case class ApplicantDetails(transactor: TransactorDetails,
                            entity: BusinessEntity,
                            currentAddress: Address,
                            previousAddress: Option[Address] = None,
                            contact: DigitalContactOptional,
                            changeOfName: Option[FormerName] = None,
                            roleInBusiness: RoleInBusiness) {

  def personalIdentifiers: List[CustomerId] =
    List(CustomerId(transactor.nino, NinoIdType, IdVerified, date = Some(transactor.dateOfBirth)))

}

object ApplicantDetails extends VatApplicantDetailsValidator
  with JsonUtilities {

  implicit val format: Format[ApplicantDetails] = (
    (__ \ "transactor").format[TransactorDetails] and
      (__ \ "entity").format[BusinessEntity] and
      (__ \ "currentAddress").format[Address] and
      (__ \ "previousAddress").formatNullable[Address] and
      (__ \ "contact").format[DigitalContactOptional] and
      (__ \ "changeOfName").formatNullable[FormerName] and
      (__ \ "roleInTheBusiness").format[RoleInBusiness]
    ) (ApplicantDetails.apply, unlift(ApplicantDetails.unapply))

}
