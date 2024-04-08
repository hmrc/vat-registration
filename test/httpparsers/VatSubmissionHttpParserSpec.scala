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

package httpparsers

import httpparsers.VatSubmissionHttpParser._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http._

class VatSubmissionHttpParserSpec extends PlaySpec {

  val testHttpVerb     = "POST"
  val testUri          = "/"
  val testFormBundleId = "testFormBundleId"
  val testResponse     = Json.obj("formBundle" -> testFormBundleId)

  "VatSubmissionHttpParser" should {
    "successfully parse the formBundleId with status OK" in {
      VatSubmissionHttpReads.read(testHttpVerb, testUri, HttpResponse(OK, Json.stringify(testResponse))) mustBe Right(
        VatSubmissionSuccess(testFormBundleId)
      )
    }

    "fail when status OK does not contain a formBundleId" in {
      VatSubmissionHttpReads.read(testHttpVerb, testUri, HttpResponse(OK, "{}")) mustBe Left(
        VatSubmissionFailure(OK, "VAT submission API - no form bundle ID in response")
      )
    }

    "fail when parsing the response with status BAD_REQUEST" when {
      "the code is INVALID_PAYLOAD" in {
        val jsonBody = Json.obj(failuresKey -> Json.arr(Json.obj(codeKey -> invalidPayloadKey))).toString()

        VatSubmissionHttpReads.read(testHttpVerb, testUri, HttpResponse(BAD_REQUEST, jsonBody)) mustBe
          Left(VatSubmissionFailure(BAD_REQUEST, "VAT Submission API - errors: Invalid Payload"))
      }

      "the code is INVALID_CREDENTIALID" in {
        val jsonBody = Json.obj(failuresKey -> Json.arr(Json.obj(codeKey -> invalidCredentialIdKey))).toString()

        VatSubmissionHttpReads.read(testHttpVerb, testUri, HttpResponse(BAD_REQUEST, jsonBody)) mustBe
          Left(VatSubmissionFailure(BAD_REQUEST, "VAT Submission API - errors: Invalid Credential ID"))
      }

      "the code is INVALID_SESSIONID" in {
        val jsonBody = Json.obj(failuresKey -> Json.arr(Json.obj(codeKey -> invalidSessionIdKey))).toString()

        VatSubmissionHttpReads.read(testHttpVerb, testUri, HttpResponse(BAD_REQUEST, jsonBody)) mustBe
          Left(VatSubmissionFailure(BAD_REQUEST, "VAT Submission API - errors: Invalid Session ID"))
      }

      "the code is INVALID_CORRELATIONID" in {
        val jsonBody = Json.obj(failuresKey -> Json.arr(Json.obj(codeKey -> invalidCorrelationIdKey))).toString()

        VatSubmissionHttpReads.read(testHttpVerb, testUri, HttpResponse(BAD_REQUEST, jsonBody)) mustBe
          Left(VatSubmissionFailure(BAD_REQUEST, "VAT Submission API - errors: Invalid Correlation ID"))
      }

      "there are multiple codes" in {
        val jsonBody = Json
          .obj(
            failuresKey -> Json.arr(
              Json.obj(codeKey -> invalidPayloadKey),
              Json.obj(codeKey -> invalidCredentialIdKey),
              Json.obj(codeKey -> invalidSessionIdKey),
              Json.obj(codeKey -> invalidCorrelationIdKey),
              Json.obj(codeKey -> "error")
            )
          )
          .toString()

        VatSubmissionHttpReads.read(testHttpVerb, testUri, HttpResponse(BAD_REQUEST, jsonBody)) mustBe
          Left(
            VatSubmissionFailure(
              BAD_REQUEST,
              "VAT Submission API - errors: Invalid Payload, Invalid Credential ID, Invalid Session ID," +
                " Invalid Correlation ID, Unexpected Bad Request error reason"
            )
          )
      }
    }

    "throw an INTERNAL SERVER EXCEPTION" when {
      "the VAT Submission API returns an unexpected response" in {
        VatSubmissionHttpReads.read(testHttpVerb, testUri, HttpResponse(INTERNAL_SERVER_ERROR, "{}")) mustBe Left(
          VatSubmissionFailure(INTERNAL_SERVER_ERROR, "Unexpected response from VAT Submission API - status = 500")
        )
      }
    }

  }

}
