/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time.LocalDate

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class LodgingOfficerDetails(currentAddress: Address,
                                 changeOfName: Option[FormerName],
                                 previousAddress : Option[Address],
                                 contact: DigitalContactOptional)

object LodgingOfficerDetails {
  implicit val format = (
    (__ \ "currentAddress").format[Address] and
    (__ \ "changeOfName").formatNullable[FormerName] and
    (__ \ "previousAddress").formatNullable[Address] and
    (__ \ "contact").format[DigitalContactOptional]
  )(LodgingOfficerDetails.apply, unlift(LodgingOfficerDetails.unapply))
}

case class LodgingOfficer(@deprecated("Use LodgingOfficerDetails instead", "SCRS-9379") currentAddress: Option[Address] = None,
                          dob: LocalDate,
                          nino: String,
                          role: String,
                          name: Name,
                          ivPassed: Option[Boolean],
                          @deprecated("Use LodgingOfficerDetails instead", "SCRS-9379") changeOfName: Option[ChangeOfName] = None,
                          @deprecated("Use LodgingOfficerDetails instead", "SCRS-9379") currentOrPreviousAddress : Option[CurrentOrPreviousAddress] = None,
                          @deprecated("Use LodgingOfficerDetails instead", "SCRS-9379") contact: Option[OfficerContactDetails] = None,
                          details: Option[LodgingOfficerDetails])

object LodgingOfficer extends VatLodgingOfficerValidator {
  implicit val format: OFormat[LodgingOfficer] = (
    (__ \ "currentAddress").formatNullable[Address] and
    (__ \ "dob").format[LocalDate] and
    (__ \ "nino").format[String](ninoValidator) and
    (__ \ "role").format[String](roleValidator) and
    (__ \ "name").format[Name] and
    (__ \ "ivPassed").formatNullable[Boolean] and
    (__ \ "changeOfName").formatNullable[ChangeOfName] and
    (__ \ "currentOrPreviousAddress").formatNullable[CurrentOrPreviousAddress] and
    (__ \ "contact").formatNullable[OfficerContactDetails] and
    (__ \ "details").formatNullable[LodgingOfficerDetails]
  )(LodgingOfficer.apply, unlift(LodgingOfficer.unapply))
}
