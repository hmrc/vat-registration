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

package models.monitoring

import models.api._
import models.submission.VatSubmission
import play.api.libs.json.{JsObject, JsValue, Json}
import services.monitoring.AuditModel
import uk.gov.hmrc.auth.core.AffinityGroup
import utils.JsonUtilities

import java.time.LocalDate

object RegistrationSubmissionAuditing {
  val vatRegTransactionName = "subscription-submitted"
  val vatRegAuditType = "SubscriptionSubmitted"

  case class RegistrationSubmissionAuditModel(vatSubmission: VatSubmission,
                                              regId: String,
                                              authProviderId: String,
                                              affinityGroup: AffinityGroup,
                                              optAgentReferenceNumber: Option[String]) extends AuditModel with JsonUtilities {

    override val transactionName: String = vatRegTransactionName
    override val auditType: String = vatRegAuditType

    override val detail: JsValue = formatRegReason(vatSubmission.eligibilitySubmissionData.threshold).deepMerge(Json.obj(
      "authProviderId" -> authProviderId,
      "journeyId" -> regId,
      "userType" -> affinityGroup.toString,
      "agentReferenceNumber" -> optAgentReferenceNumber.filterNot(_ == ""),
      "messageType" -> vatSubmission.messageType,
      "customerStatus" -> vatSubmission.eligibilitySubmissionData.customerStatus.toString,
      "eoriRequested" -> vatSubmission.tradingDetails.eoriRequested,
      "corporateBodyRegistered" -> Json.obj(
        "dateOfIncorporation" -> vatSubmission.applicantDetails.dateOfIncorporation,
        "countryOfIncorporation" -> vatSubmission.applicantDetails.countryOfIncorporation
      ),
      "idsVerificationStatus" -> "1",
      "cidVerification" -> "1",
      "businessPartnerReference" -> vatSubmission.applicantDetails.bpSafeId,
      "userEnteredDetails" -> Json.obj(
        "outsideEUSales" -> vatSubmission.tradingDetails.eoriRequested,
        "customerIdentification" -> Json.obj(
          "tradersPartyType" -> vatSubmission.tradersPartyType.map(_.toString),
          "identifiers" -> Json.obj(
            "companyRegistrationNumber" -> vatSubmission.applicantDetails.companyNumber,
            "ctUTR" -> vatSubmission.applicantDetails.ctutr
          ),
          "shortOrgName" -> vatSubmission.applicantDetails.companyName,
          "dateOfBirth" -> vatSubmission.applicantDetails.dateOfBirth,
          "tradingName" -> vatSubmission.tradingDetails.tradingName
        ),
        "businessContact" -> Json.obj(
          "address" -> Json.toJson(vatSubmission.businessContact.ppob)(Address.auditFormat),
          "businessCommunicationDetails" -> Json.obj(
            "telephone" -> vatSubmission.businessContact.digitalContact.tel,
            "emailAddress" -> vatSubmission.businessContact.digitalContact.email,
            "emailVerified" -> true,
            "website" -> vatSubmission.businessContact.website,
            "preference" -> vatSubmission.businessContact.commsPreference
          )
        ),
        "subscription" -> Json.obj(
          "overThresholdIn12MonthDate" -> vatSubmission.eligibilitySubmissionData.threshold.thresholdInTwelveMonths,
          "overThresholdInPreviousMonthDate" -> vatSubmission.eligibilitySubmissionData.threshold.thresholdPreviousThirtyDays,
          "overThresholdInNextMonthDate" -> vatSubmission.eligibilitySubmissionData.threshold.thresholdNextThirtyDays,
          "reasonForSubscription" -> Json.obj(
            "voluntaryOrEarlierDate" -> vatSubmission.returns.startDate,
            "exemptionOrException" -> vatSubmission.eligibilitySubmissionData.exceptionOrExemption
          ),
          "businessActivities" -> Json.obj(
            "description" -> vatSubmission.sicAndCompliance.businessDescription,
            "sicCodes" -> (Json.obj(
              "primaryMainCode" -> vatSubmission.sicAndCompliance.mainBusinessActivity.id
            ) ++ Json.toJson(vatSubmission.sicAndCompliance.otherBusinessActivities)(SicCode.sicCodeListWrites).as[JsObject])
          ),
          "turnoverEstimates" -> Json.obj(
            "turnoverNext12Months" -> vatSubmission.eligibilitySubmissionData.estimates.turnoverEstimate,
            "zeroRatedSupplies" -> vatSubmission.returns.zeroRatedSupplies,
            "vatRepaymentExpected" -> vatSubmission.returns.reclaimVatOnMostReturns
          ),
          "frsScheme" -> vatSubmission.flatRateScheme.map(Json.toJson(_)(FRSDetails.auditWrites))
        ),
        "periods" -> Json.obj(
          "customerPreferredPeriodicity" -> vatSubmission.returns.returnsFrequency
        ),
        "bankDetails" -> vatSubmission.bankDetails.map {
          case BankAccount(true, Some(details), _) => Json.obj(
            "accountName" -> details.name,
            "sortCode" -> details.sortCode,
            "accountNumber" -> details.number
          )
          case BankAccount(false, _, Some(reason)) => Json.obj(
            "reasonBankAccNotProvided" -> NoUKBankAccount.reasonId(reason)
          )
        },
        "compliance" -> vatSubmission.sicAndCompliance.labourCompliance.map(Json.toJson(_)),
        "declaration" -> Json.obj(
          "applicant" -> Json.obj(
            "roleInBusiness" -> Json.toJson(vatSubmission.applicantDetails.roleInBusiness),
            "otherRole" -> "None",
            "name" -> Json.toJson(vatSubmission.applicantDetails.name)(Name.auditWrites),
            "previousName" -> vatSubmission.applicantDetails.changeOfName.map(Json.toJson(_)(FormerName.auditWrites)),
            "currentAddress" -> Json.toJson(vatSubmission.applicantDetails.currentAddress)(Address.auditFormat),
            "communicationDetails" -> Json.obj(
              "telephone" -> vatSubmission.applicantDetails.contact.tel,
              "emailAddress" -> vatSubmission.applicantDetails.contact.email
            ),
            "dateOfBirth" -> Json.toJson(vatSubmission.applicantDetails.dateOfBirth),
            "identifiers" -> Json.obj(
              "nationalInsuranceNumber" -> vatSubmission.applicantDetails.nino
            ),
            "previousAddress" -> vatSubmission.applicantDetails.previousAddress.map(Json.toJson(_)(Address.auditFormat)),
            "declarationSigning" -> Json.obj(
              "confirmInformationDeclaration" -> vatSubmission.confirmInformationDeclaration,
              "declarationCapacity" -> Json.toJson(vatSubmission.applicantDetails.roleInBusiness)
            )
          )
        )
      )
    )).filterNullFields
  }

  def formatRegReason(threshold: Threshold): JsObject =
    threshold match {
      case Threshold(false, _, _, _) => Json.obj("registrationReason" -> "Voluntary")
      case Threshold(true, forwardLook1, backwardLook, forwardLook2) =>
        val earliestDate = Seq(
          forwardLook1,
          backwardLook.map(_.withDayOfMonth(1).plusMonths(2)),
          forwardLook2
        ).flatten.min(Ordering.by((date: LocalDate) => date.toEpochDay))

        if (forwardLook1.contains(earliestDate) || forwardLook2.contains(earliestDate)) {
          Json.obj(
            "registrationReason" -> "Forward Look",
            "registrationRelevantDate" -> earliestDate
          )
        }
        else {
          Json.obj(
            "registrationReason" -> "Backward Look",
            "registrationRelevantDate" -> earliestDate
          )
        }
    }
}
