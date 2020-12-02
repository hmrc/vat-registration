/*
 * Copyright 2020 HM Revenue & Customs
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
import play.api.libs.json.Reads._

case class DigitalContact(email: String, tel: Option[String], mobile: Option[String])

object DigitalContact {

  val apiFormat: Format[DigitalContact] = (
    (__ \ "email").format[String] and
    (__ \ "tel").formatNullable[String] and
    (__ \ "mobile").formatNullable[String]
  )(DigitalContact.apply, unlift(DigitalContact.unapply))

  val submissionFormat: OFormat[DigitalContact] = (
    (__ \ "email").format[String] and
    (__ \ "telephone").formatNullable[String] and
    (__ \ "mobileNumber").formatNullable[String]
  )(DigitalContact.apply, unlift(DigitalContact.unapply))

}
