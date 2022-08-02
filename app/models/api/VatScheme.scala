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

import auth.CryptoSCRS
import enums.VatRegStatus
import models.api.vatapplication.VatApplication
import models.registration._
import models.registration.sections.PartnersSection
import models.submission.PartyType
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate

case class VatScheme(registrationId: String,
                     internalId: String,
                     createdDate: LocalDate,
                     status: VatRegStatus.Value,
                     confirmInformationDeclaration: Option[Boolean] = None,
                     applicationReference: Option[String] = None,
                     acknowledgementReference: Option[String] = None,
                     nrsSubmissionPayload: Option[String] = None,
                     eligibilityData: Option[JsObject] = None,
                     eligibilitySubmissionData: Option[EligibilitySubmissionData] = None,
                     transactorDetails: Option[TransactorDetails] = None,
                     applicantDetails: Option[ApplicantDetails] = None,
                     partners: Option[PartnersSection] = None,
                     business: Option[Business] = None,
                     otherBusinessInvolvements: Option[List[OtherBusinessInvolvement]] = None,
                     vatApplication: Option[VatApplication] = None,
                     bankAccount: Option[BankAccount] = None,
                     flatRateScheme: Option[FlatRateScheme] = None,
                     attachments: Option[Attachments] = None) {

  def partyType: Option[PartyType] = eligibilitySubmissionData.map(_.partyType)

}

object VatScheme {

  val exceptionKey = "2"
  val exemptionKey = "1"
  val nonExceptionOrExemptionKey = "0"

  def exceptionOrExemption(eligibilityData: EligibilitySubmissionData, vatApplication: VatApplication): String = {
    (eligibilityData.appliedForException, vatApplication.appliedForExemption) match {
      case (Some(true), Some(true)) =>
        throw new InternalServerException("User has applied for both exception and exemption")
      case (Some(true), _) => exceptionKey
      case (_, Some(true)) => exemptionKey
      case _ => nonExceptionOrExemptionKey
    }
  }

  // scalastyle:off
  def reads(crypto: Option[CryptoSCRS] = None): Reads[VatScheme] =
    (__ \ EligibilitySectionId.repoKey \ "partyType").readNullable[PartyType] flatMap {
      case Some(partyType) => (
        (__ \ "registrationId").read[String] and
        (__ \ "internalId").read[String] and
        (__ \ "createdDate").readWithDefault[LocalDate](LocalDate.MIN) and
        (__ \ "status").read[VatRegStatus.Value] and
        (__ \ InformationDeclarationSectionId.repoKey).readNullable[Boolean] and
        (__ \ ApplicationReferenceSectionId.repoKey).readNullable[String] and
        (__ \ "acknowledgementReference").readNullable[String] and
        (__ \ NrsSubmissionPayloadSectionId.repoKey).readNullable[String] and
        (__ \ "eligibilityData").readNullable[JsObject] and
        (__ \ EligibilitySectionId.repoKey).readNullable[EligibilitySubmissionData] and
        (__ \ TransactorSectionId.repoKey).readNullable[TransactorDetails] and
        (__ \ ApplicantSectionId.repoKey).readNullable[ApplicantDetails](ApplicantDetails.reads(partyType)) and
        (__ \ PartnersSectionId.repoKey).readNullable[PartnersSection] and
        (__ \ BusinessSectionId.repoKey).readNullable[Business] and
        (__ \ OtherBusinessInvolvementsSectionId.repoKey).readNullable[List[OtherBusinessInvolvement]] and
        (__ \ VatApplicationSectionId.repoKey).readNullable[VatApplication] and
        (__ \ BankAccountSectionId.repoKey).readNullable[BankAccount](crypto.map(BankAccountMongoFormat.encryptedFormat).getOrElse(BankAccount.format)) and
        (__ \ FlatRateSchemeSectionId.repoKey).readNullable[FlatRateScheme] and
        (__ \ AttachmentsSectionId.repoKey).readNullable[Attachments]
      ) (VatScheme.apply _)
      case _ => (
        (__ \ "registrationId").read[String] and
        (__ \ "internalId").read[String] and
        (__ \ "createdDate").readWithDefault[LocalDate](LocalDate.MIN) and
        (__ \ "status").read[VatRegStatus.Value] and
        (__ \ InformationDeclarationSectionId.repoKey).readNullable[Boolean] and
        (__ \ ApplicationReferenceSectionId.repoKey).readNullable[String] and
        (__ \ "acknowledgementReference").readNullable[String] and
        (__ \ NrsSubmissionPayloadSectionId.repoKey).readNullable[String] and
        (__ \ "eligibilityData").readNullable[JsObject] and
        (__ \ EligibilitySectionId.repoKey).readNullable[EligibilitySubmissionData] and
        (__ \ TransactorSectionId.repoKey).readNullable[TransactorDetails] and
        Reads.pure(None) and
        (__ \ PartnersSectionId.repoKey).readNullable[PartnersSection] and
        (__ \ BusinessSectionId.repoKey).readNullable[Business] and
        (__ \ OtherBusinessInvolvementsSectionId.repoKey).readNullable[List[OtherBusinessInvolvement]] and
        (__ \ VatApplicationSectionId.repoKey).readNullable[VatApplication] and
        (__ \ BankAccountSectionId.repoKey).readNullable[BankAccount](crypto.map(BankAccountMongoFormat.encryptedFormat).getOrElse(BankAccount.format)) and
        (__ \ FlatRateSchemeSectionId.repoKey).readNullable[FlatRateScheme] and
        (__ \ AttachmentsSectionId.repoKey).readNullable[Attachments]
      ) (VatScheme.apply _)
    }

  def writes(crypto: Option[CryptoSCRS] = None): OWrites[VatScheme] = (
    (__ \ "registrationId").write[String] and
    (__ \ "internalId").write[String] and
    (__ \ "createdDate").write[LocalDate] and
    (__ \ "status").write[VatRegStatus.Value] and
    (__ \ InformationDeclarationSectionId.repoKey).writeNullable[Boolean] and
    (__ \ ApplicationReferenceSectionId.repoKey).writeNullable[String] and
    (__ \ "acknowledgementReference").writeNullable[String] and
    (__ \ NrsSubmissionPayloadSectionId.repoKey).writeNullable[String] and
    (__ \ "eligibilityData").writeNullable[JsObject] and
    (__ \ EligibilitySectionId.repoKey).writeNullable[EligibilitySubmissionData] and
    (__ \ TransactorSectionId.repoKey).writeNullable[TransactorDetails] and
    (__ \ ApplicantSectionId.repoKey).writeNullable[ApplicantDetails] and
    (__ \ PartnersSectionId.repoKey).writeNullable[PartnersSection] and
    (__ \ BusinessSectionId.repoKey).writeNullable[Business] and
    (__ \ OtherBusinessInvolvementsSectionId.repoKey).writeNullable[List[OtherBusinessInvolvement]] and
    (__ \ VatApplicationSectionId.repoKey).writeNullable[VatApplication] and
    (__ \ BankAccountSectionId.repoKey).writeNullable[BankAccount](crypto.map(BankAccountMongoFormat.encryptedFormat).getOrElse(BankAccount.format)) and
    (__ \ FlatRateSchemeSectionId.repoKey).writeNullable[FlatRateScheme] and
    (__ \ AttachmentsSectionId.repoKey).writeNullable[Attachments]
  ) (unlift(VatScheme.unapply))

  def format(crypto: Option[CryptoSCRS] = None): OFormat[VatScheme] =
    OFormat[VatScheme](reads(crypto), writes(crypto))

}
