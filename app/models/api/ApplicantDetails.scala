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

import helpers.ApplicantDetailsHelper
import models.submission.{DateOfBirth, RoleInBusiness}
import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

case class ApplicantDetails(nino: String,
                            roleInBusiness: RoleInBusiness,
                            name: Name,
                            dateOfBirth: DateOfBirth,
                            companyName: String,
                            companyNumber: String,
                            dateOfIncorporation: LocalDate,
                            ctutr: String,
                            businessVerification: BusinessVerificationStatus,
                            registration: BusinessRegistrationStatus,
                            identifiersMatch: Boolean,
                            bpSafeId: Option[String] = None,
                            currentAddress: Address,
                            contact: DigitalContactOptional,
                            changeOfName: Option[FormerName] = None,
                            previousAddress: Option[Address] = None,
                            countryOfIncorporation: String = "GB")

object ApplicantDetails extends VatApplicantDetailsValidator
  with ApplicantDetailsHelper {

  implicit val format: Format[ApplicantDetails] = (
    (__ \ "nino").format[String] and
      (__ \ "roleInTheBusiness").format[RoleInBusiness] and
      (__ \ "name").format[Name] and
      (__ \ "dateOfBirth").format[DateOfBirth] and
      (__ \ "companyName").format[String] and
      (__ \ "companyNumber").format[String] and
      (__ \ "dateOfIncorporation").format[LocalDate] and
      (__ \ "ctutr").format[String] and
      (__ \ "businessVerification").format[BusinessVerificationStatus] and
      (__ \ "registration").format[BusinessRegistrationStatus] and
      (__ \ "identifiersMatch").format[Boolean] and
      (__ \ "bpSafeId").formatNullable[String] and
      (__ \ "currentAddress").format[Address] and
      (__ \ "contact").format[DigitalContactOptional] and
      (__ \ "changeOfName").formatNullable[FormerName] and
      (__ \ "previousAddress").formatNullable[Address] and
      (__ \ "countryOfIncorporation").format[String]
    ) (ApplicantDetails.apply, unlift(ApplicantDetails.unapply))
}
