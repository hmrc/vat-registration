/*
 * Copyright 2023 HM Revenue & Customs
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

import models._
import models.api.VatScheme
import models.submission.{IdVerificationStatus, NETP, NonUkNonEstablished}
import play.api.libs.json.{JsString, JsValue}
import services.monitoring.AuditModel
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

case class SubmissionAuditModel(userAnswers: JsValue,
                                vatScheme: VatScheme,
                                authProviderId: String,
                                affinityGroup: AffinityGroup,
                                optAgentReferenceNumber: Option[String],
                                formBundleId: String) extends AuditModel {

  private val messageType = "SubscriptionCreate"
  private val registeredStatus = "0"
  private val cidVerificationStatus = "1"
  private val MTDfB = "2"

  override val auditType: String = "SubscriptionSubmitted"
  override val transactionName: String = "subscription-submitted"

  override val detail: JsValue =
    (vatScheme.eligibilitySubmissionData, vatScheme.applicantDetails, vatScheme.vatApplication) match {
      case (Some(eligibilityData), Some(applicantDetails), Some(vatApplication)) =>
        jsonObject(
          "authProviderId" -> authProviderId,
          "journeyId" -> vatScheme.registrationId,
          "userType" -> affinityGroup.toString,
          "formBundleId" -> formBundleId,
          optional("agentReferenceNumber" -> optAgentReferenceNumber.filterNot(_ == "")),
          "messageType" -> messageType,
          "customerStatus" -> MTDfB,
          conditional(List(NETP, NonUkNonEstablished).contains(eligibilityData.partyType))(
            "overseasTrader" -> true
          ),
          "eoriRequested" -> vatApplication.eoriRequested,
          "registrationReason" -> {
            if (vatApplication.currentlyTrading.contains(false) && eligibilityData.registrationReason.equals(Voluntary)) IntendingTrader.humanReadableKey
            else eligibilityData.registrationReason.humanReadableKey
          },
          optional("registrationRelevantDate" -> {
            eligibilityData.registrationReason match {
              case Voluntary | SuppliesOutsideUk | GroupRegistration | IntendingTrader => vatApplication.startDate
              case NonUk => eligibilityData.threshold.thresholdOverseas
              case TransferOfAGoingConcern => eligibilityData.togcCole.map(_.dateOfTransfer)
              case _ => Some(eligibilityData.threshold.earliestDate)
            }
          }),
          optional("corporateBodyRegistered" -> {
            applicantDetails.entity match {
              case Some(IncorporatedEntity(_, _, dateOfIncorporation, _, _, countryOfIncorporation, _, _, _, _)) =>
                Some(jsonObject(
                  "countryOfIncorporation" -> countryOfIncorporation,
                  optional("dateOfIncorporation" -> dateOfIncorporation)
                ))
              case _ => None
            }
          }),
          "idsVerificationStatus" -> applicantDetails.entity.map(entity => entity.bpSafeId.fold(
            IdVerificationStatus.toJsString(entity.idVerificationStatus)
          )(_ => JsString(registeredStatus))),
          "cidVerification" -> cidVerificationStatus,
          optional("businessPartnerReference" -> applicantDetails.entity.flatMap(_.bpSafeId)),
          "userEnteredDetails" -> userAnswers
        )
      case _ =>
        throw new InternalServerException(
          s"""
          [SubmissionAuditModel] Could not construct Audit JSON as required blocks are missing.

          eligibilitySubmissionData is present?   ${vatScheme.eligibilitySubmissionData.isDefined}
          applicantDetails is present?            ${vatScheme.applicantDetails.isDefined}
          vatApplication is present?              ${vatScheme.vatApplication.isDefined}
        """)
    }


}
