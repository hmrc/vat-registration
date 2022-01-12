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

package models.api

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Name(first: Option[String],
                middle: Option[String],
                last: String)

object Name extends VatApplicantDetailsValidator {
  implicit val format: Format[Name] = (
    (__ \ "first").formatNullable[String](nameValidator) and
    (__ \ "middle").formatNullable[String](nameValidator) and
    (__ \ "last").format[String](nameValidator)
  )(Name.apply, unlift(Name.unapply))

  val auditWrites: Format[Name] = (
    (__ \ "firstName").formatNullable[String](nameValidator) and
    (__ \ "middleName").formatNullable[String](nameValidator) and
    (__ \ "lastName").format[String](nameValidator)
  )(Name.apply, unlift(Name.unapply))

}
