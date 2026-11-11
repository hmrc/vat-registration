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

package httpparsers

import httpparsers.VatSubmissionHttpParser.VatSubmissionResponse
import models.hip.HipPlatformErrors._
import models.hip.{Hip422Error, HipInner422Err, HipSubmissionSuccess, HipSuccessWrapper}
import play.api.http.Status._
import play.api.libs.json.JsValue
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import scala.util.Try

object HipVatSubmissionHttpParser {

  private val inFlightSubmissionCode         = "089"
  private val duplicateAcknowledgementRefCode = "004"
  private val serviceUnavailableCodes        = Set("003", "105", "999")

  implicit object HipVatSubmissionHttpReads extends HttpReads[VatSubmissionResponse] {
    override def read(method: String, url: String, response: HttpResponse): VatSubmissionResponse =
      response.status match {
        case CREATED                                     =>
          parseJson(response).flatMap(_.asOpt[HipSuccessWrapper[HipSubmissionSuccess]]) match {
            case Some(HipSuccessWrapper(HipSubmissionSuccess(formBundle))) =>
              Right(VatSubmissionSuccess(formBundle))
            case None                                                      =>
              Left(VatSubmissionFailure(CREATED, "VAT submission API - no form bundle ID in response"))
          }
        case BAD_REQUEST                                 =>
          envelopeFailure(response, BAD_REQUEST)
        case INTERNAL_SERVER_ERROR | SERVICE_UNAVAILABLE =>
          envelopeFailure(response, SERVICE_UNAVAILABLE)
        case UNPROCESSABLE_ENTITY                        =>
          handle422Response(response)
        case _                                           =>
          unexpected(response)
      }

    private def handle422Response(response: HttpResponse): VatSubmissionResponse = {
      parseJson(response).flatMap(_.asOpt[Hip422Error]) match {
        case Some(Hip422Error(HipInner422Err(`inFlightSubmissionCode`, _, _))) =>
          Left(VatSubmissionFailure(CONFLICT, "VAT Submission API - application already in progress"))
        case Some(Hip422Error(HipInner422Err(code@`duplicateAcknowledgementRefCode`, _, text))) =>
          Left(VatSubmissionFailure(INTERNAL_SERVER_ERROR, s"VAT Submission API - errors: $code: $text"))
        case Some(Hip422Error(HipInner422Err(code, _, text))) if serviceUnavailableCodes.contains(code) =>
          Left(VatSubmissionFailure(SERVICE_UNAVAILABLE, s"VAT Submission API - errors: $code: $text"))
        case _ =>
          unexpected(response)
      }
    }

    private def envelopeFailure(response: HttpResponse, status: Int): VatSubmissionResponse =
      parseHipErrorEnvelope(response) match {
        case (Some(origin), error: HipSystemErrorObject)  =>
          Left(VatSubmissionFailure(status, s"VAT Submission API - errors: ${describe(origin, error)}"))
        case (Some(origin), error: HipFailuresErrorArray) =>
          Left(VatSubmissionFailure(status, s"VAT Submission API - errors: ${describe(origin, error)}"))
        case _                                            =>
          unexpected(response)
      }

    private def parseHipErrorEnvelope(response: HttpResponse): (Option[Origin], HipErrorTrait) =
      parseJson(response).flatMap(_.asOpt[HipErrorWrapper]) match {
        case Some(HipErrorWrapper(origin, error)) => (Some(origin), error)
        case None                                 => (None, HipUnexpectedError(response.status, response.body))
      }

    private def describe(origin: Origin, error: HipErrorTrait): String =
      error match {
        case HipSystemErrorObject(HipError(code, logID, message)) =>
          s"$origin $code (logID $logID): $message"
        case HipFailuresErrorArray(failures)                      =>
          s"$origin ${failures.map(failure => s"${failure.`type`}: ${failure.reason}").mkString(", ")}"
        case HipUnexpectedError(status, body)                     =>
          s"$origin unexpected error - status = $status, body = $body"
      }

    private def parseJson(response: HttpResponse): Option[JsValue] =
      Try(response.json).toOption

    private def unexpected(response: HttpResponse): VatSubmissionResponse =
      Left(
        VatSubmissionFailure(
          response.status,
          s"Unexpected response from VAT Submission API - status = ${response.status}"
        )
      )
  }

}
