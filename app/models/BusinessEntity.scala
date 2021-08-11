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
  def reads(partyType: PartyType): Reads[BusinessEntity] = Reads { json =>
    partyType match {
      case UkCompany | RegSociety | CharitableOrg => Json.fromJson(json)(IncorporatedEntity.format)
      case Individual => Json.fromJson(json)(SoleTrader.format)
      case Partnership => Json.fromJson(json)(PartnershipIdEntity.format)
      case UnincorpAssoc | Trust => Json.fromJson(json)(BusinessIdEntity.format)
      case _ => throw new InternalServerException("Tried to parse business entity for an unsupported party type")
    }
  }
  val writes: Writes[BusinessEntity] = Writes {
    case incorporatedEntity: IncorporatedEntity =>
      Json.toJson(incorporatedEntity)(IncorporatedEntity.format)
    case soleTrader@SoleTrader(_, _, _, _, _, _, _, _, _) =>
      Json.toJson(soleTrader)(SoleTrader.format)
    case partnershipIdEntity@PartnershipIdEntity(_, _, _, _, _, _, _) =>
      Json.toJson(partnershipIdEntity)(PartnershipIdEntity.format)
    case businessIdEntity: BusinessIdEntity =>
      Json.toJson(businessIdEntity)(BusinessIdEntity.format)
    case entity =>
      Json.obj()
  }
}

// Entity specific types

case class IncorporatedEntity(companyName: String,
                              companyNumber: String,
                              dateOfIncorporation: LocalDate,
                              ctutr: Option[String] = None,
                              bpSafeId: Option[String] = None,
                              countryOfIncorporation: String = "GB",
                              businessVerification: BusinessVerificationStatus,
                              registration: BusinessRegistrationStatus,
                              identifiersMatch: Boolean,
                              chrn: Option[String] = None) extends BusinessEntity {

  override def identifiers: List[CustomerId] = List(
    ctutr.map(utr =>
      CustomerId(
        idValue = utr,
        idType = UtrIdType,
        IDsVerificationStatus = idVerificationStatus
      )),
    Some(CustomerId(
      idValue = companyNumber,
      idType = CrnIdType,
      IDsVerificationStatus = idVerificationStatus,
      date = Some(dateOfIncorporation)
    )),
    chrn.map(chrn =>
      CustomerId(
        idValue = chrn,
        idType = CharityRefIdType,
        IDsVerificationStatus = idVerificationStatus
      )
    )
  ).flatten

}

object IncorporatedEntity {
  implicit val format: Format[IncorporatedEntity] = Json.format[IncorporatedEntity]
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
    List(
      Some(CustomerId(
        idValue = nino,
        idType = NinoIdType,
        IDsVerificationStatus = idVerificationStatus
      )),
      sautr.map(utr =>
        CustomerId(
          idValue = utr,
          idType = UtrIdType,
          IDsVerificationStatus = idVerificationStatus
        ))
    ).flatten

}

object SoleTrader {
  implicit val format: Format[SoleTrader] = Json.format[SoleTrader]
}

case class PartnershipIdEntity(sautr: Option[String],
                               postCode: Option[String],
                               chrn: Option[String],
                               bpSafeId: Option[String] = None,
                               businessVerification: BusinessVerificationStatus,
                               registration: BusinessRegistrationStatus,
                               identifiersMatch: Boolean) extends BusinessEntity {

  override def identifiers: List[CustomerId] =
    List(
      sautr.map(utr => CustomerId(
        idValue = utr,
        idType = UtrIdType,
        IDsVerificationStatus = idVerificationStatus
      )),
      chrn.map(chrn => CustomerId(
        idValue = chrn,
        idType = CharityRefIdType,
        IDsVerificationStatus = idVerificationStatus
      ))
    ).flatten

}

object PartnershipIdEntity {
  implicit val format: Format[PartnershipIdEntity] = Json.format[PartnershipIdEntity]
}

case class BusinessIdEntity(sautr: Option[String],
                            postCode: Option[String],
                            chrn: Option[String],
                            casc: Option[String],
                            registration: BusinessRegistrationStatus,
                            businessVerification: BusinessVerificationStatus,
                            bpSafeId: Option[String] = None,
                            identifiersMatch: Boolean) extends BusinessEntity {

  override def identifiers: List[CustomerId] =
    List(
      sautr.map(utr => CustomerId(
        idValue = utr,
        idType = UtrIdType,
        IDsVerificationStatus = idVerificationStatus
      )),
      chrn.map(chrn => CustomerId(
        idValue = chrn,
        idType = CharityRefIdType,
        IDsVerificationStatus = idVerificationStatus
      )),
      casc.map(casc => CustomerId(
        idValue = casc,
        idType = CascIdType,
        IDsVerificationStatus = idVerificationStatus
      ))
    ).flatten
}

object BusinessIdEntity {
  implicit val format: Format[BusinessIdEntity] = Json.format[BusinessIdEntity]
}