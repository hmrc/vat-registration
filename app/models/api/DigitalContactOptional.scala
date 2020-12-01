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
import play.api.libs.json.Reads._
import play.api.libs.json._

case class DigitalContactOptional(email: Option[String] = None,
                                  tel: Option[String] = None,
                                  mobile: Option[String] = None,
                                  emailVerified: Option[Boolean] = None)

object DigitalContactOptional extends VatDigitalContactValidator {
  implicit val format: Format[DigitalContactOptional] = new Format[DigitalContactOptional] {
    val defaultFormat: Format[DigitalContactOptional] = (
      (__ \ "email").formatNullable[String](readToFmt(maxLength[String](70) keepAnd email)) and
      (__ \ "tel").formatNullable[String](telValidator) and
      (__ \ "mobile").formatNullable[String](mobileValidator) and
      (__ \ "emailVerified").formatNullable[Boolean]
    )(DigitalContactOptional.apply, unlift(DigitalContactOptional.unapply))

    override def reads(json: JsValue): JsResult[DigitalContactOptional] = {
      if (json.equals(Json.obj())) {
        JsError("error.path.missing.atLeast.oneValue")
      } else {
        Json.fromJson(json)(defaultFormat)
      }
    }

    override def writes(o: DigitalContactOptional): JsObject = Json.toJson(o)(defaultFormat).as[JsObject]
  }

  val submissionFormat: Format[DigitalContactOptional] = (
    (__ \ "email").formatNullable[String] and
    (__ \ "telephone").formatNullable[String] and
    (__ \ "mobileNumber").formatNullable[String] and
    OFormat[Option[Boolean]](
      read =  { _: JsValue => JsSuccess(None) },
      write = { _: Option[Boolean] => Json.obj() }
    )
  )(DigitalContactOptional.apply, unlift(DigitalContactOptional.unapply))
}
