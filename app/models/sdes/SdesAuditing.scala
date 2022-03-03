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

package models.sdes

import play.api.libs.json.{JsValue, Json}
import services.SdesService.{attachmentReferenceKey, formBundleKey, nrsSubmissionKey}
import services.monitoring.AuditModel

object SdesAuditing {

  case class SdesCallbackFailureAudit(callback: SdesCallback) extends AuditModel {
    override val auditType: String = "SDESCallBackFailure"
    override val transactionName: String = "sdes-callback-failure"
    override val detail: JsValue = Json.obj(
      "notification" -> callback.notification,
      "filename" -> callback.filename,
      "checksumAlgorithm" -> callback.checksumAlgorithm,
      "checksum" -> callback.checksum,
      "correlationID" -> callback.correlationID,
      "availableUntil" -> callback.availableUntil,
      "failureReason" -> callback.failureReason,
      "dateTime" -> callback.dateTime,
      "nrSubmissionId" -> callback.getPropertyValue(nrsSubmissionKey),
      "attachmentId" -> callback.getPropertyValue(attachmentReferenceKey),
      "formBundleId" -> callback.getPropertyValue(formBundleKey)
    )
  }

}
