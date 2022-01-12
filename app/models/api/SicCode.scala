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

case class SicCode(id: String,
                   description: String,
                   displayDetails: String)

object SicCode extends SicCodeValidator {

  val apiFormat: Format[SicCode] = (
    (__ \ "code").format[String](idValidator) and
      (__ \ "desc").format[String] and
      (__ \ "indexes").format[String]
    ) (SicCode.apply, unlift(SicCode.unapply))

  val sicCodeListWrites = Writes[List[SicCode]] { codes =>
    Json.obj(
      "mainCode2" -> codes.lift(0).map(_.id),
      "mainCode3" -> codes.lift(1).map(_.id),
      "mainCode4" -> codes.lift(2).map(_.id)
    ) match {
      case JsObject(fieldSet) => JsObject(fieldSet.flatMap {
        case (_, JsNull) => None
        case _@value => Some(value)
      })
      case obj => obj
    }
  }

  implicit val format: Format[SicCode] = Json.format[SicCode]

}
