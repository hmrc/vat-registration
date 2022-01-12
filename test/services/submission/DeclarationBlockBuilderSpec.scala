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

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.MockVatSchemeRepository
import models.api.DigitalContactOptional
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException

class DeclarationBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture with MockVatSchemeRepository {

  object TestBuilder extends DeclarationBlockBuilder(mockVatSchemeRepository)

  val testApplicantDetails = validApplicantDetails.copy(changeOfName = None)
  val declarationVatScheme = testVatScheme.copy(applicantDetails = Some(testApplicantDetails), confirmInformationDeclaration = Some(true))

  "The declaration block builder" must {
    "return valid json" when {
      "the user has no previous address" in {
        mockGetVatScheme(testRegId)(Some(declarationVatScheme))

        val res = await(TestBuilder.buildDeclarationBlock(testRegId))

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
            |      "postCode": "XX XX",
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
        mockGetVatScheme(testRegId)(Some(declarationVatScheme.copy(applicantDetails = Some(applicantDetails))))

        val res = await(TestBuilder.buildDeclarationBlock(testRegId))

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
            |      "postCode": "XX XX",
            |      "countryCode": "GB",
            |      "addressValidated": true
            |    },
            |    "prevAddress": {
            |      "line1": "line1",
            |      "line2": "line2",
            |      "postCode": "XX XX",
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
        val applicantDetails = testApplicantDetails.copy(changeOfName = Some(testFormerName))
        mockGetVatScheme(testRegId)(Some(declarationVatScheme.copy(applicantDetails = Some(applicantDetails))))

        val res = await(TestBuilder.buildDeclarationBlock(testRegId))

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
            |      "postCode": "XX XX",
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
        val contactDetails = DigitalContactOptional(
          email = Some("skylake@vilikariet.com"),
          tel = Some("1234"),
          mobile = Some("5678"),
          emailVerified = Some(true)
        )
        val applicantDetails = testApplicantDetails.copy(contact = contactDetails)
        mockGetVatScheme(testRegId)(Some(declarationVatScheme.copy(applicantDetails = Some(applicantDetails))))

        val res = await(TestBuilder.buildDeclarationBlock(testRegId))

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
            |      "postCode": "XX XX",
            |      "countryCode": "GB",
            |      "addressValidated": true
            |    },
            |    "commDetails": {
            |      "email": "skylake@vilikariet.com",
            |      "telephone": "1234",
            |      "mobileNumber": "5678"
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
      mockGetVatScheme(testRegId)(Some(testVatScheme))

      intercept[InternalServerException] {
        await(TestBuilder.buildDeclarationBlock(testRegId))
      }
    }
    "throw an exception when the VAT scheme is missing" in {
      mockGetVatScheme(testRegId)(None)

      intercept[InternalServerException] {
        await(TestBuilder.buildDeclarationBlock(testRegId))
      }
    }
  }

}
