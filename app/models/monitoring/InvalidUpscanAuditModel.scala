/*
 * Copyright 2024 HM Revenue & Customs
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

import models.api.UpscanDetails
import play.api.libs.json.{JsValue, Json}
import services.monitoring.AuditModel

case class InvalidUpscanAuditModel(upscanDetails: UpscanDetails) extends AuditModel {
  override val auditType: String = "InvalidUpscanReceivedError"
  override val transactionName: String = "submit-attachment-to-nrs"
  override val detail: JsValue = Json.obj(
    "registrationId" -> upscanDetails.registrationId,
          "reference" -> upscanDetails.reference,
          "attachmentType" -> upscanDetails.attachmentType,
          "downloadUrl" -> upscanDetails.downloadUrl,
          "fileStatus" -> upscanDetails.fileStatus,
          "uploadDetails" -> upscanDetails.uploadDetails,
          "failureDetails" -> upscanDetails.failureDetails
  )
}
