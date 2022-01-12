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

package common.exceptions

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.Helpers._

import scala.concurrent.Future


class LeftStateSpec extends PlaySpec {

  private def errorJson(statusCode: Int, msg: String): JsObject = Json.obj("errorCode" -> statusCode, "errorMessage" -> msg)

  "converting NotFound to Result" should {
    "produce result with 404 status code and JSON body" in {
      val message = "notfound"
      val result: Future[Result] = Future.successful(ResourceNotFound(message).toResult)
      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe errorJson(NOT_FOUND, message)
    }
  }

  "converting Forbidden to Result" should {
    "produce result with 403 status code and JSON body" in {
      val message = "forbidden"
      val result: Future[Result] = Future.successful(ForbiddenAccess(message).toResult)
      status(result) mustBe FORBIDDEN
      contentAsJson(result) mustBe errorJson(FORBIDDEN, message)
    }
  }

  "converting GenericDatabaseError to Result" should {
    "produce result with 503 status code and JSON body" in {
      val message = "db-gone"
      val regId = "regId"
      val result: Future[Result] = Future.successful(GenericDatabaseError(new RuntimeException(message), Some(regId)).toResult)
      status(result) mustBe SERVICE_UNAVAILABLE
      contentAsJson(result) mustBe errorJson(SERVICE_UNAVAILABLE, s"Mongo exception: java.lang.RuntimeException: $message ; registration ID: $regId")
    }
  }

  "converting GenericError to Result" should {
    "produce result with 503 status code and JSON body" in {
      val message = "error"
      val result: Future[Result] = Future.successful(GenericError(new RuntimeException(message)).toResult)
      status(result) mustBe SERVICE_UNAVAILABLE
      contentAsJson(result) mustBe errorJson(SERVICE_UNAVAILABLE, s"Generic exception: java.lang.RuntimeException: $message")
    }
  }
}
