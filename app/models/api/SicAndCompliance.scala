/*
 * Copyright 2018 HM Revenue & Customs
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

import play.api.libs.json.{OFormat, __}
import play.api.libs.functional.syntax._

case class SicAndCompliance(businessDescription:String,
                            labourCompliance:ComplianceLabour,
                            mainBusinessActivity:SicCode)

object SicAndCompliance {

  implicit val formats = (
        (__ \ "businessDescription").format[String] and
        (__ \ "labourCompliance").format[ComplianceLabour] and
        (__ \ "mainBusinessActivity").format[SicCode]
  )(SicAndCompliance.apply, unlift(SicAndCompliance.unapply))
  }
