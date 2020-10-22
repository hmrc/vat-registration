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

package utils

import play.api.libs.json._

trait JsonUtilities {

  implicit class JsonUtilities(json: JsValue) {

    def getOptionalField[T](path: JsPath)(implicit r: Reads[T]): Option[T] =
      path(json)
        .headOption
        .map(_.result.as[T])

    def getField[T](path: JsPath)(implicit r: Reads[T]): T =
      getOptionalField(path)
        .getOrElse(throw new Exception(s"Could not parse JSON at path: ${path.toString()}"))

    def filterNullFields: JsValue = json match {
      case JsObject(fieldSet) => JsObject(fieldSet.flatMap {
        case (_, JsNull) => None
        case _@value => Some(value)
      })
      case obj => obj
    }

  }
}
