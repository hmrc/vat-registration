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

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class DateOfBirth(day: Int, month: Int, year: Int)

object DateOfBirth {
  implicit val format = (
    (__ \ "day").format[Int](min(1) keepAnd max(31)) and
      (__ \ "month").format[Int](min(1) keepAnd max(12)) and
      (__ \ "year").format[Int](min(1000) keepAnd max(9999))
  ) (DateOfBirth.apply, unlift(DateOfBirth.unapply))
}