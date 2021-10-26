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

package models.submission

import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

case class CustomerId(idValue: String,
                      idType: IdType,
                      IDsVerificationStatus: IdVerificationStatus = IdVerified,
                      countryOfIncorporation: Option[String] = None,
                      date: Option[LocalDate] = None,
                      safeIDBPFound: Option[String] = None,
                      partyType: Option[PartyType] = None)

object CustomerId {
  implicit val format: OFormat[CustomerId] = Json.format[CustomerId]
  val transactorWrites: Writes[CustomerId] = (
    (__ \ "idValue").write[String] and
    (__ \ "idType").write[IdType] and
    (__ \ "IDsFailedOnlineVerification").write[IdVerificationStatus] and
    (__ \ "countryOfIncorporation").writeNullable[String] and
    (__ \ "date").writeNullable[LocalDate] and
    (__ \ "safeIDBPFound").writeNullable[String] and
    (__ \ "partyType").writeNullable[PartyType]
  ) (unlift(CustomerId.unapply))

}


