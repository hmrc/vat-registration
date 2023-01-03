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

package models.nonrepudiation

import models.sdes.PropertyExtractor.{attachmentReferenceKey, formBundleKey, nrsSubmissionKey}
import models.sdes.SdesCallback
import play.api.libs.json.{JsValue, Json}
import services.monitoring.AuditModel
import utils.JsonUtils._

object NonRepudiationAuditing {

  case class NonRepudiationSubmissionSuccessAudit(journeyid: String,
                                                  nonRepudiationSubmissionId: String) extends AuditModel {
    override val auditType: String = "SubmitVATRegistrationToNrs"
    override val transactionName: String = "submit-vat-registration-to-nrs"
    override val detail: JsValue = Json.obj(
      "journeyId" -> journeyid,
      "nrSubmissionId" -> nonRepudiationSubmissionId
    )
  }

  case class NonRepudiationSubmissionFailureAudit(journeyId: String,
                                                  statusCode: Int,
                                                  body: String) extends AuditModel {
    override val auditType: String = "SubmitVATRegistrationToNRSError"
    override val transactionName: String = "submit-vat-registration-to-nrs"
    override val detail: JsValue = Json.obj(
      "statusCode" -> statusCode.toString,
      "statusReason" -> body,
      "journeyId" -> journeyId
    )
  }

  case class NonRepudiationAttachmentSuccessAudit(sdesCallback: SdesCallback, nrAttachmentId: String) extends AuditModel {
    override val auditType: String = "SubmitAttachmentToNrs"
    override val transactionName: String = "submit-attachment-to-nrs"
    override val detail: JsValue = jsonObject(
      "nrSubmissionId" -> sdesCallback.getPropertyValue(nrsSubmissionKey),
      "filename" -> sdesCallback.filename,
      "checksumAlgorithm" -> sdesCallback.checksumAlgorithm,
      "checksum" -> sdesCallback.checksum,
      "correlationID" -> sdesCallback.correlationID,
      "availableUntil" -> sdesCallback.correlationID,
      "nrAttachmentId" -> nrAttachmentId,
      "attachmentId" -> sdesCallback.getPropertyValue(attachmentReferenceKey),
      "formBundleId" -> sdesCallback.getPropertyValue(formBundleKey)
    )
  }

  case class NonRepudiationAttachmentFailureAudit(sdesCallback: SdesCallback, status: Int) extends AuditModel {
    override val auditType: String = "SubmitAttachmentToNRSError"
    override val transactionName: String = "submit-attachment-to-nrs"
    override val detail: JsValue = jsonObject(
      "nrSubmissionId" -> sdesCallback.getPropertyValue(nrsSubmissionKey),
      "filename" -> sdesCallback.filename,
      "checksumAlgorithm" -> sdesCallback.checksumAlgorithm,
      "checksum" -> sdesCallback.checksum,
      "correlationID" -> sdesCallback.correlationID,
      "availableUntil" -> sdesCallback.correlationID,
      "attachmentId" -> sdesCallback.getPropertyValue(attachmentReferenceKey),
      "formBundleId" -> sdesCallback.getPropertyValue(formBundleKey),
      "statusCode" -> status
    )
  }
}
