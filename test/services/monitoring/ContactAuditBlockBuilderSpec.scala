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

package services.monitoring

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api._
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.InternalServerException

class ContactAuditBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  implicit val request: Request[_] = FakeRequest()

  object TestService extends ContactAuditBlockBuilder

  lazy val contactBlockMinimalJson: JsObject = Json
    .parse("""
      |{
      |     "address": {
      |      "line1": "line1",
      |      "line2": "line2"
      |    },
      |    "businessCommunicationDetails": {
      |      "telephone": "12345",
      |      "emailAddress": "email@email.com",
      |      "emailVerified": false,
      |      "preference": "ZEL"
      |    }
      |}
      |""".stripMargin)
    .as[JsObject]

  lazy val contactBlockFullJson: JsObject = Json
    .parse("""
      |{
      |     "address": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "line3": "line3",
      |      "line4": "line4",
      |      "line5": "line5",
      |      "postcode": "ZZ1 1ZZ",
      |      "countryCode": "GB"
      |    },
      |    "businessCommunicationDetails": {
      |      "telephone": "12345",
      |      "emailAddress": "email@email.com",
      |      "emailVerified": true,
      |      "webAddress": "www.foo.com",
      |      "preference": "ZEL"
      |    }
      |}
      |""".stripMargin)
    .as[JsObject]

  "buildContactBlock" should {
    "build a minimal contact json when all mandatory data is provided" in {
      val vatScheme = testVatScheme.copy(
        business = Some(
          testBusiness.copy(
            email = Some("email@email.com"),
            telephoneNumber = Some("12345"),
            website = None,
            ppobAddress = Some(Address("line1", Some("line2"), None, None, None, None, None)),
            contactPreference = Some(Email),
            hasWebsite = Some(false)
          )
        ),
        applicantDetails = Some(validApplicantDetails)
      )

      val res = TestService.buildContactBlock(vatScheme)

      res mustBe contactBlockMinimalJson
    }

    "build a full contact json when all data is provided" in {
      val vatScheme = testVatScheme.copy(
        business = Some(
          testBusiness.copy(
            email = Some("email@email.com"),
            telephoneNumber = Some("12345"),
            website = Some("www.foo.com"),
            ppobAddress = Some(
              Address(
                "line1",
                Some("line2"),
                Some("line3"),
                Some("line4"),
                Some("line5"),
                Some(testPostcode),
                Some(Country(Some("GB"), None))
              )
            ),
            contactPreference = Some(Email),
            hasWebsite = Some(true)
          )
        ),
        applicantDetails = Some(
          validApplicantDetails.copy(contact =
            Contact(
              email = Some("email@email.com"),
              emailVerified = Some(true)
            )
          )
        )
      )

      val res = TestService.buildContactBlock(vatScheme)

      res mustBe contactBlockFullJson
    }
  }

  "the business data block is missing in the vat scheme" should {
    "throw an exception" in {
      intercept[InternalServerException] {
        TestService.buildContactBlock(testVatScheme.copy(business = None))
      }
    }
  }

}
