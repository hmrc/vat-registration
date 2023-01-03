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

package fixtures

import enums.VatRegStatus
import models.submission.UkCompany
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation

trait SubmissionAuditFixture extends VatRegistrationFixture {

  val testFormBundleId = "testFormBundleId"

  val testAddressJson = Json.obj(
    "line1" -> "line1",
    "line2" -> "line2",
    "postcode" -> "ZZ1 1ZZ",
    "countryCode" -> "GB"
  )

  val bankAuditBlockJson: JsObject = Json.obj(
    "accountName" -> "testBankName",
    "sortCode" -> "01-02-03",
    "accountNumber" -> "01023456"
  )

  val complianceAuditBlockJson = Json.obj(
    "numOfWorkersSupplied" -> 1,
    "intermediaryArrangement" -> true,
    "supplyWorkers" -> true
  )

  lazy val contactBlockFullJson: JsObject =
    Json.obj(
      "address" -> testAddressJson,
      "businessCommunicationDetails" -> Json.obj(
        "telephone" -> "12345678910",
        "emailAddress" -> "test@test.com",
        "emailVerified" -> true,
        "webAddress" -> "www.foo.com",
        "preference" -> "ZEL"
      )
    )

  val customerIdentificationAuditBlockJson = Json.obj(
    "customerIdentification" -> Json.obj(
      "tradersPartyType" -> UkCompany.toString,
      "identifiers" -> Json.obj(
        "companyRegistrationNumber" -> testCrn,
        "ctUTR" -> testUtr
      ),
      "shortOrgName" -> testCompanyName,
      "dateOfBirth" -> testDateOfBirth,
      "tradingName" -> testTradingName
    )
  )

  val declarationAuditBlockJson =
    Json.obj(
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> "Director"
      ),
      "applicant" -> Json.obj(
        "roleInBusiness" -> "Director",
        "name" -> Json.obj(
          "firstName" -> testName.first,
          "lastName" -> testName.last
        ),
        "previousName" -> Json.obj(
          "firstName" -> testOldName.first,
          "lastName" -> testOldName.last,
          "nameChangeDate" -> testDate
        ),
        "currentAddress" -> testAddressJson,
        "previousAddress" -> testAddressJson,
        "dateOfBirth" -> testDate,
        "communicationDetails" -> Json.obj(
          "emailAddress" -> "skylake@vilikariet.com",
          "telephone" -> "1234567890"
        ),
        "identifiers" -> Json.obj(
          "nationalInsuranceNumber" -> testNino
        )
      )
    )

  val periodsAuditBlockJson = Json.obj(
    "customerPreferredPeriodicity" -> "MA"
  )

  val fullSubscriptionBlockJson: JsValue =
    Json.obj(
      "overThresholdIn12MonthPeriod" -> true,
      "overThresholdIn12MonthDate" -> testDate,
      "overThresholdInPreviousMonth" -> true,
      "overThresholdInPreviousMonthDate" -> testDate,
      "overThresholdInNextMonth" -> true,
      "overThresholdInNextMonthDate" -> testDate,
      "reasonForSubscription" -> Json.obj(
        "voluntaryOrEarlierDate" -> testDate,
        "exemptionOrException" -> "0"
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> 123456,
        "zeroRatedSupplies" -> 12.99,
        "vatRepaymentExpected" -> true
      ),
      "schemes" -> Json.obj(
        "startDate" -> testDate,
        "flatRateSchemeCategory" -> "123",
        "flatRateSchemePercentage" -> 15,
        "limitedCostTrader" -> false
      ),
      "businessActivities" -> Json.obj(
        "sicCodes" -> Json.obj(
          "primaryMainCode" -> "12345"
        ),
        "description" -> "the flu"
      )
    )

  val auditModelJson = Json.obj(
    "authProviderId" -> "1",
    "journeyId" -> testRegId,
    "userType" -> Organisation.toString,
    "messageType" -> "SubscriptionCreate",
    "customerStatus" -> VatRegStatus.submitted,
    "eoriRequested" -> true,
    "corporateBodyRegistered" -> Json.obj(
      "dateOfIncorporation" -> testDate,
      "countryOfIncorporation" -> testDate
    ),
    "idsVerificationStatus" -> "1",
    "cidVerification" -> "1",
    "userEnteredDetails" -> detailBlockAnswers
  )

  val detailBlockAnswers = Json.obj(
    "outsideEUSales" -> true,
    "subscription" -> fullSubscriptionBlockJson,
    "compliance" -> complianceAuditBlockJson,
    "declaration" -> declarationAuditBlockJson,
    "customerIdentification" -> customerIdentificationAuditBlockJson,
    "bankDetails" -> bankAuditBlockJson,
    "businessContact" -> contactBlockFullJson,
    "periods" -> periodsAuditBlockJson
  )

}
