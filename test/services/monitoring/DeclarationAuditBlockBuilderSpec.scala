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
import mocks.MockVatSchemeRepository
import models.api._
import models.submission.{AccountantAgent, Other}
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class DeclarationAuditBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture with MockVatSchemeRepository {

  object TestBuilder extends DeclarationAuditBlockBuilder

  implicit val request: Request[_] = FakeRequest()

  val testApplicantDetails: ApplicantDetails = validApplicantDetails.copy(changeOfName = FormerName())
  val declarationVatScheme: VatScheme        = testVatScheme.copy(
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
            |      "postcode": "ZZ1 1ZZ",
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
      "the user is an agent" in {
        val scheme = declarationVatScheme.copy(transactorDetails =
          Some(
            TransactorDetails(
              personalDetails = Some(
                PersonalDetails(
                  name = Name(Some(testFirstName), None, testLastName),
                  nino = None,
                  trn = None,
                  arn = Some(testArn),
                  identifiersMatch = true,
                  dateOfBirth = None,
                  score = None
                )
              ),
              telephone = Some(testTelephone),
              email = Some(testEmail),
              isPartOfOrganisation = None,
              organisationName = None,
              emailVerified = Some(true),
              address = None,
              declarationCapacity = Some(DeclarationCapacityAnswer(AccountantAgent))
            )
          )
        )
        val res    = TestBuilder.buildDeclarationBlock(scheme)

        res mustBe Json.parse(
          s"""{
             |  "declarationSigning": {
             |    "confirmInformationDeclaration": true,
             |    "declarationCapacity": "AccountantAgent"
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
             |      "postcode": "ZZ1 1ZZ",
             |      "countryCode": "GB"
             |    },
             |    "dateOfBirth": "2018-01-01",
             |    "communicationDetails": {
             |      "emailAddress": "skylake@vilikariet.com"
             |    },
             |    "identifiers": {
             |      "nationalInsuranceNumber": "AB123456A"
             |    }
             |  },
             |  "agentOrCapacitor": {
             |    "individualName": {
             |      "firstName": "$testFirstName",
             |      "lastName": "$testLastName"
             |    },
             |    "commDetails": {
             |      "telephone": "$testTelephone",
             |      "email": "$testEmail"
             |    },
             |    "identification": [
             |      {
             |        "idValue": "$testArn",
             |        "idType": "ARN",
             |        "IDsVerificationStatus": "1"
             |      }
             |    ]
             |  }
             |}""".stripMargin
        )
      }
      "the user has a previous address" in {
        val applicantDetails = testApplicantDetails.copy(previousAddress = Some(testAddress))

        val res =
          TestBuilder.buildDeclarationBlock(declarationVatScheme.copy(applicantDetails = Some(applicantDetails)))

        res mustBe Json.parse("""
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
            |      "postcode": "ZZ1 1ZZ",
            |      "countryCode": "GB"
            |    },
            |    "previousAddress": {
            |      "line1": "line1",
            |      "line2": "line2",
            |      "postcode": "ZZ1 1ZZ",
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
        val applicantDetails = testApplicantDetails.copy(changeOfName = testFormerName)

        val res =
          TestBuilder.buildDeclarationBlock(declarationVatScheme.copy(applicantDetails = Some(applicantDetails)))

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
            |      "postcode": "ZZ1 1ZZ",
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
        val contactDetails   = Contact(
          email = Some("skylake@vilikariet.com"),
          tel = Some("1234"),
          emailVerified = Some(true)
        )
        val applicantDetails = testApplicantDetails.copy(contact = contactDetails)

        val res =
          TestBuilder.buildDeclarationBlock(declarationVatScheme.copy(applicantDetails = Some(applicantDetails)))

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
            |      "postcode": "ZZ1 1ZZ",
            |      "countryCode": "GB"
            |    },
            |    "dateOfBirth": "2018-01-01",
            |    "communicationDetails": {
            |      "emailAddress": "skylake@vilikariet.com",
            |      "telephone": "1234"
            |    },
            |    "identifiers": {
            |      "nationalInsuranceNumber": "AB123456A"
            |    }
            |  }
            |}""".stripMargin
        )
      }
      "the user provides all contact details and their role is 'Other'" in {
        val contactDetails   = Contact(
          email = Some("skylake@vilikariet.com"),
          tel = Some("1234"),
          emailVerified = Some(true)
        )
        val applicantDetails = testApplicantDetails.copy(
          contact = contactDetails,
          roleInTheBusiness = Some(Other),
          otherRoleInTheBusiness = Some("testRole")
        )

        val res =
          TestBuilder.buildDeclarationBlock(declarationVatScheme.copy(applicantDetails = Some(applicantDetails)))

        res mustBe Json.parse(
          """{
            |  "declarationSigning": {
            |    "confirmInformationDeclaration": true,
            |    "declarationCapacity": "Other",
            |    "capacityOther": "testRole"
            |  },
            |  "applicant": {
            |    "roleInBusiness": "Other",
            |    "otherRole": "testRole",
            |    "name": {
            |      "firstName": "Forename",
            |      "lastName": "Surname"
            |    },
            |    "currentAddress": {
            |      "line1": "line1",
            |      "line2": "line2",
            |      "postcode": "ZZ1 1ZZ",
            |      "countryCode": "GB"
            |    },
            |    "dateOfBirth": "2018-01-01",
            |    "communicationDetails": {
            |      "emailAddress": "skylake@vilikariet.com",
            |      "telephone": "1234"
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
      mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testVatScheme)))

      intercept[InternalServerException](TestBuilder.buildDeclarationBlock(testVatScheme))
    }
  }

}
