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

package models.api

import java.time.LocalDate
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class RegistrationInformation(internalId: String,
                                   registrationId: String,
                                   status: RegistrationStatus,
                                   regStartDate: LocalDate,
                                   channel: RegistrationChannel,
                                   lastModified: LocalDate)

object RegistrationInformation {
  val reads: Reads[RegistrationInformation] = (
    (__ \ "internalId").read[String] and
    (__ \ "registrationId").read[String] and
    (__ \ "status").read[RegistrationStatus] and
    (__ \ "regStartDate").read[LocalDate] and
    (__ \ "channel").read[RegistrationChannel] and
      // Default to regStartDate for records created before this field was added
    (__ \ "lastModified").read[LocalDate].orElse((__ \ "regStartDate").read[LocalDate])
  )(RegistrationInformation.apply _)

  val writes: Writes[RegistrationInformation] = Json.writes[RegistrationInformation]

  implicit val format: Format[RegistrationInformation] = Format(reads, writes)
}
