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

package httpparsers

import play.api.http.Status.{BAD_REQUEST, CONFLICT, OK}
import play.api.libs.json.JsSuccess
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object VatSubmissionHttpParser {

  val CodeKey = "code"
  val InvalidPayloadKey = "INVALID_PAYLOAD"
  val InvalidSessionIdKey = "INVALID_SESSIONID"
  val InvalidCredentialIdKey = "INVALID_CREDENTIALID"
  val FormBundleIdKey = "formBundle"

  type VatSubmissionResponse = Either[VatSubmissionFailure, VatSubmissionSuccess]

  implicit object VatSubmissionHttpReads extends HttpReads[VatSubmissionResponse] {
    override def read(method: String, url: String, response: HttpResponse): VatSubmissionResponse = {
      response.status match {
        case OK =>
          (response.json \ FormBundleIdKey).validate[String].asEither
            .map(VatSubmissionSuccess)
            .left.map(_ => VatSubmissionFailure(OK, "VAT submission API - no form bundle ID in response"))
        case BAD_REQUEST =>
          (response.json \ CodeKey).validate[String] match {
            case JsSuccess(InvalidPayloadKey, _) =>
              Left(VatSubmissionFailure(BAD_REQUEST, "VAT Submission API - invalid payload"))
            case JsSuccess(InvalidSessionIdKey, _) =>
              Left(VatSubmissionFailure(BAD_REQUEST, "VAT Submission API - invalid Session ID"))
            case JsSuccess(InvalidCredentialIdKey, _) =>
              Left(VatSubmissionFailure(BAD_REQUEST, "VAT Submission API - invalid Credential ID"))
            case _ =>
              Left(VatSubmissionFailure(BAD_REQUEST, s"Unexpected response from VAT Submission API - status = ${response.status}"))
          }
        case CONFLICT =>
          Left(VatSubmissionFailure(CONFLICT, "VAT Submission API - application already in progress"))
        case _ =>
          Left(VatSubmissionFailure(response.status, s"Unexpected response from VAT Submission API - status = ${response.status}"))
      }
    }
  }

}

case class VatSubmissionSuccess(formBundleId: String)

case class VatSubmissionFailure(status: Int, body: String)