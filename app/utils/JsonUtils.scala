/*
 * Copyright 2023 HM Revenue & Customs
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

import play.api.libs.json.{Format, JsObject, JsValue, Writes}

object JsonUtils {

  case class JsonField private (json: Option[(String, JsValue)])

  def jsonObject(fields: JsonField*): JsObject =
    JsObject(fields.flatMap(_.json))

  implicit def toJsonField[T](field: (String, T))(implicit writer: Writes[T]): JsonField =
    JsonField(Some(field._1 -> writer.writes(field._2)))

  def optional[T](field: (String, Option[T]))(implicit writer: Writes[T]): JsonField =
    field match {
      case (key, Some(value)) =>
        JsonField(Some(field._1 -> writer.writes(value)))
      case (key, None)        =>
        JsonField(None)
    }

  def required[T](field: (String, Option[T]))(implicit writer: Writes[T]): JsonField =
    field match {
      case (_, Some(value)) =>
        JsonField(Some(field._1 -> writer.writes(value)))
      case (_, None)        =>
        throw new IllegalStateException(s"Field '${field._1}' was missing but is required")
    }

  def optionalRequiredIf[T](condition: => Boolean)(field: (String, Option[T]))(implicit writer: Writes[T]): JsonField =
    if (condition)
      JsonField(
        Some(
          field._1 -> writer.writes(
            field._2.getOrElse(throw new IllegalStateException(s"Field '${field._1}' was missing but is required"))
          )
        )
      )
    else JsonField(None)

  def conditional[T](condition: => Boolean)(field: (String, T))(implicit writer: Writes[T]): JsonField =
    if (condition) JsonField(Some(field._1 -> writer.writes(field._2)))
    else JsonField(None)

  def canParseTo[A](implicit fmt: Format[A]): PartialFunction[JsValue, A] = new PartialFunction[JsValue, A] {
    def apply(js: JsValue): A             = js.validate[A].get
    def isDefinedAt(js: JsValue): Boolean = js.validate[A].isSuccess
  }
}
