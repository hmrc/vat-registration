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

package models.nonrepudiation

import play.api.libs.json.{JsValue, Json}
import services.monitoring.AuditModel

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

}
