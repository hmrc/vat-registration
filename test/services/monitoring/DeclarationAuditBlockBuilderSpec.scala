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

package services.monitoring

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.MockVatSchemeRepository
import models.api.{ApplicantDetails, DigitalContactOptional, VatScheme}
import play.api.libs.json.Json
import uk.gov.hmrc.http.InternalServerException

class DeclarationAuditBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture with MockVatSchemeRepository {

  object TestBuilder extends DeclarationAuditBlockBuilder

  val testApplicantDetails: ApplicantDetails = validApplicantDetails.copy(changeOfName = None)
  val declarationVatScheme: VatScheme = testVatScheme.copy(
    applicantDetails = Some(testApplicantDetails),
    confirmInformationDeclaration = Some(true)
  )

  "The declaration block builder" must {
    "return valid json" when {
      "the user has no previous address" in {
        val res = TestBuilder.buildDeclarationBlock(declarationVatScheme)

        res mustBe Json.parse(
          """{
            |  "declarationSigning": {
            |    "confirmInformationDeclaration": true,
            |    "declarationCapacity": "Director"
            |  },
            |  "applicant": {
            |    "roleInBusiness": "Director",
            |    "name": {
            |      "firstName": "Forename",
            |      "lastName": "Surname"
            |    },
            |    "currentAddress": {
            |      "line1": "line1",
            |      "line2": "line2",
            |      "postcode": "XX XX",
            |      "countryCode": "GB"
            |    },
            |    "dateOfBirth": "2018-01-01",
            |    "communicationDetails": {
            |      "emailAddress": "skylake@vilikariet.com"
            |    },
            |    "identifiers": {
            |      "nationalInsuranceNumber": "AB123456A"
            |    }
            |  }
            |}""".stripMargin
        )
      }
      "the user has a previous address" in {
        val applicantDetails = testApplicantDetails.copy(previousAddress = Some(testAddress))

        val res = TestBuilder.buildDeclarationBlock(declarationVatScheme.copy(applicantDetails = Some(applicantDetails)))

        res mustBe Json.parse(
          """
            |{
            |  "declarationSigning": {
            |    "confirmInformationDeclaration": true,
            |    "declarationCapacity": "Director"
            |  },
            |  "applicant": {
            |    "roleInBusiness": "Director",
            |    "name": {
            |      "firstName": "Forename",
            |      "lastName": "Surname"
            |    },
            |    "currentAddress": {
            |      "line1": "line1",
            |      "line2": "line2",
            |      "postcode": "XX XX",
            |      "countryCode": "GB"
            |    },
            |    "previousAddress": {
            |      "line1": "line1",
            |      "line2": "line2",
            |      "postcode": "XX XX",
            |      "countryCode": "GB"
            |    },
            |    "dateOfBirth": "2018-01-01",
            |    "communicationDetails": {
            |      "emailAddress": "skylake@vilikariet.com"
            |    },
            |    "identifiers": {
            |      "nationalInsuranceNumber": "AB123456A"
            |    }
            |  }
            |}
            |""".stripMargin)
      }
      "the user has a previous name" in {
        val applicantDetails = testApplicantDetails.copy(changeOfName = Some(testFormerName))

        val res = TestBuilder.buildDeclarationBlock(declarationVatScheme.copy(applicantDetails = Some(applicantDetails)))

        res mustBe Json.parse(
          """{
            |  "declarationSigning": {
            |    "confirmInformationDeclaration": true,
            |    "declarationCapacity": "Director"
            |  },
            |  "applicant": {
            |    "roleInBusiness": "Director",
            |    "name": {
            |      "firstName": "Forename",
            |      "lastName": "Surname"
            |    },
            |    "previousName": {
            |      "firstName": "Forename",
            |      "lastName": "Surname",
            |      "nameChangeDate": "2018-01-01"
            |    },
            |    "currentAddress": {
            |      "line1": "line1",
            |      "line2": "line2",
            |      "postcode": "XX XX",
            |      "countryCode": "GB"
            |    },
            |    "dateOfBirth": "2018-01-01",
            |    "communicationDetails": {
            |      "emailAddress": "skylake@vilikariet.com"
            |    },
            |    "identifiers": {
            |      "nationalInsuranceNumber": "AB123456A"
            |    }
            |  }
            |}""".stripMargin
        )
      }
      "the user provides all contact details" in {
        val contactDetails = DigitalContactOptional(
          email = Some("skylake@vilikariet.com"),
          tel = Some("1234"),
          mobile = Some("5678"),
          emailVerified = Some(true)
        )
        val applicantDetails = testApplicantDetails.copy(contact = contactDetails)

        val res = TestBuilder.buildDeclarationBlock(declarationVatScheme.copy(applicantDetails = Some(applicantDetails)))

        res mustBe Json.parse(
          """{
            |  "declarationSigning": {
            |    "confirmInformationDeclaration": true,
            |    "declarationCapacity": "Director"
            |  },
            |  "applicant": {
            |    "roleInBusiness": "Director",
            |    "name": {
            |      "firstName": "Forename",
            |      "lastName": "Surname"
            |    },
            |    "currentAddress": {
            |      "line1": "line1",
            |      "line2": "line2",
            |      "postcode": "XX XX",
            |      "countryCode": "GB"
            |    },
            |    "dateOfBirth": "2018-01-01",
            |    "communicationDetails": {
            |      "emailAddress": "skylake@vilikariet.com",
            |      "telephone": "1234",
            |      "mobileNumber": "5678"
            |    },
            |    "identifiers": {
            |      "nationalInsuranceNumber": "AB123456A"
            |    }
            |  }
            |}""".stripMargin
        )
      }
    }
    "throw an exception if the user hasn't answered the declaration question" in {
      mockGetVatScheme(testRegId)(Some(testVatScheme))

      intercept[InternalServerException](TestBuilder.buildDeclarationBlock(testVatScheme))
    }
  }

}
