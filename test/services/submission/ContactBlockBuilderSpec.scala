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

package services.submission

import fixtures.{VatRegistrationFixture, VatSubmissionFixture}
import helpers.VatRegSpec
import models.api.DigitalContactOptional
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.InternalServerException

class ContactBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture with VatSubmissionFixture {

  class Setup {
    val service: ContactBlockBuilder = new ContactBlockBuilder
  }

  lazy val contactBlockJson: JsObject = Json.parse(
    """
      |{
      |    "commDetails": {
      |      "mobileNumber": "54321",
      |      "telephone": "12345",
      |      "email": "email@email.com",
      |      "emailVerified": true,
      |      "commsPreference": "ZEL"
      |    },
      |    "address": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "postCode": "ZZ1 1ZZ",
      |      "countryCode": "GB"
      |    }
      |}
      |""".stripMargin).as[JsObject]

  "ContactBlockBuilder" should {
    "return the built contact block" when {
      "business contact details are available" in new Setup {
        val vatScheme = testVatScheme.copy(
          businessContact = Some(validFullBusinessContact),
          applicantDetails = Some(validApplicantDetails.copy(
            contact = DigitalContactOptional(
              email = validFullBusinessContact.email,
              emailVerified = Some(true)
            )
          ))
        )

        val result: JsObject = service.buildContactBlock(vatScheme)
        result mustBe contactBlockJson
      }
    }

    "throw an Interval Server Exception" when {
      "contact details do not exist" in new Setup {
        intercept[InternalServerException](service.buildContactBlock(testVatScheme))
      }
    }
  }
}
