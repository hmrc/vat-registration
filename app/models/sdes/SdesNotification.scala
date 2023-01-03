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

import play.api.libs.json.{Json, OFormat}

case class SdesNotification(informationType: String,
                            file: FileDetails,
                            audit: AuditDetals)

object SdesNotification {
  implicit val format: OFormat[SdesNotification] = Json.format[SdesNotification]
}

case class FileDetails(recipientOrSender: String,
                       name: String,
                       location: String,
                       checksum: Checksum,
                       size: Int,
                       properties: List[Property]) extends PropertyExtractor

object FileDetails {
  implicit val format: OFormat[FileDetails] = Json.format[FileDetails]
}

case class Checksum(algorithm: String,
                    value: String)

object Checksum {
  implicit val format: OFormat[Checksum] = Json.format[Checksum]
}

case class AuditDetals(correlationID: String)

object AuditDetals {
  implicit val format: OFormat[AuditDetals] = Json.format[AuditDetals]
}