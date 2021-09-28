
package itutil

import models.api.Submitted
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation

trait SubmissionAuditFixture extends ITVatSubmissionFixture {

  val testFullAddressJson = Json.obj(
    "line1" -> "line1",
    "line2" -> "line2",
    "line3" -> "line3",
    "line4" -> "line4",
    "line5" -> "line5",
    "postcode" -> "XX XX",
    "countryCode" -> "GB"
  )

  val bankAuditBlockJson: JsObject = Json.obj(
    "accountName" -> "testBankName",
    "sortCode" -> "11-11-11",
    "accountNumber" -> "01234567"
  )

  val complianceAuditBlockJson = Json.obj(
    "numOfWorkersSupplied" -> 1,
    "intermediaryArrangement" -> true,
    "supplyWorkers" -> true
  )

  lazy val contactBlockFullJson: JsObject =
    Json.obj(
      "address" -> testFullAddressJson,
      "businessCommunicationDetails" -> Json.obj(
        "telephone" -> "12345678910",
        "emailAddress" -> "test@test.com",
        "emailVerified" -> true,
        "webAddress" -> "www.foo.com",
        "preference" -> "ZEL"
      )
    )

  val customerIdentificationAuditBlockJson = Json.obj(
    "tradersPartyType" -> "50",
    "identifiers" -> Json.obj(
      "companyRegistrationNumber" -> testCrn,
      "ctUTR" -> testCtUtr
    ),
    "shortOrgName" -> testCompanyName,
    "dateOfBirth" -> testDateOfBirth,
    "tradingName" -> testTradingName
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
          "firstName" -> "Forename",
          "lastName" -> "Surname"
        ),
        "previousName" -> Json.obj(
          "firstName" -> "Bob",
          "lastName" -> "Smith",
          "nameChangeDate" -> "2017-01-01"
        ),
        "currentAddress" -> testFullAddressJson,
        "previousAddress" -> testFullAddressJson,
        "dateOfBirth" -> "2017-01-01",
        "communicationDetails" -> Json.obj(
          "emailAddress" -> "skylake@vilikariet.com",
          "telephone" -> "1234567890",
          "mobileNumber" -> "1234567890"
        ),
        "identifiers" -> Json.obj(
          "nationalInsuranceNumber" -> "NB686868C"
        )
      )
    )

  val periodsAuditBlockJson = Json.obj(
    "customerPreferredPeriodicity" -> "YA"
  )

  val fullSubscriptionBlockJson: JsValue =
    Json.obj(
      "overThresholdIn12MonthPeriod" -> true,
      "overThresholdIn12MonthDate" -> "2017-01-01",
      "overThresholdInPreviousMonth" -> true,
      "overThresholdInPreviousMonthDate" -> "2017-01-01",
      "overThresholdInNextMonth" -> true,
      "overThresholdInNextMonthDate" -> "2017-01-01",
      "reasonForSubscription" -> Json.obj(
        "voluntaryOrEarlierDate" -> "2017-01-01",
        "exemptionOrException" -> "0"
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> 123456,
        "zeroRatedSupplies" -> 12.99,
        "vatRepaymentExpected" -> true,
        "goodsFromOtherEU" -> testTurnover,
        "goodsSoldToOtherEU" -> testTurnover
      ),
      "schemes" -> Json.obj(
        "startDate" -> "2017-01-01",
        "flatRateSchemeCategory" -> "123",
        "flatRateSchemePercentage" -> 15,
        "limitedCostTrader" -> false
      ),
      "businessActivities" -> Json.obj(
        "sicCodes" -> Json.obj(
          "primaryMainCode" -> "12345",
          "mainCode2" -> "00002",
          "mainCode3" -> "00003",
          "mainCode4" -> "00004"
        ),
        "description" -> "businessDesc"
      )
    )

  val fullAnnualAccountingBlockJson: JsValue =
    Json.obj("submissionType" -> "1",
      "customerRequest" -> Json.obj(
        "paymentMethod" -> "01",
        "annualStagger" -> "YA",
        "paymentFrequency" -> "M",
        "estimatedTurnover" -> 123456,
        "reqStartDate" -> "2017-01-01"
      )
    )

  val auditModelJson = Json.obj(
    "authProviderId" -> testAuthProviderId,
    "journeyId" -> vatScheme.id,
    "userType" -> Organisation.toString,
    "messageType" -> "SubscriptionCreate",
    "customerStatus" -> Submitted.toString,
    "eoriRequested" -> true,
    "corporateBodyRegistered" -> Json.obj(
      "dateOfIncorporation" -> "2017-01-01",
      "countryOfIncorporation" -> "2017-01-01"
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
    "periods" -> periodsAuditBlockJson,
    "joinAA" -> fullAnnualAccountingBlockJson
  )

}
