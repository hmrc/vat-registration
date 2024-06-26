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

package models

import enums.VatRegStatus
import models.api.Attachments
import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

case class VatSchemeHeader(
  registrationId: String,
  status: VatRegStatus.Value,
  applicationReference: Option[String] = None,
  confirmInformationDeclaration: Option[Boolean] = None,
  createdDate: LocalDate,
  requiresAttachments: Boolean
)

case object VatSchemeHeader {
  val reads: Reads[VatSchemeHeader] = (
    (__ \ "registrationId").read[String] and
      (__ \ "status").read[VatRegStatus.Value] and
      (__ \ "applicationReference").readNullable[String] and
      (__ \ "confirmInformationDeclaration").readNullable[Boolean] and
      (__ \ "createdDate").read[LocalDate] and
      (__ \ "attachments").readNullable[Attachments].fmap(block => block.exists(_.method.isDefined))
  )(VatSchemeHeader.apply _)

  val writes: Writes[VatSchemeHeader] = Json.writes[VatSchemeHeader]

  implicit val format: Format[VatSchemeHeader] = Format[VatSchemeHeader](reads, writes)
}
