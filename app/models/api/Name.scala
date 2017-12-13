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

case class Name(first: String,
                middle: Option[String],
                last: Option[String],
                @deprecated("Use first instead", "SCRS-9379") forename: Option[String] = None,
                @deprecated("Use middle instead", "SCRS-9379") otherForenames: Option[String] = None,
                @deprecated("Use last instead", "SCRS-9379") surname: Option[String] = None,
                @deprecated("No use anymore", "SCRS-9379") title: Option[String] = None)

object Name extends VatLodgingOfficerValidator {

  implicit val format: Format[Name] = (
    (__ \ "first").format[String] and
    (__ \ "middle").formatNullable[String] and
    (__ \ "last").formatNullable[String] and
    (__ \ "forename").formatNullable[String](nameValidator) and
    (__ \ "other_forenames").formatNullable[String](nameValidator) and
    (__ \ "surname").formatNullable[String](nameValidator) and
    (__ \ "title").formatNullable[String](titleValidator)
  )(Name.apply, unlift(Name.unapply))

  // $COVERAGE-OFF$
  val writesDES: Writes[Name] = new Writes[Name] {
    override def writes(name: Name): JsValue = {
      val successWrites = (
        (__ \ "first").write[String] and
        (__ \ "middle").writeNullable[String] and
        (__ \ "last").writeNullable[String] and
        (__ \ "firstName").writeNullable[String] and
        (__ \ "middleName").writeNullable[String] and
        (__ \ "lastName").writeNullable[String] and
        (__ \ "title").writeNullable[String]
      )(unlift(Name.unapply))

      Json.toJson(name)(successWrites).as[JsObject]
    }
  }
  // $COVERAGE-ON$
}
