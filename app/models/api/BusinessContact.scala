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

import models.registration.RegistrationSection
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class BusinessContact(digitalContact: DigitalContact,
                           website: Option[String],
                           ppob: Address,
                           commsPreference: ContactPreference) extends RegistrationSection[BusinessContact] {

  override def isComplete: BusinessContact => Boolean = {
    _ => true
  }

}

object BusinessContact {

  implicit val format: OFormat[BusinessContact] = (
    (__ \ "digitalContact").format[DigitalContact](DigitalContact.apiFormat) and
      (__ \ "website").formatNullable[String] and
      (__ \ "ppob").format[Address] and
      (__ \ "contactPreference").format[ContactPreference]
    ) (BusinessContact.apply, unlift(BusinessContact.unapply))

}
