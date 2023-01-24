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

import play.api.libs.json._
import utils.JsonUtils._

object EligibilityDataJsonUtils {
  def toJsObject(json: JsValue, regId: String): JsObject = {
    (json \\ "data") match {
      case Nil => json.as[JsObject]
      case oldList =>
        val list = oldList.foldLeft(Seq.empty[JsValue])((l, v) => l ++ v.as[Seq[JsValue]])
        val cleanedUp = list.foldLeft(Map[String, JsValue]())((o, v) => o + ((v \ "questionId").as[String] -> (v \ "answerValue").as[JsValue]))
        val jsonList = cleanedUp.map {
          case ("thresholdInTwelveMonths", json) =>
            jsonObject(
              "thresholdInTwelveMonths" -> jsonObject(
                "value" -> json,
                optional("optionalData" -> cleanedUp.get("thresholdInTwelveMonths-optionalData"))
              )
            )
          case ("thresholdPreviousThirtyDays", json) =>
            jsonObject(
              "thresholdPreviousThirtyDays" -> jsonObject(
                "value" -> json,
                optional("optionalData" -> cleanedUp.get("thresholdPreviousThirtyDays-optionalData"))
              )
            )
          case ("thresholdNextThirtyDays", json) =>
            jsonObject(
              "thresholdNextThirtyDays" -> jsonObject(
                "value" -> json,
                optional("optionalData" -> cleanedUp.get("thresholdNextThirtyDays-optionalData"))
              )
            )
          case ("thresholdTaxableSupplies", json) =>
            jsonObject(
              "thresholdTaxableSupplies" -> jsonObject(
                "date" -> json
              )
            )
          case ("dateOfBusinessTransfer", json) =>
            jsonObject(
              "dateOfBusinessTransfer" -> jsonObject(
                "date" -> json
              )
            )
          case ("thresholdInTwelveMonths-optionalData", _) |
            ("thresholdPreviousThirtyDays-optionalData", _) |
            ("thresholdNextThirtyDays-optionalData", _) => jsonObject()
          case (field, json) =>
            jsonObject(
              field -> json
            )
        }
        jsonList.foldLeft(Json.obj())((o , v) => o ++ v) ++ Json.obj(
          "CurrentProfile" -> Json.obj(
            "registrationID" -> regId
          )
        )
    }
  }
}