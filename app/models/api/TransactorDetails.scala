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

import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

case class TransactorDetails(name: Name,
                             nino: Option[String],
                             trn: Option[String],
                             identifiersMatch: Boolean,
                             dateOfBirth: LocalDate)

object TransactorDetails {
  implicit val format: Format[TransactorDetails] = (
    (__ \ "name").format[Name] and
      (__ \ "nino").formatNullable[String] and
      (__ \ "trn").formatNullable[String] and
      (__ \ "identifiersMatch").formatWithDefault[Boolean](true) and
      (__ \ "dateOfBirth").format[LocalDate]
    ) (TransactorDetails.apply, unlift(TransactorDetails.unapply))
}
