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

package models.submission

import models.api._
import models.api.returns.Returns
import play.api.libs.json._
import utils.JsonUtilities

case class VatSubmission(messageType: String = "SubscriptionCreate",
                         tradersPartyType: Option[PartyType],
                         confirmInformationDeclaration: Option[Boolean],
                         companyRegistrationNumber: Option[String],
                         applicantDetails: ApplicantDetails,
                         bankDetails: Option[BankAccount],
                         sicAndCompliance: SicAndCompliance,
                         businessContact: BusinessContact,
                         tradingDetails: TradingDetails,
                         flatRateScheme: Option[FRSDetails],
                         eligibilitySubmissionData: EligibilitySubmissionData,
                         returns: Returns)

object VatSubmission extends JsonUtilities {

  implicit val mongoFormat: OFormat[VatSubmission] = Json.format[VatSubmission]

  private def missingSection(section: String) = throw new IllegalStateException(s"VAT scheme missing $section section")

  def fromVatScheme(scheme: VatScheme): VatSubmission =
    VatSubmission(
      tradersPartyType = Some(UkCompany),
      confirmInformationDeclaration = scheme.confirmInformationDeclaration,
      companyRegistrationNumber = Some("CRN"),
      applicantDetails = scheme.applicantDetails.getOrElse(missingSection("ApplicantDetails")),
      bankDetails = scheme.bankAccount,
      sicAndCompliance = scheme.sicAndCompliance.getOrElse(missingSection("SIC and Compliance")),
      businessContact = scheme.businessContact.getOrElse(missingSection("Business contact")),
      tradingDetails = scheme.tradingDetails.getOrElse(missingSection("Trading details")),
      flatRateScheme = scheme.flatRateScheme.flatMap(frs => if (frs.joinFrs) frs.frsDetails else None),
      eligibilitySubmissionData = scheme.eligibilitySubmissionData.getOrElse(missingSection("Eligibility")),
      returns = scheme.returns.getOrElse(missingSection("Returns"))
    )

}
