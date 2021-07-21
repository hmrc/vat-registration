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

import models.{IncorporatedEntity, SoleTrader}
import models.api.{EligibilitySubmissionData, VatScheme}
import play.api.libs.json.{JsValue, Json}
import services.monitoring.AuditModel
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

case class SubmissionAuditModel(userAnswers: JsValue,
                                vatScheme: VatScheme,
                                authProviderId: String,
                                affinityGroup: AffinityGroup,
                                optAgentReferenceNumber: Option[String]) extends AuditModel {

  private val messageType = "SubscriptionCreate"
  private val idsVerificationStatus: String = "1"
  private val cidVerificationStatus: String = "1"

  override val auditType: String = "SubscriptionSubmitted"
  override val transactionName: String = "subscription-submitted"

  override val detail: JsValue =
    (vatScheme.eligibilitySubmissionData, vatScheme.tradingDetails, vatScheme.applicantDetails, vatScheme.returns) match {
      case (Some(eligibilityData), Some(tradingDetails), Some(applicantDetails), Some(returns)) =>
        jsonObject(
          "authProviderId" -> authProviderId,
          "journeyId" -> vatScheme.id,
          "userType" -> affinityGroup.toString,
          optional("agentReferenceNumber" -> optAgentReferenceNumber.filterNot(_ == "")),
          "messageType" -> messageType,
          "customerStatus" -> eligibilityData.customerStatus.toString,
          "eoriRequested" -> tradingDetails.eoriRequested,
          "registrationReason" -> eligibilityData.reasonForRegistration(humanReadable = true),
          optional("registrationRelevantDate" -> {
            if (eligibilityData.reasonForRegistration() == EligibilitySubmissionData.voluntaryKey) {
              returns.startDate
            } else {
              Some(eligibilityData.earliestDate)
            }
          }),
          optional("corporateBodyRegistered" -> {
            applicantDetails.entity match {
              case IncorporatedEntity(_, _, dateOfIncorporation, _, _, countryOfIncorporation, _, _, _, _) =>
                Some(Json.obj(
                  "countryOfIncorporation" -> countryOfIncorporation,
                  "dateOfIncorporation" -> dateOfIncorporation
                ))
              case _ => None
            }
          }),
          "idsVerificationStatus" -> idsVerificationStatus,
          "cidVerification" -> cidVerificationStatus,
          optional("businessPartnerReference" -> applicantDetails.entity.bpSafeId),
          "userEnteredDetails" -> userAnswers
        )
      case _ =>
        throw new InternalServerException(s"""
          [SubmissionAuditModel] Could not construct Audit JSON as required blocks are missing.

          eligibilitySubmissionData is present?   ${vatScheme.eligibilitySubmissionData.isDefined}
          tradingDetails is present?              ${vatScheme.tradingDetails.isDefined}
          applicantDetails is present?            ${vatScheme.applicantDetails.isDefined}
          returns is present?                     ${vatScheme.returns.isDefined}
        """)
    }


}
