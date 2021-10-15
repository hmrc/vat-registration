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

import models.api.VatScheme
import models.submission.{IdVerificationStatus, NETP, NonUkNonEstablished}
import models.{IncorporatedIdEntity, NonUk, SuppliesOutsideUk, Voluntary}
import play.api.libs.json.{JsString, JsValue, Json}
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
          "formBundleId" -> formBundleId,
          optional("agentReferenceNumber" -> optAgentReferenceNumber.filterNot(_ == "")),
          "messageType" -> messageType,
          "customerStatus" -> eligibilityData.customerStatus.toString,
          conditional(List(NETP, NonUkNonEstablished).contains(eligibilityData.partyType))(
            "overseasTrader" -> true
          ),
          "eoriRequested" -> tradingDetails.eoriRequested,
          "registrationReason" -> eligibilityData.registrationReason.humanReadableKey,
          optional("registrationRelevantDate" -> {
            eligibilityData.registrationReason match {
              case Voluntary | SuppliesOutsideUk => returns.startDate
              case NonUk => eligibilityData.threshold.thresholdOverseas
              case _ => Some(eligibilityData.threshold.earliestDate)
            }
          }),
          optional("corporateBodyRegistered" -> {
            applicantDetails.entity match {
              case IncorporatedIdEntity(_, _, dateOfIncorporation, _, _, countryOfIncorporation, _, _, _, _) =>
                Some(Json.obj(
                  "countryOfIncorporation" -> countryOfIncorporation,
                  "dateOfIncorporation" -> dateOfIncorporation
                ))
              case _ => None
            }
          }),
          "idsVerificationStatus" -> applicantDetails.entity.bpSafeId.fold(
            IdVerificationStatus.toJsString(applicantDetails.entity.idVerificationStatus)
          )(_ => JsString(registeredStatus)),
          "cidVerification" -> cidVerificationStatus,
          optional("businessPartnerReference" -> applicantDetails.entity.bpSafeId),
          "userEnteredDetails" -> userAnswers
        )
      case _ =>
        throw new InternalServerException(
          s"""
          [SubmissionAuditModel] Could not construct Audit JSON as required blocks are missing.

          eligibilitySubmissionData is present?   ${vatScheme.eligibilitySubmissionData.isDefined}
          tradingDetails is present?              ${vatScheme.tradingDetails.isDefined}
          applicantDetails is present?            ${vatScheme.applicantDetails.isDefined}
          returns is present?                     ${vatScheme.returns.isDefined}
        """)
    }


}
