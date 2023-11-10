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

package models.sdes

import models.sdes.PropertyExtractor._
import play.api.libs.json.JsValue
import services.monitoring.AuditModel
import utils.JsonUtils._

object SdesAuditing {

  case class SdesFileSubmissionAudit(sdesPayload: SdesNotification, response: SdesNotificationResult, providerId: String) extends AuditModel {
    val formBundleId = sdesPayload.file.getPropertyValue(formBundleKey)
    override val auditType: String = "SDESFileSubmission"
    override val transactionName: String = "SDESFileSubmission"
    override val detail: JsValue = jsonObject(
      "authProviderId" -> providerId,
      "informationType" -> sdesPayload.informationType,
      "file" -> jsonObject(
        "recipientOrSender" -> sdesPayload.file.recipientOrSender,
        "name" -> sdesPayload.file.name,
        "location" -> sdesPayload.file.location,
        "checksum" -> sdesPayload.file.checksum,
        "size" -> sdesPayload.file.size,
        "mimeType" -> sdesPayload.file.getPropertyValue(mimeTypeKey),
        "prefixedFormBundleId" -> sdesPayload.file.getPropertyValue(prefixedFormBundleKey),
        "formBundleId" -> formBundleId,
        optional("nrsSubmissionId" -> sdesPayload.file.getPropertyValue(nrsSubmissionKey)),
        "attachmentId" -> sdesPayload.file.getPropertyValue(attachmentReferenceKey),
        "submissionDate" -> sdesPayload.file.getPropertyValue(submissionDateKey),
        "audit" -> sdesPayload.audit
      ),
      "response" -> jsonObject(
        "statusCode" -> response.status,
        "message" -> response.body
      )
    )
  }

  case class SdesCallbackFailureAudit(callback: SdesCallback) extends AuditModel {
    override val auditType: String = "SDESCallBackFailure"
    override val transactionName: String = "sdes-callback-failure"
    override val detail: JsValue = jsonObject(
      "notification" -> callback.notification,
      "filename" -> callback.filename,
      "checksumAlgorithm" -> callback.checksumAlgorithm,
      "checksum" -> callback.checksum,
      "correlationID" -> callback.correlationID,
      "availableUntil" -> callback.availableUntil,
      "failureReason" -> callback.failureReason,
      "dateTime" -> callback.dateTime,
      optional("nrSubmissionId" -> callback.getPropertyValue(nrsSubmissionKey)),
      "attachmentId" -> callback.getPropertyValue(attachmentReferenceKey),
      "formBundleId" -> callback.getPropertyValue(formBundleKey)
    )
  }

  case class SdesCallbackNotSentToNrsAudit(callback: SdesCallback) extends AuditModel {
    override val auditType: String = "SDESCallbackNotSentToNRS"
    override val transactionName: String = "sdes-callback-not-sent-to-nrs"
    override val detail: JsValue = jsonObject(
      "notification" -> callback.notification,
      "filename" -> callback.filename,
      "checksumAlgorithm" -> callback.checksumAlgorithm,
      "checksum" -> callback.checksum,
      "correlationID" -> callback.correlationID,
      "availableUntil" -> callback.availableUntil,
      "failureReason" -> callback.failureReason,
      "dateTime" -> callback.dateTime,
      optional("nrSubmissionId" -> callback.getPropertyValue(nrsSubmissionKey)),
      "attachmentId" -> callback.getPropertyValue(attachmentReferenceKey),
      "formBundleId" -> callback.getPropertyValue(formBundleKey)
    )
  }

}
