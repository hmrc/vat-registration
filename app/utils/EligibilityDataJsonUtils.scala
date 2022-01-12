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

package utils

import play.api.libs.json._

object EligibilityDataJsonUtils {
  def toJsObject(json: JsValue): JsObject = {
    val list = (json \\ "data").foldLeft(Seq.empty[JsValue])((l, v) => l ++ v.as[Seq[JsValue]])
    list.foldLeft(Json.obj())((o, v) => o + ((v \ "questionId").as[String] -> (v \ "answerValue").as[JsValue]))
  }

  def mongoReads[T](implicit r: Reads[T]): Reads[T] = new Reads[T] {
    override def reads(json: JsValue): JsResult[T] = toJsObject(json).validate[T]
  }
}
