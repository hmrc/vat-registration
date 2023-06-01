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

package services.submission

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.MockVatSchemeRepository
import models.api.{Contact, FormerName}
import models.submission.Other
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.InternalServerException

class DeclarationBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture with MockVatSchemeRepository {

  object TestBuilder extends DeclarationBlockBuilder

  val testApplicantDetails = validApplicantDetails.copy(changeOfName = FormerName())
  val declarationVatScheme = testVatScheme.copy(applicantDetails = Some(testApplicantDetails), confirmInformationDeclaration = Some(true))

  implicit val request: Request[_] = FakeRequest()

  "The declaration block builder" must {
    "return valid json" when {
      "the user has no previous address" in {
        val res = TestBuilder.buildDeclarationBlock(declarationVatScheme)

        res mustBe Json.parse(
          """{
            |  "declarationSigning": {
            |    "confirmInformationDeclaration": true,
            |    "declarationCapacity": "03"
            |  },
            |  "applicantDetails": {
            |    "roleInBusiness": "03",
            |    "name": {
            |      "firstName": "Forename",
            |      "lastName": "Surname"
            |    },
            |    "dateOfBirth": "2018-01-01",
            |    "currAddress": {
            |      "line1": "line1",
            |      "line2": "line2",
            |      "postCode": "ZZ1 1ZZ",
            |      "countryCode": "GB",
            |      "addressValidated": true
            |    },
            |    "commDetails": {
            |      "email": "skylake@vilikariet.com"
            |    },
            |    "identifiers": [
            |      {
            |        "idValue": "AB123456A",
            |        "idType": "NINO",
            |        "IDsVerificationStatus": "1",
            |        "date": "2018-01-01"
            |      }
            |    ]
            |  }
            |}""".stripMargin
        )
      }
      "the user has a previous address" in {
        val applicantDetails = testApplicantDetails.copy(previousAddress = Some(testAddress))
        val vatScheme = declarationVatScheme.copy(applicantDetails = Some(applicantDetails))

        val res = TestBuilder.buildDeclarationBlock(vatScheme)

        res mustBe Json.parse(
          """
            |{
            |  "declarationSigning": {
            |    "confirmInformationDeclaration": true,
            |    "declarationCapacity": "03"
            |  },
            |  "applicantDetails": {
            |    "roleInBusiness": "03",
            |    "name": {
            |      "firstName": "Forename",
            |      "lastName": "Surname"
            |    },
            |    "dateOfBirth": "2018-01-01",
            |    "currAddress": {
            |      "line1": "line1",
            |      "line2": "line2",
            |      "postCode": "ZZ1 1ZZ",
            |      "countryCode": "GB",
            |      "addressValidated": true
            |    },
            |    "prevAddress": {
            |      "line1": "line1",
            |      "line2": "line2",
            |      "postCode": "ZZ1 1ZZ",
            |      "countryCode": "GB",
            |      "addressValidated": true
            |    },
            |    "commDetails": {
            |      "email": "skylake@vilikariet.com"
            |    },
            |    "identifiers": [
            |      {
            |        "idValue": "AB123456A",
            |        "idType": "NINO",
            |        "IDsVerificationStatus": "1",
            |        "date": "2018-01-01"
            |      }
            |    ]
            |  }
            |}
            |""".stripMargin)
      }
      "the user has a previous name" in {
        val applicantDetails = testApplicantDetails.copy(changeOfName = testFormerName)
        val vatScheme = declarationVatScheme.copy(applicantDetails = Some(applicantDetails))

        val res = TestBuilder.buildDeclarationBlock(vatScheme)

        res mustBe Json.parse(
          """{
            |  "declarationSigning": {
            |    "confirmInformationDeclaration": true,
            |    "declarationCapacity": "03"
            |  },
            |  "applicantDetails": {
            |    "roleInBusiness": "03",
            |    "name": {
            |      "firstName": "Forename",
            |      "lastName": "Surname"
            |    },
            |    "prevName": {
            |      "firstName": "Forename",
            |      "lastName": "Surname",
            |      "nameChangeDate": "2018-01-01"
            |    },
            |    "dateOfBirth": "2018-01-01",
            |    "currAddress": {
            |      "line1": "line1",
            |      "line2": "line2",
            |      "postCode": "ZZ1 1ZZ",
            |      "countryCode": "GB",
            |      "addressValidated": true
            |    },
            |    "commDetails": {
            |      "email": "skylake@vilikariet.com"
            |    },
            |    "identifiers": [
            |      {
            |        "idValue": "AB123456A",
            |        "idType": "NINO",
            |        "IDsVerificationStatus": "1",
            |        "date": "2018-01-01"
            |      }
            |    ]
            |  }
            |}""".stripMargin
        )
      }
      "the user provides all contact details" in {
        val contactDetails = Contact(
          email = Some("skylake@vilikariet.com"),
          tel = Some("1234"),
          emailVerified = Some(true)
        )
        val applicantDetails = testApplicantDetails.copy(contact = contactDetails)
        val vatScheme = declarationVatScheme.copy(applicantDetails = Some(applicantDetails))

        val res = TestBuilder.buildDeclarationBlock(vatScheme)

        res mustBe Json.parse(
          """{
            |  "declarationSigning": {
            |    "confirmInformationDeclaration": true,
            |    "declarationCapacity": "03"
            |  },
            |  "applicantDetails": {
            |    "roleInBusiness": "03",
            |    "name": {
            |      "firstName": "Forename",
            |      "lastName": "Surname"
            |    },
            |    "dateOfBirth": "2018-01-01",
            |    "currAddress": {
            |      "line1": "line1",
            |      "line2": "line2",
            |      "postCode": "ZZ1 1ZZ",
            |      "countryCode": "GB",
            |      "addressValidated": true
            |    },
            |    "commDetails": {
            |      "email": "skylake@vilikariet.com",
            |      "telephone": "1234"
            |    },
            |    "identifiers": [
            |      {
            |        "idValue": "AB123456A",
            |        "idType": "NINO",
            |        "IDsVerificationStatus": "1",
            |        "date": "2018-01-01"
            |      }
            |    ]
            |  }
            |}""".stripMargin
        )
      }
      "the user provides all contact details and their role is 'Other'" in {
        val contactDetails = Contact(
          email = Some("skylake@vilikariet.com"),
          tel = Some("1234"),
          emailVerified = Some(true)
        )
        val applicantDetails = testApplicantDetails.copy(
          contact = contactDetails,
          roleInTheBusiness = Some(Other),
          otherRoleInTheBusiness = Some("testRole")
        )
        val vatScheme = declarationVatScheme.copy(applicantDetails = Some(applicantDetails))

        val res = TestBuilder.buildDeclarationBlock(vatScheme)

        res mustBe Json.parse(
          """{
            |  "declarationSigning": {
            |    "confirmInformationDeclaration": true,
            |    "declarationCapacity": "10",
            |    "capacityOther": "testRole"
            |  },
            |  "applicantDetails": {
            |    "roleInBusiness": "08",
            |    "otherRole": "testRole",
            |    "name": {
            |      "firstName": "Forename",
            |      "lastName": "Surname"
            |    },
            |    "dateOfBirth": "2018-01-01",
            |    "currAddress": {
            |      "line1": "line1",
            |      "line2": "line2",
            |      "postCode": "ZZ1 1ZZ",
            |      "countryCode": "GB",
            |      "addressValidated": true
            |    },
            |    "commDetails": {
            |      "email": "skylake@vilikariet.com",
            |      "telephone": "1234"
            |    },
            |    "identifiers": [
            |      {
            |        "idValue": "AB123456A",
            |        "idType": "NINO",
            |        "IDsVerificationStatus": "1",
            |        "date": "2018-01-01"
            |      }
            |    ]
            |  }
            |}""".stripMargin
        )
      }
    }
    "throw an exception if the user hasn't answered the declaration question" in {
      val vatScheme = testFullVatScheme.copy(confirmInformationDeclaration = None)

      val res = intercept[InternalServerException] {
        TestBuilder.buildDeclarationBlock(vatScheme)
      }

      res.getMessage mustBe "Could not construct declaration block because the following are missing: declaration"
    }
    "throw an exception if vat scheme doesn't contain applicant details" in {
      val vatScheme = testFullVatScheme.copy(applicantDetails = None)

      val res = intercept[InternalServerException] {
        TestBuilder.buildDeclarationBlock(vatScheme)
      }

      res.getMessage mustBe "Could not construct declaration block because the following are missing: applicantDetails"
    }
    "throw an exception when the VAT scheme is empty" in {
      intercept[InternalServerException] {
        TestBuilder.buildDeclarationBlock(testVatScheme)
      }
    }
  }

}
