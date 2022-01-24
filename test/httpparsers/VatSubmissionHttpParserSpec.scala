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

import httpparsers.VatSubmissionHttpParser.VatSubmissionHttpReads
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http._

class VatSubmissionHttpParserSpec extends PlaySpec {

  val testHttpVerb = "POST"
  val testUri = "/"
  val testFormBundleId = "testFormBundleId"
  val testResponse = Json.obj("formBundle" -> testFormBundleId)

  "VatSubmissionHttpParser" should {
    "successfully parse the formBundleId with status OK" in {
      VatSubmissionHttpReads.read(testHttpVerb, testUri, HttpResponse(OK, Json.stringify(testResponse))) mustBe Right(VatSubmissionSuccess(testFormBundleId))
    }

    "fail when status OK does not contain a formBundleId" in {
      VatSubmissionHttpReads.read(testHttpVerb, testUri, HttpResponse(OK, "{}")) mustBe Left(VatSubmissionFailure(OK, "VAT submission API - no form bundle ID in response"))
    }

    "fail when parsing the response with status BAD_REQUEST" when {
      "the code is INVALID_PAYLOAD" in {
        val jsonBody = Json.obj(VatSubmissionHttpParser.CodeKey -> VatSubmissionHttpParser.InvalidPayloadKey).toString()

        VatSubmissionHttpReads.read(testHttpVerb, testUri, HttpResponse(BAD_REQUEST, jsonBody)) mustBe Left(VatSubmissionFailure(BAD_REQUEST, "VAT Submission API - invalid payload"))
      }

      "the code is INVALID_CREDENTIALID" in {
        val jsonBody = Json.obj(VatSubmissionHttpParser.CodeKey -> VatSubmissionHttpParser.InvalidCredentialIdKey).toString()

        VatSubmissionHttpReads.read(testHttpVerb, testUri, HttpResponse(BAD_REQUEST, jsonBody)) mustBe Left(VatSubmissionFailure(BAD_REQUEST, "VAT Submission API - invalid Credential ID"))
      }

      "the code is INVALID_SESSIONID" in {
        val jsonBody = Json.obj(VatSubmissionHttpParser.CodeKey -> VatSubmissionHttpParser.InvalidSessionIdKey).toString()

        VatSubmissionHttpReads.read(testHttpVerb, testUri, HttpResponse(BAD_REQUEST, jsonBody)) mustBe Left(VatSubmissionFailure(BAD_REQUEST, "VAT Submission API - invalid Session ID"))
      }
    }

    "throw an INTERNAL SERVER EXCEPTION" when {
      "the VAT Submission API returns an unexpected response" in {
        VatSubmissionHttpReads.read(testHttpVerb, testUri, HttpResponse(INTERNAL_SERVER_ERROR, "{}")) mustBe Left(VatSubmissionFailure(INTERNAL_SERVER_ERROR, "Unexpected response from VAT Submission API - status = 500"))
      }
    }

  }

}
