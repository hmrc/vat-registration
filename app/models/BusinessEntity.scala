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
      case UkCompany | RegSociety | CharitableOrg => Json.fromJson(json)(IncorporatedIdEntity.format)
      case Individual | NETP => Json.fromJson(json)(SoleTraderIdEntity.format)
      case Partnership => Json.fromJson(json)(PartnershipIdEntity.format)
      case UnincorpAssoc | Trust => Json.fromJson(json)(BusinessIdEntity.format)
      case _ => throw new InternalServerException("Tried to parse business entity for an unsupported party type")
    }
  }

  val writes: Writes[BusinessEntity] = Writes {
    case incorporatedEntity: IncorporatedIdEntity =>
      Json.toJson(incorporatedEntity)(IncorporatedIdEntity.format)
    case soleTrader@SoleTraderIdEntity(_, _, _, _, _, _, _, _, _, _, _) =>
      Json.toJson(soleTrader)(SoleTraderIdEntity.format)
    case partnershipIdEntity@PartnershipIdEntity(_, _, _, _, _, _, _) =>
      Json.toJson(partnershipIdEntity)(PartnershipIdEntity.format)
    case businessIdEntity: BusinessIdEntity =>
      Json.toJson(businessIdEntity)(BusinessIdEntity.format)
    case entity =>
      Json.obj()
  }
}

// Entity specific types

// IncorporatedIdEntity supports UK Companies, Registered Societies and Charitable Organisations

case class IncorporatedIdEntity(companyName: String,
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

object IncorporatedIdEntity {
  implicit val format: Format[IncorporatedIdEntity] = Json.format[IncorporatedIdEntity]
}

// SoleTraderIdEntity supports Sole Traders and NETP

case class SoleTraderIdEntity(firstName: String,
                              lastName: String,
                              dateOfBirth: LocalDate,
                              nino: Option[String] = None,
                              sautr: Option[String] = None,
                              trn: Option[String] = None,
                              bpSafeId: Option[String] = None,
                              businessVerification: BusinessVerificationStatus,
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

// PartnershipIdEntity supports Partnerships

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

// BusinessIdEntity supports Unincorporated Associations and Trusts

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