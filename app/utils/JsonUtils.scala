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

package utils

import play.api.libs.json.{JsObject, JsValue, Writes}

object JsonUtils {

  case class JsonField private(json: Option[(String, JsValue)])

  def jsonObject(fields: JsonField*): JsObject =
    JsObject(fields.flatMap(_.json))

  implicit def toJsonField[T](field: (String, T))(implicit writer: Writes[T]): JsonField =
    JsonField(Some(field._1 -> writer.writes(field._2)))

  def optional[T](field: (String, Option[T]))(implicit writer: Writes[T]): JsonField =
    field match {
      case (key, Some(value)) =>
        JsonField(Some(field._1 -> writer.writes(value)))
      case (key, None) =>
        JsonField(None)
    }
}