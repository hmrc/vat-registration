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

package models

import models.api._
import models.submission._
import play.api.libs.json._
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate

sealed trait BusinessEntity {
  def bpSafeId: Option[String]
  def businessVerification: BusinessVerificationStatus
  def registration: BusinessRegistrationStatus
  def identifiersMatch: Boolean
  def identifiers: List[CustomerId]

  def idVerificationStatus: IdVerificationStatus =
    (identifiersMatch, businessVerification, registration) match {
      case (true, BvPass, FailedStatus) => IdVerified
      case (true, BvCtEnrolled, FailedStatus) => IdVerified
      case (true, BvFail, NotCalledStatus) => IdVerificationFailed
      case (true, BvUnchallenged, NotCalledStatus) => IdVerificationFailed
      case (false, BvUnchallenged, NotCalledStatus) => IdUnverifiable
      case _ => throw new InternalServerException("[ApplicantDetailsHelper][idVerificationStatus] method called with unsupported data from incorpId")
    }
}

object BusinessEntity {
  implicit val format: Format[BusinessEntity] = Json.format[BusinessEntity]
}

// Entity specific types

case class LimitedCompany(companyName: String,
                          companyNumber: String,
                          dateOfIncorporation: LocalDate,
                          ctutr: String,
                          bpSafeId: Option[String] = None,
                          countryOfIncorporation: String = "GB",
                          businessVerification: BusinessVerificationStatus,
                          registration: BusinessRegistrationStatus,
                          identifiersMatch: Boolean) extends BusinessEntity {

  override def identifiers: List[CustomerId] = List(
    CustomerId(
      idValue = ctutr,
      idType = UtrIdType,
      IDsVerificationStatus = idVerificationStatus
    ),
    CustomerId(
      idValue = companyNumber,
      idType = CrnIdType,
      IDsVerificationStatus = idVerificationStatus,
      date = Some(dateOfIncorporation)
    )
  )

}

object LimitedCompany {
  implicit val format: Format[LimitedCompany] = Json.format[LimitedCompany]
}


case class SoleTrader(firstName: String,
                      lastName: String,
                      dateOfBirth: LocalDate,
                      nino: String,
                      sautr: Option[String] = None,
                      bpSafeId: Option[String] = None,
                      businessVerification: BusinessVerificationStatus,
                      registration: BusinessRegistrationStatus,
                      identifiersMatch: Boolean) extends BusinessEntity {

  override def identifiers: List[CustomerId] =
    List(sautr.map(utr =>
      CustomerId(
        idValue = utr,
        idType = UtrIdType,
        IDsVerificationStatus = idVerificationStatus
      )
    )).flatten

}

object SoleTrader {
  implicit val format: Format[SoleTrader] = Json.format[SoleTrader]
}

case class GeneralPartnership(sautr: Option[String],
                              postCode: Option[String],
                              bpSafeId: Option[String] = None,
                              businessVerification: BusinessVerificationStatus,
                              registration: BusinessRegistrationStatus,
                              identifiersMatch: Boolean) extends BusinessEntity {

  override def identifiers: List[CustomerId] =
    List(sautr.map(utr =>
      CustomerId(
        idValue = utr,
        idType = UtrIdType,
        IDsVerificationStatus = idVerificationStatus
      )
    )).flatten

}

object GeneralPartnership {
  implicit val format: Format[GeneralPartnership] = Json.format[GeneralPartnership]
}