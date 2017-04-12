/*
 * Copyright 2017 HM Revenue & Customs
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


case class VatDigitalContact(email: String, tel: Option[String], mobile: Option[String])

object VatDigitalContact extends VatDigitalContactValidator {

  implicit val format: OFormat[VatDigitalContact] = (
    (__ \ "email").format[String](maxLength[String](70) keepAnd emailValidator) and
      (__ \ "tel").formatNullable[String](teleValidator) and
      (__ \ "mobile").formatNullable[String](mobileValidator)
    ) (VatDigitalContact.apply, unlift(VatDigitalContact.unapply))
}
