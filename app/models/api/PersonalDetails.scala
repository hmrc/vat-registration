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

package models.api

import models.submission._
import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

case class PersonalDetails(
  name: Name,
  nino: Option[String],
  trn: Option[String],
  arn: Option[String],
  identifiersMatch: Boolean,
  dateOfBirth: Option[LocalDate],
  score: Option[Int]
) {

  def personalIdentifiers: List[CustomerId] =
    List(
      nino.map(nino =>
        CustomerId(
          nino,
          NinoIdType,
          if (identifiersMatch) IdVerified else IdVerificationFailed,
          date = dateOfBirth
        )
      ),
      None,
      arn.map(arn =>
        CustomerId(
          arn,
          ArnIdType,
          IdVerified
        )
      )
    ).flatten
}

object PersonalDetails {
  implicit val format: Format[PersonalDetails] = (
    (__ \ "name").format[Name] and
      (__ \ "nino").formatNullable[String] and
      (__ \ "trn").formatNullable[String] and
      (__ \ "arn").formatNullable[String] and
      (__ \ "identifiersMatch").formatWithDefault[Boolean](true) and
      (__ \ "dateOfBirth").formatNullable[LocalDate] and
      (__ \ "score").formatNullable[Int]
  )(PersonalDetails.apply, unlift(PersonalDetails.unapply))
}
