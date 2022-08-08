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

package models

import models.api._
import models.submission._
import play.api.libs.json._
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate

sealed trait BusinessEntity {
  def bpSafeId: Option[String]

  def businessVerification: Option[BusinessVerificationStatus]

  def registration: BusinessRegistrationStatus

  def identifiersMatch: Boolean

  def identifiers: List[CustomerId]


  // scalastyle:off
  def idVerificationStatus: IdVerificationStatus =
    (identifiersMatch, businessVerification, registration) match {
      case (true, Some(BvPass), FailedStatus) => IdVerified
      case (true, Some(BvCtEnrolled), FailedStatus) => IdVerified
      case (true, None, FailedStatus) => IdVerified
      case (true, Some(BvFail), NotCalledStatus) => IdVerificationFailed
      case (true, Some(BvUnchallenged), NotCalledStatus) => IdVerificationFailed
      case (false, Some(BvUnchallenged), NotCalledStatus) => IdUnverifiable
      case (false, None, NotCalledStatus) => IdUnverifiable
      case (true, Some(BvUnchallenged), FailedStatus) => IdVerified
      case (true, Some(BvSaEnrolled), FailedStatus) => IdVerified
      case _ => throw new InternalServerException("[ApplicantDetailsHelper][idVerificationStatus] method called with unsupported data from incorpId")
    }
}

object BusinessEntity {
  def reads(partyType: PartyType): Reads[BusinessEntity] = Reads { json =>
    partyType match {
      case UkCompany | RegSociety | CharitableOrg =>
        Json.fromJson(json)(IncorporatedEntity.format)
      case Individual | NETP =>
        Json.fromJson(json)(SoleTraderIdEntity.format)
      case Partnership | ScotPartnership | LtdPartnership | ScotLtdPartnership | LtdLiabilityPartnership =>
        Json.fromJson(json)(PartnershipIdEntity.format)
      case UnincorpAssoc | Trust | NonUkNonEstablished =>
        Json.fromJson(json)(MinorEntity.format)
      case _ => throw new InternalServerException("Tried to parse business entity for an unsupported party type")
    }
  }

  val writes: Writes[BusinessEntity] = Writes {
    case incorporatedEntity: IncorporatedEntity =>
      Json.toJson(incorporatedEntity)(IncorporatedEntity.format)
    case soleTrader@SoleTraderIdEntity(_, _, _, _, _, _, _, _, _, _, _) =>
      Json.toJson(soleTrader)(SoleTraderIdEntity.format)
    case partnershipIdEntity@PartnershipIdEntity(_, _, _, _, _, _, _, _, _, _) =>
      Json.toJson(partnershipIdEntity)(PartnershipIdEntity.format)
    case minorEntity: MinorEntity =>
      Json.toJson(minorEntity)(MinorEntity.format)
    case entity =>
      Json.obj()
  }
}

// Entity specific types

// IncorporatedIdEntity supports UK Companies, Registered Societies and Charitable Organisations

case class IncorporatedEntity(companyName: Option[String],
                              companyNumber: String,
                              dateOfIncorporation: Option[LocalDate],
                              ctutr: Option[String] = None,
                              bpSafeId: Option[String] = None,
                              countryOfIncorporation: String = "GB",
                              businessVerification: Option[BusinessVerificationStatus],
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
      date = dateOfIncorporation
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

// SoleTraderIdEntity supports Sole Traders and NETP

case class SoleTraderIdEntity(firstName: String,
                              lastName: String,
                              dateOfBirth: LocalDate,
                              nino: Option[String] = None,
                              sautr: Option[String] = None,
                              trn: Option[String] = None,
                              bpSafeId: Option[String] = None,
                              businessVerification: Option[BusinessVerificationStatus],
                              registration: BusinessRegistrationStatus,
                              overseas: Option[OverseasIdentifierDetails] = None,
                              identifiersMatch: Boolean) extends BusinessEntity {

  override def identifiers: List[CustomerId] =
    List(
      nino.map(nino =>
        CustomerId(
          idValue = nino,
          idType = NinoIdType,
          IDsVerificationStatus = idVerificationStatus
        )),
      sautr.map(utr =>
        CustomerId(
          idValue = utr,
          idType = UtrIdType,
          IDsVerificationStatus = idVerificationStatus
        )),
      trn.map(trn =>
        CustomerId(
          idValue = trn,
          idType = TempNinoIDType,
          IDsVerificationStatus = idVerificationStatus
        )),
      overseas.map(details =>
        CustomerId(
          idType = OtherIdType,
          idValue = details.taxIdentifier,
          countryOfIncorporation = Some(details.country),
          IDsVerificationStatus = idVerificationStatus
        ))
    ).flatten

}

object SoleTraderIdEntity {
  implicit val format: Format[SoleTraderIdEntity] = Json.format[SoleTraderIdEntity]
}

case class OverseasIdentifierDetails(taxIdentifier: String, country: String)

object OverseasIdentifierDetails {
  implicit val format: Format[OverseasIdentifierDetails] = Json.format[OverseasIdentifierDetails]
}

// PartnershipIdEntity supports Partnerships, Scottish Partnerships, Ltd Partnerships, Scottish Ltd Partnerships and Limited Liability Partnerships

case class PartnershipIdEntity(sautr: Option[String],
                               companyNumber: Option[String],
                               companyName: Option[String] = None,
                               dateOfIncorporation: Option[LocalDate],
                               postCode: Option[String],
                               chrn: Option[String],
                               bpSafeId: Option[String] = None,
                               businessVerification: Option[BusinessVerificationStatus],
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
      )),
      companyNumber.map(crn => CustomerId(
        idValue = crn,
        idType = CrnIdType,
        IDsVerificationStatus = idVerificationStatus,
        date = dateOfIncorporation
      ))
    ).flatten

}

object PartnershipIdEntity {
  implicit val format: Format[PartnershipIdEntity] = Json.format[PartnershipIdEntity]
}

// MinorEntityIdEntity supports Unincorporated Associations, Trusts and Non UK Companies

case class MinorEntity(companyName: Option[String],
                       sautr: Option[String],
                       ctutr: Option[String],
                       overseas: Option[OverseasIdentifierDetails],
                       postCode: Option[String],
                       chrn: Option[String],
                       casc: Option[String],
                       registration: BusinessRegistrationStatus,
                       businessVerification: Option[BusinessVerificationStatus],
                       bpSafeId: Option[String] = None,
                       identifiersMatch: Boolean) extends BusinessEntity {

  override def identifiers: List[CustomerId] =
    List(
      sautr.map(utr => CustomerId(
        idValue = utr,
        idType = UtrIdType,
        IDsVerificationStatus = idVerificationStatus
      )),
      ctutr.map(utr => CustomerId(
        idValue = utr,
        idType = UtrIdType,
        IDsVerificationStatus = idVerificationStatus
      )),
      overseas.map(details => CustomerId(
        idType = OtherIdType,
        idValue = details.taxIdentifier,
        countryOfIncorporation = Some(details.country),
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

object MinorEntity {
  implicit val format: Format[MinorEntity] = Json.format[MinorEntity]
}