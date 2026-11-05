/*
 * Copyright 2026 HM Revenue & Customs
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

package models.hip

import play.api.libs.json._

object HipPlatformErrors {

  sealed trait Origin
  object Origin {
    case object HoD extends Origin
    case object HIP extends Origin
  }

  implicit val originReads: Reads[Origin] = Reads {
    case JsString("HoD") => JsSuccess(Origin.HoD)
    case JsString("HIP") => JsSuccess(Origin.HIP)
    case JsString(_)     => JsError("Invalid origin string")
    case _               => JsError("Expected JsString")
  }

  case class HipFailure(`type`: String, reason: String)
  case class HipError(code: String, logID: String, message: String)

  sealed trait HipErrorTrait
  case class HipSystemErrorObject(error: HipError)              extends HipErrorTrait
  case class HipFailuresErrorArray(failures: Array[HipFailure]) extends HipErrorTrait
  case class HipUnexpectedError(status: Int, body: String)      extends HipErrorTrait

  implicit val failureReads: Reads[HipFailure] = Json.reads[HipFailure]
  implicit val errorReads: Reads[HipError]     = Json.reads[HipError]

  implicit val hipErrorTraitReads: Reads[HipErrorTrait] = Reads {
    case obj: JsObject if (obj \ "failures").isDefined =>
      (obj \ "failures").validate[Seq[HipFailure]].map(failures => HipFailuresErrorArray(failures.toArray))
    case obj: JsObject if (obj \ "error").isDefined    =>
      (obj \ "error").validate[HipError].map(HipSystemErrorObject)
    case badJson                                       =>
      JsError(s"Expected HipErrorTrait Json... got $badJson")
  }

  case class HipErrorWrapper(origin: Origin, response: HipErrorTrait)

  implicit val hipErrorWrapperReads: Reads[HipErrorWrapper] = Json.reads[HipErrorWrapper]
}
