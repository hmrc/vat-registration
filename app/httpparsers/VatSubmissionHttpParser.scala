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

package httpparsers

import play.api.http.Status.{BAD_REQUEST, CONFLICT, OK}
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.{ConflictException, HttpReads, HttpResponse, InternalServerException}

object VatSubmissionHttpParser {

  val CodeKey = "code"
  val InvalidPayloadKey = "INVALID_PAYLOAD"
  val InvalidSessionIdKey = "INVALID_SESSIONID"
  val InvalidCredentialIdKey = "INVALID_CREDENTIALID"
  val FormBundleIdKey = "formBundle"

  implicit object VatSubmissionHttpReads extends HttpReads[String] {
    override def read(method: String, url: String, response: HttpResponse): String = {
      response.status match {
        case OK =>
          (response.json \ FormBundleIdKey).validate[String]
            .getOrElse(throw new InternalServerException("VAT submission API - no form bundle ID in response"))
        case BAD_REQUEST =>
          (response.json \ CodeKey).validate[String] match {
            case JsSuccess(InvalidPayloadKey, _) =>
              throw new InternalServerException("VAT Submission API - invalid payload")
            case JsSuccess(InvalidSessionIdKey, _) =>
              throw new InternalServerException("VAT Submission API - invalid Session ID")
            case JsSuccess(InvalidCredentialIdKey, _) =>
              throw new InternalServerException("VAT Submission API - invalid Credential ID")
            case _ =>
              throw new InternalServerException(s"Unexpected Json response for this status: $BAD_REQUEST")
          }
        case CONFLICT =>
          throw new ConflictException("VAT Submission API - application already in progress")
        case _ =>
          throw new InternalServerException(
            s"Unexpected response from VAT Submission API - status = ${response.status}, body = ${response.body}"
          )
      }
    }
  }

}
