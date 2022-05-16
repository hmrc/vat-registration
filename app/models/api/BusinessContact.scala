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

case class BusinessContact(email: Option[String],
                           telephoneNumber: Option[String],
                           mobile: Option[String],
                           hasWebsite: Option[Boolean],
                           website: Option[String],
                           ppob: Address,
                           commsPreference: ContactPreference)

object BusinessContact {

  // TODO Replace OFormat instances with (__ \ "").formatNullable[] 2 weeks from deployment
  implicit val format: OFormat[BusinessContact] = (
    OFormat(
      (__ \ "email").read[String].fmap(Option[String]).orElse((__ \ "digitalContact" \ "email").readNullable[String]),
      (__ \ "email").writeNullable[String]
    ) and
      OFormat(
        (__ \ "telephoneNumber").read[String].fmap(Option[String]).orElse((__ \ "digitalContact" \ "tel").readNullable[String]),
        (__ \ "telephoneNumber").writeNullable[String]
      ) and
      OFormat(
        (__ \ "mobile").read[String].fmap(Option[String]).orElse((__ \ "digitalContact" \ "mobile").readNullable[String]),
        (__ \ "mobile").writeNullable[String]
      ) and
      (__ \ "hasWebsite").formatNullable[Boolean] and
      (__ \ "website").formatNullable[String] and
      (__ \ "ppob").format[Address] and
      (__ \ "contactPreference").format[ContactPreference]
    ) (BusinessContact.apply, unlift(BusinessContact.unapply))

}
