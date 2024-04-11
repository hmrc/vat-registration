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

import java.time.ZoneId
import java.time.format.DateTimeFormatter

case class Property(name: String, value: String)

object Property {
  implicit val format: OFormat[Property] = Json.format[Property]
}

trait PropertyExtractor {
  val properties: List[Property]

  def getPropertyValue(key: String): Option[String] = this.properties.find(_.name.equals(key)).map(_.value)
}

object PropertyExtractor {
  val mimeTypeKey            = "mimeType"
  val prefixedFormBundleKey  = "prefixedFormBundleId"
  val formBundleKey          = "formBundleId"
  val attachmentReferenceKey = "attachmentId"
  val submissionDateKey      = "submissionDate"
  val nrsSubmissionKey       = "nrsSubmissionId"
  val locationKey            = "location"

  val checksumAlgorithm                    = "SHA-256"
  val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter
    .ofPattern("dd/MM/yyyy hh:mm:ss")
    .withZone(ZoneId.of("UTC"))
}
