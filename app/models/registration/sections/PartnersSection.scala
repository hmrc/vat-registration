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

package models.registration.sections

import models.api.Partner
import models.registration.RegistrationSection
import play.api.libs.json.{Format, Json, Reads, Writes}

case class PartnersSection(partners: List[Partner]) extends RegistrationSection[PartnersSection] {

  override def isComplete: PartnersSection => Boolean = {
    _ => true
  }

}

object PartnersSection {
  implicit val format: Format[PartnersSection] = Format[PartnersSection](
    Reads[PartnersSection] { json =>
      json.validate[List[Partner]].orElse(
        (json \ "partners").validate[List[Partner]]
      ).map(PartnersSection.apply)
    },
    Writes[PartnersSection] { partnerSection =>
      Json.toJson(partnerSection.partners)
    }
  )
}

