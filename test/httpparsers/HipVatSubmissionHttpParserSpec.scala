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

import httpparsers.HipVatSubmissionHttpParser._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http._

class HipVatSubmissionHttpParserSpec extends PlaySpec {

  val testFormBundleId = "123412341234"
  val testLogId        = "D82EBAB67AC6D7565C0682CA91BDC577"

  def read(status: Int, body: String): VatSubmissionHttpParser.VatSubmissionResponse =
    HipVatSubmissionHttpReads.read(method = "POST", url = "/", response = HttpResponse(status, body))

  def unexpected(status: Int): VatSubmissionHttpParser.VatSubmissionResponse =
    Left(VatSubmissionFailure(status, s"Unexpected response from VAT Submission API - status = $status"))

  def hodEnvelope(code: String, message: String): String =
    Json.stringify(
      Json.obj(
        "origin"   -> "HoD",
        "response" -> Json.obj("error" -> Json.obj("code" -> code, "logID" -> testLogId, "message" -> message))
      )
    )

  def hipEnvelope(failures: (String, String)*): String =
    Json.stringify(
      Json.obj(
        "origin"   -> "HIP",
        "response" -> Json.obj("failures" -> failures.map { case (t, r) => Json.obj("type" -> t, "reason" -> r) })
      )
    )

  def unprocessable(code: String, text: String): String =
    Json.stringify(
      Json.obj("error" -> Json.obj("code" -> code, "processingDate" -> "2026-03-09T12:34:46Z", "text" -> text))
    )

  "HipVatSubmissionHttpParser" when {

    "the response is CREATED" should {
      "successfully parse the formBundle from the success wrapper" in {
        val body = Json.obj("success" -> Json.obj("formBundle" -> testFormBundleId))

        read(CREATED, Json.stringify(body)) mustBe Right(VatSubmissionSuccess(testFormBundleId))
      }

      "fail when the response doesn't contain a success wrapper" in {
        val body = Json.obj("formBundle" -> testFormBundleId)

        read(CREATED, Json.stringify(body)) mustBe Left(
          VatSubmissionFailure(CREATED, "VAT submission API - unexpected success response format: missing 'success.formBundle'")
        )
      }

      "fail when the response doesn't contain a formBundle" in {
        val body = Json.obj("success" -> Json.obj("" -> ""))

        read(CREATED, Json.stringify(body)) mustBe Left(
          VatSubmissionFailure(CREATED, "VAT submission API - unexpected success response format: missing 'success.formBundle'")
        )
      }
    }

    "the response is BAD_REQUEST" should {

      "describe a HIP failures envelope" in {
        read(BAD_REQUEST, hipEnvelope("Type of Failure" -> "Reason for Failure", "Other Type" -> "Other Reason")) mustBe
          Left(
            VatSubmissionFailure(
              BAD_REQUEST,
              "VAT Submission API - errors: HIP Type of Failure: Reason for Failure, Other Type: Other Reason"
            )
          )
      }

      "describe a HoD error envelope" in {
        read(BAD_REQUEST, hodEnvelope("400", "Invalid payload")) mustBe Left(
          VatSubmissionFailure(BAD_REQUEST, s"VAT Submission API - errors: HoD 400 (logID $testLogId): Invalid payload")
        )
      }

      "fall back to an unexpected response when the body cannot be parsed" in {
        read(BAD_REQUEST, "{}") mustBe unexpected(BAD_REQUEST)
      }
    }

    "the response is UNPROCESSABLE_ENTITY" should {
      "map code 089 to CONFLICT" in {
        read(UNPROCESSABLE_ENTITY, unprocessable("089", "Applicant already has an in-flight submission, FB 123")) mustBe
          Left(VatSubmissionFailure(CONFLICT, "VAT Submission API - application already in progress"))
      }

      "map code 004 to INTERNAL_SERVER_ERROR" in {
        read(UNPROCESSABLE_ENTITY, unprocessable("004", "Duplicate submission acknowledgment reference")) mustBe
          Left(
            VatSubmissionFailure(
              INTERNAL_SERVER_ERROR,
              "VAT Submission API - errors: 004: Duplicate submission acknowledgment reference"
            )
          )
      }

      Seq(
        "003" -> "Request could not be processed",
        "105" -> "ID UTR1234567890 already exist in current FB, form 123",
        "999" -> "Technical System Error"
      ).foreach { case (code, text) =>
        s"map code $code to SERVICE_UNAVAILABLE" in {
          read(UNPROCESSABLE_ENTITY, unprocessable(code, text)) mustBe Left(
            VatSubmissionFailure(SERVICE_UNAVAILABLE, s"VAT Submission API - errors: $code: $text")
          )
        }
      }

      "fall back to an unexpected response for an unknown code" in {
        read(UNPROCESSABLE_ENTITY, unprocessable("123", "Unknown")) mustBe unexpected(UNPROCESSABLE_ENTITY)
      }

      "fall back to an unexpected response when the body cannot be parsed" in {
        read(UNPROCESSABLE_ENTITY, "not json") mustBe unexpected(UNPROCESSABLE_ENTITY)
      }
    }

    "the response is INTERNAL_SERVER_ERROR" should {
      "map a HoD error envelope to SERVICE_UNAVAILABLE" in {
        read(INTERNAL_SERVER_ERROR, hodEnvelope("500", "Internal Server Error")) mustBe Left(
          VatSubmissionFailure(
            SERVICE_UNAVAILABLE,
            s"VAT Submission API - errors: HoD 500 (logID $testLogId): Internal Server Error"
          )
        )
      }

      "map a HIP failures envelope to SERVICE_UNAVAILABLE" in {
        read(INTERNAL_SERVER_ERROR, hipEnvelope("Type of Failure" -> "Reason for Failure")) mustBe Left(
          VatSubmissionFailure(SERVICE_UNAVAILABLE, "VAT Submission API - errors: HIP Type of Failure: Reason for Failure")
        )
      }

      "fall back to an unexpected response when the body cannot be parsed" in {
        read(INTERNAL_SERVER_ERROR, "{}") mustBe unexpected(INTERNAL_SERVER_ERROR)
      }
    }

    "the response is SERVICE_UNAVAILABLE" should {
      "map a HIP failures envelope to SERVICE_UNAVAILABLE" in {
        read(SERVICE_UNAVAILABLE, hipEnvelope("Type of Failure" -> "Reason for Failure")) mustBe Left(
          VatSubmissionFailure(SERVICE_UNAVAILABLE, "VAT Submission API - errors: HIP Type of Failure: Reason for Failure")
        )
      }

      "fall back to an unexpected response when the body cannot be parsed" in {
        read(SERVICE_UNAVAILABLE, "") mustBe unexpected(SERVICE_UNAVAILABLE)
      }
    }

    Seq(UNAUTHORIZED, FORBIDDEN, NOT_FOUND).foreach { status =>
      s"the response is $status with no body" should {
        "fall back to an unexpected response keeping the status" in {
          read(status, "") mustBe unexpected(status)
        }
      }
    }
  }

}
