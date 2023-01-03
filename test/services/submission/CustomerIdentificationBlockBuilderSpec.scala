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
import models.OverseasIdentifierDetails
import models.api._
import models.submission.{Individual, NETP}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.InternalServerException

class CustomerIdentificationBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service: CustomerIdentificationBlockBuilder = new CustomerIdentificationBlockBuilder
  }

  lazy val customerIdentificationBlockWithBPJson: JsObject = Json.parse(
    """
      |{
      |    "tradingName": "trading-name",
      |    "tradersPartyType": "50",
      |    "primeBPSafeID": "testBpSafeId"
      |}
      |""".stripMargin).as[JsObject]

  def customerIdentificationBlockJson(idVerificationStatusCode: Int): JsObject = Json.parse(
    s"""
       |{
       |    "tradingName": "trading-name",
       |    "tradersPartyType": "50",
       |    "shortOrgName": "testCompanyName",
       |    "organisationName": "testCompanyName",
       |    "customerID": [
       |      {
       |        "idValue": "testUtr",
       |        "idType": "UTR",
       |        "IDsVerificationStatus": "$idVerificationStatusCode"
       |      },
       |      {
       |        "idValue": "testCrn",
       |        "idType": "CRN",
       |        "IDsVerificationStatus": "$idVerificationStatusCode",
       |        "date": "2020-01-02"
       |      }
       |    ]
       |}
       |""".stripMargin).as[JsObject]

  val soleTraderBlockJson = Json.obj(
    "tradersPartyType" -> "Z1",
    "tradingName" -> testTradingName,
    "customerID" -> Json.arr(
      Json.obj(
        "idType" -> "NINO",
        "idValue" -> testNino,
        "IDsVerificationStatus" -> "1"
      ),
      Json.obj(
        "idType" -> "UTR",
        "idValue" -> testUtr,
        "IDsVerificationStatus" -> "1"
      )
    ),
    "name" -> Json.obj(
      "firstName" -> testFirstName,
      "lastName" -> testLastName
    ),
    "dateOfBirth" -> testDateOfBirth
  )

  val netpBlockJson: JsObject = Json.obj(
    "tradersPartyType" -> "Z1",
    "tradingName" -> testTradingName,
    "customerID" -> Json.arr(
      Json.obj(
        "idType" -> "UTR",
        "idValue" -> testUtr,
        "IDsVerificationStatus" -> "1"
      ),
      Json.obj(
        "idType" -> "TEMPNI",
        "idValue" -> testTrn,
        "IDsVerificationStatus" -> "1"
      )
    ),
    "name" -> Json.obj(
      "firstName" -> testFirstName,
      "lastName" -> testLastName
    ),
    "dateOfBirth" -> testDateOfBirth
  )

  val netpBlockJsonWithOverseas: JsObject = Json.obj(
    "tradersPartyType" -> "Z1",
    "tradingName" -> testTradingName,
    "customerID" -> Json.arr(
      Json.obj(
        "idType" -> "UTR",
        "idValue" -> testUtr,
        "IDsVerificationStatus" -> "1"
      ),
      Json.obj(
        "idType" -> "TEMPNI",
        "idValue" -> testTrn,
        "IDsVerificationStatus" -> "1"
      ),
      Json.obj(
        "idType" -> "OTHER",
        "idValue" -> "1234",
        "countryOfIncorporation" -> "FR",
        "IDsVerificationStatus" -> "1"
      )
    ),
    "name" -> Json.obj(
      "firstName" -> testFirstName,
      "lastName" -> testLastName
    ),
    "dateOfBirth" -> testDateOfBirth
  )

  Json.parse(
    s"""
       |{
       |    "tradingName": "trading-name",
       |    "tradersPartyType": "Z1",
       |    "customerID": [
       |      {
       |        "idValue": "AB123456A",
       |        "idType": "NINO",
       |        "IDsVerificationStatus": "1"
       |      },
       |      {
       |        "idValue": "testUtr",
       |        "idType": "UTR",
       |        "IDsVerificationStatus": "1"
       |      }
       |    ]
       |}
       |""".stripMargin).as[JsObject]

  "buildCustomerIdentificationBlock" should {
    "build the correct json for a sole trader entity type with given business verification type" in new Setup {

      private def verifySoleTraderEntity(businessVerificationStatus: BusinessVerificationStatus) = {
        val appDetails = validApplicantDetails.copy(
          entity = Some(testSoleTraderEntity.copy(businessVerification = Some(businessVerificationStatus)))
        )
        val eligibilityData = testEligibilitySubmissionData.copy(partyType = Individual)
        val vatScheme = testFullVatScheme.copy(applicantDetails = Some(appDetails), eligibilitySubmissionData = Some(eligibilityData))

        val result: JsObject = service.buildCustomerIdentificationBlock(vatScheme)
        result mustBe soleTraderBlockJson
      }

      verifySoleTraderEntity(BvPass)
      verifySoleTraderEntity(BvUnchallenged)
      verifySoleTraderEntity(BvSaEnrolled)
    }

    "build the correct json for a NETP entity type" in new Setup {
      val appDetails = validApplicantDetails.copy(
        entity = Some(testSoleTraderEntity.copy(
          nino = None,
          trn = Some(testTrn)
        ))
      )
      val eligibilityData = testEligibilitySubmissionData.copy(partyType = NETP)
      val vatScheme = testFullVatScheme.copy(applicantDetails = Some(appDetails), eligibilitySubmissionData = Some(eligibilityData))

      val result: JsObject = service.buildCustomerIdentificationBlock(vatScheme)
      result mustBe netpBlockJson
    }

    "build the correct json for a NETP entity type with overseas details" in new Setup {
      val appDetails = validApplicantDetails.copy(
        entity = Some(testSoleTraderEntity.copy(
          nino = None,
          trn = Some(testTrn),
          overseas = Some(OverseasIdentifierDetails(
            taxIdentifier = "1234",
            country = "FR"
          ))
        ))
      )
      val eligibilityData = testEligibilitySubmissionData.copy(partyType = NETP)
      val vatScheme = testFullVatScheme.copy(applicantDetails = Some(appDetails), eligibilitySubmissionData = Some(eligibilityData))

      val result: JsObject = service.buildCustomerIdentificationBlock(vatScheme)
      result mustBe netpBlockJsonWithOverseas
    }

    "return Status Code 1" when {
      "the businessVerificationStatus is BvPass" in new Setup {
        val appDetails = validApplicantDetails.copy(entity = Some(testLtdCoEntity.copy(businessVerification = Some(BvPass), registration = FailedStatus)))
        val vatScheme = testFullVatScheme.copy(applicantDetails = Some(appDetails))

        val result: JsObject = service.buildCustomerIdentificationBlock(vatScheme)
        result mustBe customerIdentificationBlockJson(1)
      }
      "the businessVerificationStatus is CtEnrolled" in new Setup {
        val appDetails = validApplicantDetails.copy(entity = Some(testLtdCoEntity.copy(businessVerification = Some(BvCtEnrolled), registration = FailedStatus)))
        val vatScheme = testFullVatScheme.copy(applicantDetails = Some(appDetails))

        val result: JsObject = service.buildCustomerIdentificationBlock(vatScheme)
        result mustBe customerIdentificationBlockJson(1)
      }
    }
    "return Status Code 2" when {
      "the identifiersMatch is false" in new Setup {
        val appDetails = validApplicantDetails.copy(entity = Some(testLtdCoEntity.copy(businessVerification = Some(BvUnchallenged), identifiersMatch = false)))
        val vatScheme = testFullVatScheme.copy(applicantDetails = Some(appDetails))

        val result: JsObject = service.buildCustomerIdentificationBlock(vatScheme)
        result mustBe customerIdentificationBlockJson(2)
      }
    }
    "return Status Code 3" when {
      "businessVerification fails" in new Setup {
        val result: JsObject = service.buildCustomerIdentificationBlock(testFullVatScheme)
        result mustBe customerIdentificationBlockJson(3)
      }
      "businessVerification is not called" in new Setup {
        val appDetails = validApplicantDetails.copy(entity = Some(testLtdCoEntity.copy(businessVerification = Some(BvUnchallenged))))
        val vatScheme = testFullVatScheme.copy(applicantDetails = Some(appDetails))

        val result: JsObject = service.buildCustomerIdentificationBlock(vatScheme)
        result mustBe customerIdentificationBlockJson(3)
      }
    }
    "return the BP Safe ID" when {
      "businessVerificationStatus is Pass" in new Setup {
        val appDetails = validApplicantDetails.copy(entity = Some(testLtdCoEntity.copy(businessVerification = Some(BvPass), bpSafeId = Some(testBpSafeId))))
        val vatScheme = testFullVatScheme.copy(applicantDetails = Some(appDetails))

        val result: JsObject = service.buildCustomerIdentificationBlock(vatScheme)
        result mustBe customerIdentificationBlockWithBPJson
      }
      "businessVerification is CT-Enrolled" in new Setup {
        val appDetails = validApplicantDetails.copy(entity = Some(testLtdCoEntity.copy(businessVerification = Some(BvCtEnrolled), bpSafeId = Some(testBpSafeId))))
        val vatScheme = testFullVatScheme.copy(applicantDetails = Some(appDetails))

        val result: JsObject = service.buildCustomerIdentificationBlock(vatScheme)
        result mustBe customerIdentificationBlockWithBPJson
      }
    }
    "throw an Interval Server Exception" when {
      "applicant details is missing" in new Setup {
        val vatScheme = testFullVatScheme.copy(applicantDetails = None)

        intercept[InternalServerException](service.buildCustomerIdentificationBlock(vatScheme))
      }
      "business details is missing" in new Setup {
        val vatScheme = testFullVatScheme.copy(business = None)

        intercept[InternalServerException](service.buildCustomerIdentificationBlock(vatScheme))
      }
      "applicant details and trading details are missing" in new Setup {
        val vatScheme = testFullVatScheme.copy(applicantDetails = None, business = None)

        intercept[InternalServerException](service.buildCustomerIdentificationBlock(vatScheme))
      }
    }
  }
}
