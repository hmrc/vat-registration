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
import models.api.returns.Returns
import models.registration.BusinessSectionId
import models.registration.sections.PartnersSection
import models.submission.PartyType
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate

case class VatScheme(id: String,
                     internalId: String,
                     tradingDetails: Option[TradingDetails] = None,
                     returns: Option[Returns] = None,
                     sicAndCompliance: Option[SicAndCompliance] = None,
                     businessContact: Option[BusinessContact] = None,
                     bankAccount: Option[BankAccount] = None,
                     acknowledgementReference: Option[String] = None,
                     flatRateScheme: Option[FlatRateScheme] = None,
                     status: VatRegStatus.Value,
                     eligibilityData: Option[JsObject] = None,
                     eligibilitySubmissionData: Option[EligibilitySubmissionData] = None,
                     applicantDetails: Option[ApplicantDetails] = None,
                     transactorDetails: Option[TransactorDetails] = None,
                     confirmInformationDeclaration: Option[Boolean] = None,
                     nrsSubmissionPayload: Option[String] = None,
                     partners: Option[PartnersSection] = None,
                     attachments: Option[Attachments] = None,
                     createdDate: Option[LocalDate] = None,
                     applicationReference: Option[String] = None,
                     otherBusinessInvolvements: Option[List[OtherBusinessInvolvement]] = None,
                     business: Option[Business] = None) {

  def partyType: Option[PartyType] = eligibilitySubmissionData.map(_.partyType)

}

object VatScheme {

  val exceptionKey = "2"
  val exemptionKey = "1"
  val nonExceptionOrExemptionKey = "0"
  def exceptionOrExemption(eligibilityData: EligibilitySubmissionData, returns: Returns): String = {
    (eligibilityData.appliedForException, returns.appliedForExemption) match {
      case (Some(true), Some(true)) =>
        throw new InternalServerException("User has applied for both exception and exemption")
      case (Some(true), _) => exceptionKey
      case (_, Some(true)) => exemptionKey
      case _ => nonExceptionOrExemptionKey
    }
  }

  // scalastyle:off
  def reads(crypto: Option[CryptoSCRS] = None): Reads[VatScheme] =
    (__ \ "eligibilitySubmissionData" \ "partyType").readNullable[PartyType] flatMap {
      case Some(partyType) => (
        (__ \ "registrationId").read[String] and
        (__ \ "internalId").read[String] and
        (__ \ "tradingDetails").readNullable[TradingDetails] and
        (__ \ "returns").readNullable[Returns] and
        (__ \ "sicAndCompliance").readNullable[SicAndCompliance] and
        (__ \ "businessContact").readNullable[BusinessContact] and
        (__ \ "bankAccount").readNullable[BankAccount](crypto.map(BankAccountMongoFormat.encryptedFormat).getOrElse(BankAccount.format)) and
        (__ \ "acknowledgementReference").readNullable[String] and
        (__ \ "flatRateScheme").readNullable[FlatRateScheme] and
        (__ \ "status").read[VatRegStatus.Value] and
        (__ \ "eligibilityData").readNullable[JsObject] and
        (__ \ "eligibilitySubmissionData").readNullable[EligibilitySubmissionData] and
        (__ \ "applicantDetails").readNullable[ApplicantDetails](Format[ApplicantDetails](ApplicantDetails.reads(partyType), ApplicantDetails.writes)) and
        (__ \ "transactorDetails").readNullable[TransactorDetails] and
        (__ \ "confirmInformationDeclaration").readNullable[Boolean] and
        (__ \ "nrsSubmissionPayload").readNullable[String] and
        (__ \ "partners").readNullable[PartnersSection] and
        (__ \ "attachments").readNullable[Attachments] and
        (__ \ "createdDate").readNullable[LocalDate] and
        (__ \ "applicationReference").readNullable[String] and
        (__ \ "otherBusinessInvolvements").readNullable[List[OtherBusinessInvolvement]] and
        (__ \ BusinessSectionId.repoKey).read[Business].fmap(Option[Business]).orElse(__.readNullable[Business](Business.tempReads).fmap {
          case Some(Business(None, None, None, None, None, None, None, None, None, None, None, None)) => None
          case optBusiness => optBusiness
        }) //TODO Replace with (__ \ BusinessSectionId.repoKey).readNullable[Business] when removing temp reads
        ) (VatScheme.apply _)
      case _ => (
        (__ \ "registrationId").read[String] and
        (__ \ "internalId").read[String] and
        (__ \ "tradingDetails").readNullable[TradingDetails] and
        (__ \ "returns").readNullable[Returns] and
        (__ \ "sicAndCompliance").readNullable[SicAndCompliance] and
        (__ \ "businessContact").readNullable[BusinessContact] and
        (__ \ "bankAccount").readNullable[BankAccount](crypto.map(BankAccountMongoFormat.encryptedFormat).getOrElse(BankAccount.format)) and
        (__ \ "acknowledgementReference").readNullable[String] and
        (__ \ "flatRateScheme").readNullable[FlatRateScheme] and
        (__ \ "status").read[VatRegStatus.Value] and
        (__ \ "eligibilityData").readNullable[JsObject] and
        (__ \ "eligibilitySubmissionData").readNullable[EligibilitySubmissionData] and
        Reads.pure(None) and
        (__ \ "transactorDetails").readNullable[TransactorDetails] and
        (__ \ "confirmInformationDeclaration").readNullable[Boolean] and
        (__ \ "nrsSubmissionPayload").readNullable[String] and
        (__ \ "partners").readNullable[PartnersSection] and
        (__ \ "attachments").readNullable[Attachments] and
        (__ \ "createdDate").readNullable[LocalDate] and
        (__ \ "applicationReference").readNullable[String] and
        (__ \ "otherBusinessInvolvements").readNullable[List[OtherBusinessInvolvement]] and
        (__ \ BusinessSectionId.repoKey).read[Business].fmap(Option[Business]).orElse(__.readNullable[Business](Business.tempReads).fmap {
          case Some(Business(None, None, None, None, None, None, None, None, None, None, None, None)) => None
          case optBusiness => optBusiness
        }) //TODO Replace with (__ \ BusinessSectionId.repoKey).readNullable[Business] when removing temp reads
        ) (VatScheme.apply _)
    }

  def writes(crypto: Option[CryptoSCRS] = None): OWrites[VatScheme] = (
    (__ \ "registrationId").write[String] and
    (__ \ "internalId").write[String] and
    (__ \ "tradingDetails").writeNullable[TradingDetails] and
    (__ \ "returns").writeNullable[Returns] and
    (__ \ "sicAndCompliance").writeNullable[SicAndCompliance] and
    (__ \ "businessContact").writeNullable[BusinessContact] and
    (__ \ "bankAccount").writeNullable[BankAccount](crypto.map(BankAccountMongoFormat.encryptedFormat).getOrElse(BankAccount.format)) and
    (__ \ "acknowledgementReference").writeNullable[String] and
    (__ \ "flatRateScheme").writeNullable[FlatRateScheme] and
    (__ \ "status").write[VatRegStatus.Value] and
    (__ \ "eligibilityData").writeNullable[JsObject] and
    (__ \ "eligibilitySubmissionData").writeNullable[EligibilitySubmissionData] and
    (__ \ "applicantDetails").writeNullable[ApplicantDetails] and
    (__ \ "transactorDetails").writeNullable[TransactorDetails] and
    (__ \ "confirmInformationDeclaration").writeNullable[Boolean] and
    (__ \ "nrsSubmissionPayload").writeNullable[String] and
    (__ \ "partners").writeNullable[PartnersSection] and
    (__ \ "attachments").writeNullable[Attachments] and
    (__ \ "createdDate").writeNullable[LocalDate] and
    (__ \ "applicationReference").writeNullable[String] and
    (__ \ "otherBusinessInvolvements").writeNullable[List[OtherBusinessInvolvement]] and
    (__ \ BusinessSectionId.repoKey).writeNullable[Business]
    ) (unlift(VatScheme.unapply))

  def format(crypto: Option[CryptoSCRS] = None): OFormat[VatScheme] =
    OFormat[VatScheme](reads(crypto), writes(crypto))

}
