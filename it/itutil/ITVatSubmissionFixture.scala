
package itutil

import models.api.vatapplication.{StoringGoodsForDispatch, StoringWithinUk}
import models.api.{AttachmentMethod, AttachmentType, Post}
import models.submission._
import models.{ForwardLook, RegistrationReason}
import play.api.libs.json.{JsArray, JsObject, Json}

trait ITVatSubmissionFixture extends ITFixtures {

  val testSubmissionJson: JsObject = Json.obj(
    "messageType" -> "SubscriptionCreate",
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2"
      ),
      "attachments" -> Json.obj(
        "EORIrequested" -> true
      )
    ),
    "customerIdentification" -> Json.obj(
      "tradersPartyType" -> "50",
      "customerID" -> Json.arr(
        Json.obj(
          "idType" -> "UTR",
          "idValue" -> testCtUtr,
          "IDsVerificationStatus" -> "3"
        ),
        Json.obj(
          "idType" -> "CRN",
          "idValue" -> testCrn,
          "date" -> testDateOfIncorp,
          "IDsVerificationStatus" -> "3"
        )
      ),
      "shortOrgName" -> testCompanyName,
      "organisationName" -> testCompanyName,
      "tradingName" -> testTradingDetails.tradingName.get
    ),
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testFullAddress.line1,
        "line2" -> testFullAddress.line2,
        "line3" -> testFullAddress.line3,
        "line4" -> testFullAddress.line4,
        "line5" -> testFullAddress.line5,
        "postCode" -> testFullAddress.postcode,
        "countryCode" -> "GB",
        "addressValidated" -> true
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testBusiness.telephoneNumber,
        "email" -> testBusiness.email,
        "emailVerified" -> false,
        "commsPreference" -> "ZEL",
        "webAddress" -> testBusiness.website
      )
    ),
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason" -> "0016",
        "relevantDate" -> testDate,
        "voluntaryOrEarlierDate" -> testDate,
        //For mandatory users - voluntary is optionally provided by the user
        //For voluntary users - relevant date = voluntaryOrEarlierDate
        "exemptionOrException" -> "0"
      ),
      "corporateBodyRegistered" -> Json.obj(
        "companyRegistrationNumber" -> testCrn,
        "dateOfIncorporation" -> testDateOfIncorp,
        "countryOfIncorporation" -> "GB"
      ),
      "businessActivities" -> Json.obj(
        "description" -> testBusiness.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testBusiness.mainBusinessActivity.map(_.id),
          "mainCode2" -> testSicCode2,
          "mainCode3" -> testSicCode3
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testTurnover,
        "zeroRatedSupplies" -> 12.99,
        "VATRepaymentExpected" -> true,
        "goodsFromOtherEU" -> testTurnover,
        "goodsSoldToOtherEU" -> testTurnover
      ),
      "schemes" -> Json.obj(
        "FRSCategory" -> frsDetails.categoryOfBusiness.get,
        "FRSPercentage" -> frsDetails.percent,
        "startDate" -> frsDetails.startDate.get,
        "limitedCostTrader" -> frsDetails.limitedCostTrader.get
      )
    ),
    "periods" -> Json.obj(
      "customerPreferredPeriodicity" -> "YA"
    ),
    "bankDetails" -> Json.obj(
      "UK" -> Json.obj(
        "accountName" -> testBankDetails.name,
        "accountNumber" -> testBankDetails.number,
        "sortCode" -> testSubmittedSortCode
      )
    ),
    "joinAA" -> Json.obj(
      "submissionType" -> "1",
      "customerRequest" -> Json.obj(
        "paymentMethod" -> "01",
        "annualStagger" -> "YA",
        "paymentFrequency" -> "M",
        "estimatedTurnover" -> testTurnover,
        "reqStartDate" -> "2017-01-01"
      )
    ),
    "compliance" -> Json.obj(
      "supplyWorkers" -> testBusiness.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testBusiness.labourCompliance.get.numOfWorkersSupplied.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> "03",
        "name" -> Json.obj(
          "firstName" -> testName.first,
          "lastName" -> testName.last
        ),
        "prevName" -> Json.obj(
          "firstName" -> testFormerName.name.get.first,
          "lastName" -> testFormerName.name.get.last,
          "nameChangeDate" -> testDate
        ),
        "currAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        ),
        "prevAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        ),
        "commDetails" -> Json.obj(
          "email" -> testDigitalContactOptional.email.get,
          "telephone" -> testDigitalContactOptional.tel,
          "mobileNumber" -> testDigitalContactOptional.mobile
        ),
        "dateOfBirth" -> testDate,
        "identifiers" -> Json.arr(
          Json.obj(
            "date" -> testDate,
            "idType" -> "NINO",
            "idValue" -> testNino,
            "IDsVerificationStatus" -> "1"
          )
        )
      ),
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> "03" //currently defaulted company director
      )
    )
  )

  val testSubmissionJsonWithShortOrgName: JsObject = Json.obj(
    "messageType" -> "SubscriptionCreate",
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2"
      ),
      "attachments" -> Json.obj(
        "EORIrequested" -> true
      )
    ),
    "customerIdentification" -> Json.obj(
      "tradersPartyType" -> "50",
      "customerID" -> Json.arr(
        Json.obj(
          "idType" -> "UTR",
          "idValue" -> testCtUtr,
          "IDsVerificationStatus" -> "3"
        ),
        Json.obj(
          "idType" -> "CRN",
          "idValue" -> testCrn,
          "date" -> testDateOfIncorp,
          "IDsVerificationStatus" -> "3"
        )
      ),
      "shortOrgName" -> testShortOrgName,
      "organisationName" -> testCompanyName,
      "tradingName" -> testTradingDetails.tradingName.get
    ),
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testFullAddress.line1,
        "line2" -> testFullAddress.line2,
        "line3" -> testFullAddress.line3,
        "line4" -> testFullAddress.line4,
        "line5" -> testFullAddress.line5,
        "postCode" -> testFullAddress.postcode,
        "countryCode" -> "GB",
        "addressValidated" -> true
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testBusiness.telephoneNumber,
        "email" -> testBusiness.email,
        "emailVerified" -> false,
        "commsPreference" -> "ZEL",
        "webAddress" -> testBusiness.website
      )
    ),
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason" -> "0016",
        "relevantDate" -> testDate,
        "voluntaryOrEarlierDate" -> testDate,
        "exemptionOrException" -> "0"
      ),
      "corporateBodyRegistered" -> Json.obj(
        "companyRegistrationNumber" -> testCrn,
        "dateOfIncorporation" -> testDateOfIncorp,
        "countryOfIncorporation" -> "GB"
      ),
      "businessActivities" -> Json.obj(
        "description" -> testBusiness.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testBusiness.mainBusinessActivity.map(_.id),
          "mainCode2" -> testSicCode2,
          "mainCode3" -> testSicCode3
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testTurnover,
        "zeroRatedSupplies" -> 12.99,
        "VATRepaymentExpected" -> true,
        "goodsFromOtherEU" -> testTurnover,
        "goodsSoldToOtherEU" -> testTurnover
      ),
      "schemes" -> Json.obj(
        "FRSCategory" -> frsDetails.categoryOfBusiness.get,
        "FRSPercentage" -> frsDetails.percent,
        "startDate" -> frsDetails.startDate.get,
        "limitedCostTrader" -> frsDetails.limitedCostTrader.get
      )
    ),
    "periods" -> Json.obj(
      "customerPreferredPeriodicity" -> "YA"
    ),
    "bankDetails" -> Json.obj(
      "UK" -> Json.obj(
        "accountName" -> testBankDetails.name,
        "accountNumber" -> testBankDetails.number,
        "sortCode" -> testSubmittedSortCode
        // Missing bank account reason is being developed
      )
    ),
    "joinAA" -> Json.obj(
      "submissionType" -> "1",
      "customerRequest" -> Json.obj(
        "paymentMethod" -> "01",
        "annualStagger" -> "YA",
        "paymentFrequency" -> "M",
        "estimatedTurnover" -> testTurnover,
        "reqStartDate" -> "2017-01-01"
      )
    ),
    "compliance" -> Json.obj(
      "supplyWorkers" -> testBusiness.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testBusiness.labourCompliance.get.numOfWorkersSupplied.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> "03",
        "name" -> Json.obj(
          "firstName" -> testName.first,
          "lastName" -> testName.last
        ),
        "prevName" -> Json.obj(
          "firstName" -> testFormerName.name.get.first,
          "lastName" -> testFormerName.name.get.last,
          "nameChangeDate" -> testDate
        ),
        "currAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        ),
        "prevAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        ),
        "commDetails" -> Json.obj(
          "email" -> testDigitalContactOptional.email.get,
          "telephone" -> testDigitalContactOptional.tel,
          "mobileNumber" -> testDigitalContactOptional.mobile
        ),
        "dateOfBirth" -> testDate,
        "identifiers" -> Json.arr(
          Json.obj(
            "date" -> testDate,
            "idType" -> "NINO",
            "idValue" -> testNino,
            "IDsVerificationStatus" -> "1"
          )
        )
      ),
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> "03"
      )
    )
  )

  val testTransactorSubmissionJson: JsObject = Json.obj(
    "messageType" -> "SubscriptionCreate",
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2"
      ),
      "attachments" -> Json.obj(
        "EORIrequested" -> true
      )
    ),
    "customerIdentification" -> Json.obj(
      "tradersPartyType" -> "50",
      "customerID" -> Json.arr(
        Json.obj(
          "idType" -> "UTR",
          "idValue" -> testCtUtr,
          "IDsVerificationStatus" -> "3"
        ),
        Json.obj(
          "idType" -> "CRN",
          "idValue" -> testCrn,
          "date" -> testDateOfIncorp,
          "IDsVerificationStatus" -> "3"
        )
      ),
      "shortOrgName" -> testCompanyName,
      "organisationName" -> testCompanyName,
      "tradingName" -> testTradingDetails.tradingName.get
    ),
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testFullAddress.line1,
        "line2" -> testFullAddress.line2,
        "line3" -> testFullAddress.line3,
        "line4" -> testFullAddress.line4,
        "line5" -> testFullAddress.line5,
        "postCode" -> testFullAddress.postcode,
        "countryCode" -> "GB",
        "addressValidated" -> true
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testBusiness.telephoneNumber,
        "email" -> testBusiness.email,
        "emailVerified" -> false,
        "commsPreference" -> "ZEL",
        "webAddress" -> testBusiness.website
      )
    ),
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason" -> "0016",
        "relevantDate" -> testDate,
        "voluntaryOrEarlierDate" -> testDate,
        "exemptionOrException" -> "0"
      ),
      "corporateBodyRegistered" -> Json.obj(
        "companyRegistrationNumber" -> testCrn,
        "dateOfIncorporation" -> testDateOfIncorp,
        "countryOfIncorporation" -> "GB"
      ),
      "businessActivities" -> Json.obj(
        "description" -> testBusiness.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testBusiness.mainBusinessActivity.map(_.id),
          "mainCode2" -> testSicCode2,
          "mainCode3" -> testSicCode3
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testTurnover,
        "zeroRatedSupplies" -> 12.99,
        "VATRepaymentExpected" -> true,
        "goodsFromOtherEU" -> testTurnover,
        "goodsSoldToOtherEU" -> testTurnover
      ),
      "schemes" -> Json.obj(
        "FRSCategory" -> frsDetails.categoryOfBusiness.get,
        "FRSPercentage" -> frsDetails.percent,
        "startDate" -> frsDetails.startDate.get,
        "limitedCostTrader" -> frsDetails.limitedCostTrader.get
      )
    ),
    "periods" -> Json.obj(
      "customerPreferredPeriodicity" -> "YA"
    ),
    "bankDetails" -> Json.obj(
      "UK" -> Json.obj(
        "accountName" -> testBankDetails.name,
        "accountNumber" -> testBankDetails.number,
        "sortCode" -> testSubmittedSortCode
      )
    ),
    "joinAA" -> Json.obj(
      "submissionType" -> "1",
      "customerRequest" -> Json.obj(
        "paymentMethod" -> "01",
        "annualStagger" -> "YA",
        "paymentFrequency" -> "M",
        "estimatedTurnover" -> testTurnover,
        "reqStartDate" -> "2017-01-01"
      )
    ),
    "compliance" -> Json.obj(
      "supplyWorkers" -> testBusiness.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testBusiness.labourCompliance.get.numOfWorkersSupplied.get
    ),
    "declaration" -> Json.obj(
      "agentOrCapacitor" -> Json.obj(
        "individualName" -> Json.obj(
          "firstName" -> testFirstName,
          "lastName" -> testLastName
        ),
        "organisationName" -> testOrganisationName,
        "identification" -> Json.arr(
          Json.obj(
            "date" -> testDate,
            "idType" -> "NINO",
            "idValue" -> testNino,
            "IDsFailedOnlineVerification" -> "1"
          )
        ),
        "commDetails" -> Json.obj(
          "telephone" -> testTelephone,
          "email" -> testEmail
        ),
        "address" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        )
      ),
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> "03",
        "name" -> Json.obj(
          "firstName" -> testName.first,
          "lastName" -> testName.last
        ),
        "prevName" -> Json.obj(
          "firstName" -> testFormerName.name.get.first,
          "lastName" -> testFormerName.name.get.last,
          "nameChangeDate" -> testDate
        ),
        "currAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        ),
        "prevAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        ),
        "commDetails" -> Json.obj(
          "email" -> testDigitalContactOptional.email.get,
          "telephone" -> testDigitalContactOptional.tel,
          "mobileNumber" -> testDigitalContactOptional.mobile
        ),
        "dateOfBirth" -> testDate,
        "identifiers" -> Json.arr(
          Json.obj(
            "date" -> testDate,
            "idType" -> "NINO",
            "idValue" -> testNino,
            "IDsVerificationStatus" -> "1"
          )
        )
      ),
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> Json.toJson[DeclarationCapacity](AuthorisedEmployee)
      )
    )
  )

  val testRegisteredBusinessPartnerSubmissionJson: JsObject = Json.obj(
    "messageType" -> "SubscriptionCreate",
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2"
      ),
      "attachments" -> Json.obj(
        "EORIrequested" -> true
      )
    ),
    "customerIdentification" -> Json.obj(
      "tradersPartyType" -> "50",
      "primeBPSafeID" -> testBpSafeId,
      "tradingName" -> testTradingDetails.tradingName.get
    ),
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testFullAddress.line1,
        "line2" -> testFullAddress.line2,
        "line3" -> testFullAddress.line3,
        "line4" -> testFullAddress.line4,
        "line5" -> testFullAddress.line5,
        "postCode" -> testFullAddress.postcode,
        "countryCode" -> "GB",
        "addressValidated" -> true
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testBusiness.telephoneNumber,
        "email" -> testBusiness.email,
        "emailVerified" -> false,
        "commsPreference" -> "ZEL",
        "webAddress" -> testBusiness.website
      )
    ),
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason" -> "0016",
        "relevantDate" -> testDate,
        "voluntaryOrEarlierDate" -> testDate,
        "exemptionOrException" -> "0"
      ),
      "corporateBodyRegistered" -> Json.obj(
        "companyRegistrationNumber" -> testCrn,
        "dateOfIncorporation" -> testDateOfIncorp,
        "countryOfIncorporation" -> "GB"
      ),
      "businessActivities" -> Json.obj(
        "description" -> testBusiness.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testBusiness.mainBusinessActivity.map(_.id),
          "mainCode2" -> testSicCode2,
          "mainCode3" -> testSicCode3
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testTurnover,
        "zeroRatedSupplies" -> 12.99,
        "VATRepaymentExpected" -> true,
        "goodsFromOtherEU" -> testTurnover,
        "goodsSoldToOtherEU" -> testTurnover
      )
    ),
    "periods" -> Json.obj(
      "customerPreferredPeriodicity" -> "MA"
    ),
    "bankDetails" -> Json.obj(
      "UK" -> Json.obj(
        "reasonBankAccNotProvided" -> "1"
      )
    ),
    "compliance" -> Json.obj(
      "supplyWorkers" -> testBusiness.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testBusiness.labourCompliance.get.numOfWorkersSupplied.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> "03",
        "name" -> Json.obj(
          "firstName" -> testName.first,
          "lastName" -> testName.last
        ),
        "currAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "postCode" -> testFullAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        ),
        "commDetails" -> Json.obj(
          "email" -> testDigitalContactOptional.email.get,
          "telephone" -> testDigitalContactOptional.tel,
          "mobileNumber" -> testDigitalContactOptional.mobile
        ),
        "dateOfBirth" -> testDate,
        "identifiers" -> Json.arr(
          Json.obj(
            "date" -> testDate,
            "idType" -> "NINO",
            "idValue" -> testNino,
            "IDsVerificationStatus" -> "1"
          )
        )
      ),
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> "03"
      )
    )
  )

  val testVerifiedSoleTraderJson: JsObject = Json.obj(
    "messageType" -> "SubscriptionCreate",
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2"
      ),
      "attachments" -> Json.obj(
        "EORIrequested" -> true
      )
    ),
    "customerIdentification" -> Json.obj(
      "tradersPartyType" -> "Z1",
      "primeBPSafeID" -> testBpSafeId,
      "tradingName" -> testTradingDetails.tradingName.get
    ),
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testFullAddress.line1,
        "line2" -> testFullAddress.line2,
        "line3" -> testFullAddress.line3,
        "line4" -> testFullAddress.line4,
        "line5" -> testFullAddress.line5,
        "postCode" -> testFullAddress.postcode,
        "countryCode" -> "GB",
        "addressValidated" -> true
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testBusiness.telephoneNumber,
        "email" -> testBusiness.email,
        "emailVerified" -> false,
        "commsPreference" -> "ZEL",
        "webAddress" -> testBusiness.website
      )
    ),
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason" -> "0016",
        "relevantDate" -> testDate,
        "voluntaryOrEarlierDate" -> testDate,
        "exemptionOrException" -> "0"
      ),
      "businessActivities" -> Json.obj(
        "description" -> testBusiness.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testBusiness.mainBusinessActivity.map(_.id),
          "mainCode2" -> testSicCode2,
          "mainCode3" -> testSicCode3
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testTurnover,
        "zeroRatedSupplies" -> 12.99,
        "VATRepaymentExpected" -> true,
        "goodsFromOtherEU" -> testTurnover,
        "goodsSoldToOtherEU" -> testTurnover
      )
    ),
    "periods" -> Json.obj(
      "customerPreferredPeriodicity" -> "MA"
    ),
    "bankDetails" -> Json.obj(
      "UK" -> Json.obj(
        "reasonBankAccNotProvided" -> "1"
      )
    ),
    "compliance" -> Json.obj(
      "supplyWorkers" -> testBusiness.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testBusiness.labourCompliance.get.numOfWorkersSupplied.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> Json.toJson(OwnerProprietor)(DeclarationCapacity.toJsString),
        "name" -> Json.obj(
          "firstName" -> testName.first,
          "lastName" -> testName.last
        ),
        "currAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "postCode" -> testFullAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        ),
        "commDetails" -> Json.obj(
          "email" -> testDigitalContactOptional.email.get,
          "telephone" -> testDigitalContactOptional.tel,
          "mobileNumber" -> testDigitalContactOptional.mobile
        ),
        "dateOfBirth" -> testDate,
        "identifiers" -> Json.arr(
          Json.obj(
            "date" -> testDate,
            "idType" -> "NINO",
            "idValue" -> testNino,
            "IDsVerificationStatus" -> "1"
          )
        )
      ),
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> Json.toJson(OwnerProprietor)(DeclarationCapacity.toJsString)
      )
    )
  )

  val testNetpJson: JsObject = Json.obj(
    "messageType" -> "SubscriptionCreate",
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2",
        "overseasTrader" -> true
      ),
      "attachments" -> Json.obj(
        "identityEvidence" -> Json.toJson[AttachmentMethod](Post)
      )
    ),
    "customerIdentification" -> Json.obj(
      "tradersPartyType" -> "Z1",
      "customerID" -> Json.arr(
        Json.obj(
          "idType" -> "UTR",
          "idValue" -> testSaUtr,
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
      "dateOfBirth" -> testDateOfBirth,
      "tradingName" -> testTradingDetails.tradingName.get
    ),
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testFullAddress.line1,
        "line2" -> testFullAddress.line2,
        "line3" -> testFullAddress.line3,
        "line4" -> testFullAddress.line4,
        "line5" -> testFullAddress.line5,
        "postCode" -> testFullAddress.postcode,
        "countryCode" -> "EE",
        "addressValidated" -> false
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testBusiness.telephoneNumber,
        "email" -> testBusiness.email,
        "emailVerified" -> false,
        "commsPreference" -> "ZEL",
        "webAddress" -> testBusiness.website
      )
    ),
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason" -> "0003",
        "relevantDate" -> testDate,
        "exemptionOrException" -> "0"
      ),
      "businessActivities" -> Json.obj(
        "description" -> testBusiness.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testBusiness.mainBusinessActivity.map(_.id),
          "mainCode2" -> testSicCode2,
          "mainCode3" -> testSicCode3
        ),
        "goodsToOverseas" -> true,
        "goodsToCustomerEU" -> true,
        "storingGoodsForDispatch" -> Json.toJson[StoringGoodsForDispatch](StoringWithinUk),
        "fulfilmentWarehouse" -> true,
        "FHDDSWarehouseNumber" -> testWarehouseNumber,
        "nameOfWarehouse" -> testWarehouseName
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testTurnover,
        "zeroRatedSupplies" -> 12.99,
        "VATRepaymentExpected" -> true
      )
    ),
    "periods" -> Json.obj(
      "customerPreferredPeriodicity" -> "MA"
    ),
    "bankDetails" -> Json.obj(
      "UK" -> Json.obj(
        "reasonBankAccNotProvided" -> "3"
      )
    ),
    "compliance" -> Json.obj(
      "supplyWorkers" -> testBusiness.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testBusiness.labourCompliance.get.numOfWorkersSupplied.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> Json.toJson(OwnerProprietor)(DeclarationCapacity.toJsString),
        "name" -> Json.obj(
          "firstName" -> testName.first,
          "lastName" -> testName.last
        ),
        "currAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testFullAddress.postcode,
          "countryCode" -> "EE",
          "addressValidated" -> false
        ),
        "commDetails" -> Json.obj(
          "email" -> testDigitalContactOptional.email.get,
          "telephone" -> testDigitalContactOptional.tel,
          "mobileNumber" -> testDigitalContactOptional.mobile
        ),
        "dateOfBirth" -> testDate
      ),
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> Json.toJson(OwnerProprietor)(DeclarationCapacity.toJsString)
      )
    )
  )

  val testNonUkCompanyJson: JsObject = Json.obj(
    "messageType" -> "SubscriptionCreate",
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2",
        "overseasTrader" -> true
      ),
      "attachments" -> Json.obj(
        "identityEvidence" -> Json.toJson[AttachmentMethod](Post)
      )
    ),
    "customerIdentification" -> Json.obj(
      "tradersPartyType" -> "55",
      "customerID" -> Json.arr(
        Json.obj(
          "idType" -> "UTR",
          "idValue" -> testCtUtr,
          "IDsVerificationStatus" -> "3"
        ),
        Json.obj(
          "countryOfIncorporation" -> "FR",
          "IDsVerificationStatus" -> "3",
          "idType" -> "OTHER",
          "idValue" -> "1234"
        )
      ),
      "tradingName" -> testTradingDetails.tradingName.get,
      "shortOrgName" -> testCompanyName,
      "organisationName" -> testCompanyName
    ),
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testFullAddress.line1,
        "line2" -> testFullAddress.line2,
        "line3" -> testFullAddress.line3,
        "line4" -> testFullAddress.line4,
        "line5" -> testFullAddress.line5,
        "postCode" -> testFullAddress.postcode,
        "countryCode" -> "EE",
        "addressValidated" -> false
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testBusiness.telephoneNumber,
        "email" -> testBusiness.email,
        "emailVerified" -> false,
        "commsPreference" -> "ZEL",
        "webAddress" -> testBusiness.website
      )
    ),
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason" -> "0003",
        "relevantDate" -> testDate,
        "exemptionOrException" -> "0"
      ),
      "businessActivities" -> Json.obj(
        "description" -> testBusiness.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testBusiness.mainBusinessActivity.map(_.id),
          "mainCode2" -> testSicCode2,
          "mainCode3" -> testSicCode3
        ),
        "goodsToOverseas" -> true,
        "goodsToCustomerEU" -> true,
        "storingGoodsForDispatch" -> Json.toJson[StoringGoodsForDispatch](StoringWithinUk),
        "fulfilmentWarehouse" -> true,
        "FHDDSWarehouseNumber" -> testWarehouseNumber,
        "nameOfWarehouse" -> testWarehouseName
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testTurnover,
        "zeroRatedSupplies" -> 12.99,
        "VATRepaymentExpected" -> true
      )
    ),
    "periods" -> Json.obj(
      "customerPreferredPeriodicity" -> "MA"
    ),
    "bankDetails" -> Json.obj(
      "UK" -> Json.obj(
        "reasonBankAccNotProvided" -> "3"
      )
    ),
    "compliance" -> Json.obj(
      "supplyWorkers" -> testBusiness.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testBusiness.labourCompliance.get.numOfWorkersSupplied.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> Json.toJson(Director)(DeclarationCapacity.toJsString),
        "name" -> Json.obj(
          "firstName" -> testName.first,
          "lastName" -> testName.last
        ),
        "currAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testFullAddress.postcode,
          "countryCode" -> "EE",
          "addressValidated" -> false
        ),
        "commDetails" -> Json.obj(
          "email" -> testDigitalContactOptional.email.get,
          "telephone" -> testDigitalContactOptional.tel,
          "mobileNumber" -> testDigitalContactOptional.mobile
        ),
        "dateOfBirth" -> testDate
      ),
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> Json.toJson(Director)(DeclarationCapacity.toJsString)
      )
    )
  )

  val testNetpJsonOverseas: JsObject = Json.obj(
    "messageType" -> "SubscriptionCreate",
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2",
        "overseasTrader" -> true
      ),
      "attachments" -> Json.obj(
        "identityEvidence" -> Json.toJson[AttachmentMethod](Post)
      )
    ),
    "customerIdentification" -> Json.obj(
      "tradersPartyType" -> "Z1",
      "customerID" -> Json.arr(
        Json.obj(
          "idType" -> "UTR",
          "idValue" -> testSaUtr,
          "IDsVerificationStatus" -> "3"
        ),
        Json.obj(
          "idType" -> "TEMPNI",
          "idValue" -> testTrn,
          "IDsVerificationStatus" -> "3"
        ),
        Json.obj(
          "idType" -> "OTHER",
          "idValue" -> "1234",
          "countryOfIncorporation" -> "FR",
          "IDsVerificationStatus" -> "3"
        )
      ),
      "name" -> Json.obj(
        "firstName" -> testFirstName,
        "lastName" -> testLastName
      ),
      "dateOfBirth" -> testDateOfBirth,
      "tradingName" -> testTradingDetails.tradingName.get
    ),
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testFullAddress.line1,
        "line2" -> testFullAddress.line2,
        "line3" -> testFullAddress.line3,
        "line4" -> testFullAddress.line4,
        "line5" -> testFullAddress.line5,
        "postCode" -> testFullAddress.postcode,
        "countryCode" -> "EE",
        "addressValidated" -> false
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testBusiness.telephoneNumber,
        "email" -> testBusiness.email,
        "emailVerified" -> false,
        "commsPreference" -> "ZEL",
        "webAddress" -> testBusiness.website
      )
    ),
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason" -> "0003",
        "relevantDate" -> testDate,
        "exemptionOrException" -> "0"
      ),
      "businessActivities" -> Json.obj(
        "description" -> testBusiness.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testBusiness.mainBusinessActivity.map(_.id),
          "mainCode2" -> testSicCode2,
          "mainCode3" -> testSicCode3
        ),
        "goodsToOverseas" -> true,
        "goodsToCustomerEU" -> true,
        "storingGoodsForDispatch" -> Json.toJson[StoringGoodsForDispatch](StoringWithinUk),
        "fulfilmentWarehouse" -> true,
        "FHDDSWarehouseNumber" -> testWarehouseNumber,
        "nameOfWarehouse" -> testWarehouseName
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testTurnover,
        "zeroRatedSupplies" -> 12.99,
        "VATRepaymentExpected" -> true
      )
    ),
    "periods" -> Json.obj(
      "customerPreferredPeriodicity" -> "MA"
    ),
    "bankDetails" -> Json.obj(
      "UK" -> Json.obj(
        "reasonBankAccNotProvided" -> "3"
      )
    ),
    "compliance" -> Json.obj(
      "supplyWorkers" -> testBusiness.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testBusiness.labourCompliance.get.numOfWorkersSupplied.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> Json.toJson(OwnerProprietor)(DeclarationCapacity.toJsString),
        "name" -> Json.obj(
          "firstName" -> testName.first,
          "lastName" -> testName.last
        ),
        "currAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testFullAddress.postcode,
          "countryCode" -> "EE",
          "addressValidated" -> false
        ),
        "commDetails" -> Json.obj(
          "email" -> testDigitalContactOptional.email.get,
          "telephone" -> testDigitalContactOptional.tel,
          "mobileNumber" -> testDigitalContactOptional.mobile
        ),
        "dateOfBirth" -> testDate
      ),
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> Json.toJson(OwnerProprietor)(DeclarationCapacity.toJsString)
      )
    )
  )

  val testVerifiedSoleTraderJsonWithUTR: JsObject = Json.obj(
    "messageType" -> "SubscriptionCreate",
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2"
      ),
      "attachments" -> Json.obj(
        "EORIrequested" -> true
      )
    ),
    "customerIdentification" -> Json.obj(
      "customerID" -> Json.arr(
        Json.obj(
          "idType" -> "NINO",
          "idValue" -> testNino,
          "IDsVerificationStatus" -> "1"
        ),
        Json.obj(
          "idType" -> "UTR",
          "idValue" -> testSaUtr,
          "IDsVerificationStatus" -> "1"
        )
      ),
      "name" -> Json.obj(
        "firstName" -> testFirstName,
        "lastName" -> testLastName
      ),
      "dateOfBirth" -> testDateOfBirth,
      "tradersPartyType" -> "Z1",
      "tradingName" -> testTradingDetails.tradingName.get
    ),
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testFullAddress.line1,
        "line2" -> testFullAddress.line2,
        "line3" -> testFullAddress.line3,
        "line4" -> testFullAddress.line4,
        "line5" -> testFullAddress.line5,
        "postCode" -> testFullAddress.postcode,
        "countryCode" -> "GB",
        "addressValidated" -> true
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testBusiness.telephoneNumber,
        "email" -> testBusiness.email,
        "emailVerified" -> false,
        "commsPreference" -> "ZEL",
        "webAddress" -> testBusiness.website
      )
    ),
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason" -> "0016",
        "relevantDate" -> testDate,
        "voluntaryOrEarlierDate" -> testDate,
        "exemptionOrException" -> "0"
      ),
      "businessActivities" -> Json.obj(
        "description" -> testBusiness.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testBusiness.mainBusinessActivity.map(_.id),
          "mainCode2" -> testSicCode2,
          "mainCode3" -> testSicCode3
        )
      ),
      "schemes" -> Json.obj(
        "FRSCategory" -> "123",
        "FRSPercentage" -> 15,
        "limitedCostTrader" -> false,
        "startDate" -> "2017-01-01"
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testTurnover,
        "zeroRatedSupplies" -> 12.99,
        "VATRepaymentExpected" -> true,
        "goodsFromOtherEU" -> testTurnover,
        "goodsSoldToOtherEU" -> testTurnover
      )
    ),
    "periods" -> Json.obj(
      "customerPreferredPeriodicity" -> "MA"
    ),
    "bankDetails" -> Json.obj(
      "UK" -> Json.obj(
        "accountName" -> "testBankName",
        "accountNumber" -> "01234567",
        "sortCode" -> "111111"
      )
    ),
    "compliance" -> Json.obj(
      "supplyWorkers" -> testBusiness.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testBusiness.labourCompliance.get.numOfWorkersSupplied.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> Json.toJson(OwnerProprietor)(DeclarationCapacity.toJsString),
        "name" -> Json.obj(
          "firstName" -> testName.first,
          "lastName" -> testName.last
        ),
        "currAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "postCode" -> testFullAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        ),
        "commDetails" -> Json.obj(
          "email" -> testDigitalContactOptional.email.get,
          "telephone" -> testDigitalContactOptional.tel,
          "mobileNumber" -> testDigitalContactOptional.mobile
        ),
        "dateOfBirth" -> testDate,
        "identifiers" -> Json.arr(
          Json.obj(
            "date" -> testDate,
            "idType" -> "NINO",
            "idValue" -> testNino,
            "IDsVerificationStatus" -> "1"
          )
        )
      ),
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> Json.toJson(OwnerProprietor)(DeclarationCapacity.toJsString)
      )
    )
  )

  val testVerifiedTrustJson: JsObject = Json.obj(
    "messageType" -> "SubscriptionCreate",
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2"
      ),
      "attachments" -> Json.obj(
        "EORIrequested" -> true
      )
    ),
    "customerIdentification" -> Json.obj(
      "tradersPartyType" -> "60",
      "primeBPSafeID" -> testBpSafeId,
      "tradingName" -> testTradingDetails.tradingName.get
    ),
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testFullAddress.line1,
        "line2" -> testFullAddress.line2,
        "line3" -> testFullAddress.line3,
        "line4" -> testFullAddress.line4,
        "line5" -> testFullAddress.line5,
        "postCode" -> testFullAddress.postcode,
        "countryCode" -> "GB",
        "addressValidated" -> true
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testBusiness.telephoneNumber,
        "email" -> testBusiness.email,
        "emailVerified" -> false,
        "commsPreference" -> "ZEL",
        "webAddress" -> testBusiness.website
      )
    ),
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason" -> "0016",
        "relevantDate" -> testDate,
        "voluntaryOrEarlierDate" -> testDate,
        "exemptionOrException" -> "0"
      ),
      "businessActivities" -> Json.obj(
        "description" -> testBusiness.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testBusiness.mainBusinessActivity.map(_.id),
          "mainCode2" -> testSicCode2,
          "mainCode3" -> testSicCode3
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testTurnover,
        "zeroRatedSupplies" -> 12.99,
        "VATRepaymentExpected" -> true,
        "goodsFromOtherEU" -> testTurnover,
        "goodsSoldToOtherEU" -> testTurnover
      )
    ),
    "periods" -> Json.obj(
      "customerPreferredPeriodicity" -> "MA"
    ),
    "bankDetails" -> Json.obj(
      "UK" -> Json.obj(
        "reasonBankAccNotProvided" -> "1"
      )
    ),
    "compliance" -> Json.obj(
      "supplyWorkers" -> testBusiness.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testBusiness.labourCompliance.get.numOfWorkersSupplied.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> Json.toJson(Director)(DeclarationCapacity.toJsString),
        "name" -> Json.obj(
          "firstName" -> testName.first,
          "lastName" -> testName.last
        ),
        "currAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "postCode" -> testFullAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        ),
        "commDetails" -> Json.obj(
          "email" -> testDigitalContactOptional.email.get,
          "telephone" -> testDigitalContactOptional.tel,
          "mobileNumber" -> testDigitalContactOptional.mobile
        ),
        "dateOfBirth" -> testDate,
        "identifiers" -> Json.arr(
          Json.obj(
            "date" -> testDate,
            "idType" -> "NINO",
            "idValue" -> testNino,
            "IDsVerificationStatus" -> "1"
          )
        )
      ),
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> Json.toJson(Director)(DeclarationCapacity.toJsString)
      )
    )
  )

  def testSubmissionJson(customerIdentification: JsObject, entities: Option[JsArray], regReason: RegistrationReason = ForwardLook, optSubscriptionBlock: Option[JsObject] = None, attachmentList: Set[AttachmentType] = Set()): JsObject = Json.obj(
    "messageType" -> "SubscriptionCreate",
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2"
      ),
      "attachments" -> (Json.obj(
        "EORIrequested" -> true
      ) ++ AttachmentType.submissionWrites(Post).writes(attachmentList).as[JsObject])
    ),
    "customerIdentification" -> customerIdentification,
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testFullAddress.line1,
        "line2" -> testFullAddress.line2,
        "line3" -> testFullAddress.line3,
        "line4" -> testFullAddress.line4,
        "line5" -> testFullAddress.line5,
        "postCode" -> testFullAddress.postcode,
        "countryCode" -> "GB",
        "addressValidated" -> true
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testBusiness.telephoneNumber,
        "email" -> testBusiness.email,
        "emailVerified" -> false,
        "commsPreference" -> "ZEL",
        "webAddress" -> testBusiness.website
      )
    ),
    "subscription" -> {
      Json.obj(
        "reasonForSubscription" -> Json.obj(
          "registrationReason" -> regReason,
          "relevantDate" -> testDate,
          "voluntaryOrEarlierDate" -> testDate,
          "exemptionOrException" -> "0"
        ),
        "businessActivities" -> Json.obj(
          "description" -> testBusiness.businessDescription,
          "SICCodes" -> Json.obj(
            "primaryMainCode" -> testBusiness.mainBusinessActivity.map(_.id),
            "mainCode2" -> testSicCode2,
            "mainCode3" -> testSicCode3
          )
        ),
        "yourTurnover" -> Json.obj(
          "turnoverNext12Months" -> testTurnover,
          "zeroRatedSupplies" -> 12.99,
          "VATRepaymentExpected" -> true,
          "goodsFromOtherEU" -> testTurnover,
          "goodsSoldToOtherEU" -> testTurnover
        ),
        "schemes" -> Json.obj(
          "FRSCategory" -> "123",
          "FRSPercentage" -> 15,
          "startDate" -> "2017-01-01",
          "limitedCostTrader" -> false
        )
      ) ++ {
        optSubscriptionBlock match {
          case Some(json) => json
          case None => Json.obj()
        }
      }
    },
    "periods" -> Json.obj(
      "customerPreferredPeriodicity" -> "MA"
    ),
    "bankDetails" -> Json.obj(
      "UK" -> Json.obj(
        "accountName" -> "testBankName",
        "sortCode" -> "111111",
        "accountNumber" -> "01234567"
      )
    ),
    "compliance" -> Json.obj(
      "supplyWorkers" -> testBusiness.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testBusiness.labourCompliance.get.numOfWorkersSupplied,
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> "03",
        "name" -> Json.obj(
          "firstName" -> testName.first,
          "lastName" -> testName.last
        ),
        "prevName" -> Json.obj(
          "firstName" -> "Bob",
          "lastName" -> "Smith",
          "nameChangeDate" -> testDate
        ),
        "currAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testFullAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        ),
        "prevAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testFullAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true
        ),
        "commDetails" -> Json.obj(
          "email" -> testDigitalContactOptional.email,
          "telephone" -> testDigitalContactOptional.tel,
          "mobileNumber" -> testDigitalContactOptional.mobile
        ),
        "dateOfBirth" -> testDate,
        "identifiers" -> Json.arr(
          Json.obj(
            "date" -> testDate,
            "idType" -> "NINO",
            "idValue" -> testNino,
            "IDsVerificationStatus" -> "1"
          )
        )
      ),
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> "03"
      )
    )
  ) ++ {
    entities match {
      case Some(json) => Json.obj("entities" -> json)
      case None => Json.obj()
    }
  }

  val generalPartnershipCustomerId = Json.obj(
    "tradersPartyType" -> Json.toJson[PartyType](Partnership),
    "customerID" -> Json.arr(
      Json.obj(
        "idValue" -> "testUtr",
        "idType" -> "UTR",
        "IDsVerificationStatus" -> "3"
      )
    ),
    "tradingName" -> testTradingDetails.tradingName.get,
    "shortOrgName" -> testShortOrgName,
    "organisationName" -> testCompanyName
  )

  val limitedPartnershipCustomerId = Json.obj(
    "tradersPartyType" -> Json.toJson[PartyType](LtdPartnership),
    "customerID" -> Json.arr(
      Json.obj(
        "idValue" -> "testUtr",
        "idType" -> "UTR",
        "IDsVerificationStatus" -> "3"
      ),
      Json.obj(
        "idValue" -> testCrn,
        "date" -> testDateOfIncorp,
        "idType" -> "CRN",
        "IDsVerificationStatus" -> "3"
      )
    ),
    "tradingName" -> testTradingDetails.tradingName.get,
    "shortOrgName" -> testShortOrgName,
    "organisationName" -> testCompanyName
  )

  val limitedLiabilityPartnershipCustomerId = Json.obj(
    "tradersPartyType" -> Json.toJson[PartyType](LtdLiabilityPartnership),
    "customerID" -> Json.arr(
      Json.obj(
        "idValue" -> "testUtr",
        "idType" -> "UTR",
        "IDsVerificationStatus" -> "3"
      ),
      Json.obj(
        "idValue" -> testCrn,
        "date" -> testDateOfIncorp,
        "idType" -> "CRN",
        "IDsVerificationStatus" -> "3"
      )
    ),
    "tradingName" -> testTradingDetails.tradingName.get,
    "shortOrgName" -> testShortOrgName,
    "organisationName" -> testCompanyName
  )

  val vatGroupCustomerId = Json.obj(
    "tradersPartyType" -> Json.toJson[PartyType](TaxGroups),
    "tradingName" -> testTradingDetails.tradingName.get,
    "shortOrgName" -> testShortOrgName,
    "organisationName" -> testCompanyName
  )

  val ukCompanyLeadPartner = Json.arr(
    Json.obj(
      "action" -> "1",
      "entityType" -> Json.toJson[EntitiesArrayType](PartnerEntity),
      "tradersPartyType" -> Json.toJson[PartyType](UkCompany),
      "customerIdentification" -> Json.obj(
        "customerID" -> Json.arr(
          Json.obj(
            "idValue" -> testUtr,
            "idType" -> "UTR",
            "IDsVerificationStatus" -> "1"
          ),
          Json.obj(
            "idValue" -> testCrn,
            "date" -> testDateOfIncorp,
            "idType" -> "CRN",
            "IDsVerificationStatus" -> "1"
          )
        ),
        "shortOrgName" -> testCompanyName,
        "organisationName" -> testCompanyName
      ),
      "businessContactDetails" -> Json.obj(
        "address" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testFullAddress.postcode,
          "countryCode" -> "GB"
        ),
        "commDetails" -> Json.obj(
          "email" -> testBusiness.email,
          "telephone" -> testBusiness.telephoneNumber
        )
      )
    )
  )

  val ukCompanyLeadEntity = Json.arr(
    Json.obj(
      "action" -> "1",
      "entityType" -> Json.toJson[EntitiesArrayType](GroupRepMemberEntity),
      "tradersPartyType" -> Json.toJson[PartyType](UkCompany),
      "customerIdentification" -> Json.obj(
        "customerID" -> Json.arr(
          Json.obj(
            "idValue" -> testUtr,
            "idType" -> "UTR",
            "IDsVerificationStatus" -> "1"
          ),
          Json.obj(
            "idValue" -> testCrn,
            "date" -> testDateOfIncorp,
            "idType" -> "CRN",
            "IDsVerificationStatus" -> "1"
          )
        ),
        "shortOrgName" -> testCompanyName,
        "organisationName" -> testCompanyName
      ),
      "businessContactDetails" -> Json.obj(
        "address" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testFullAddress.postcode,
          "countryCode" -> "GB"
        ),
        "commDetails" -> Json.obj(
          "email" -> testDigitalContactOptional.email,
          "telephone" -> testDigitalContactOptional.tel,
          "mobileNumber" -> testDigitalContactOptional.mobile
        )
      )
    )
  )

  val soleTraderLeadPartner = Json.arr(
    Json.obj(
      "action" -> "1",
      "entityType" -> Json.toJson[EntitiesArrayType](PartnerEntity),
      "tradersPartyType" -> Json.toJson[PartyType](Individual),
      "customerIdentification" -> Json.obj(
        "customerID" -> Json.arr(
          Json.obj(
            "idValue" -> "NB686868C",
            "idType" -> "NINO",
            "IDsVerificationStatus" -> "1"
          ),
          Json.obj(
            "idValue" -> "testUtr",
            "idType" -> "UTR",
            "IDsVerificationStatus" -> "1"
          )
        ),
        "name" -> Json.obj(
          "firstName" -> testFirstName,
          "lastName" -> testLastName
        ),
        "dateOfBirth" -> testDate
      ),
      "businessContactDetails" -> Json.obj(
        "address" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testFullAddress.postcode,
          "countryCode" -> "GB"
        ),
        "commDetails" -> Json.obj(
          "email" -> testBusiness.email,
          "telephone" -> testBusiness.telephoneNumber
        )
      )
    )
  )

  val scottishPartnershipLeadPartner = Json.arr(
    Json.obj(
      "action" -> "1",
      "entityType" -> Json.toJson[EntitiesArrayType](PartnerEntity),
      "tradersPartyType" -> Json.toJson[PartyType](ScotPartnership),
      "customerIdentification" -> Json.obj(
        "customerID" -> Json.arr(
          Json.obj(
            "idValue" -> "testUtr",
            "idType" -> "UTR",
            "IDsVerificationStatus" -> "1"
          )
        ),
        "shortOrgName" -> testCompanyName,
        "organisationName" -> testCompanyName
      ),
      "businessContactDetails" -> Json.obj(
        "address" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testFullAddress.postcode,
          "countryCode" -> "GB"
        ),
        "commDetails" -> Json.obj(
          "email" -> testBusiness.email,
          "telephone" -> testBusiness.telephoneNumber
        )
      )
    )
  )

  val togcBlockJson = Json.obj(
    "takingOver" -> Json.obj(
      "prevOwnerName" -> testPreviousBusinessName,
      "prevOwnerVATNumber" -> testVrn,
      "keepPrevOwnerVATNo" -> true,
      "acceptTsAndCsForTOGCOrCOLE" -> true
    ),
    "corporateBodyRegistered" -> Json.obj(
      "companyRegistrationNumber" -> testCrn,
      "dateOfIncorporation" -> testDateOfIncorp,
      "countryOfIncorporation" -> "GB"
    )
  )

  val ukCompanyCustomerId = Json.obj(
    "tradersPartyType" -> Json.toJson[PartyType](UkCompany),
    "customerID" -> Json.arr(
      Json.obj(
        "idType" -> "UTR",
        "idValue" -> testUtr,
        "IDsVerificationStatus" -> "1"
      ),
      Json.obj(
        "idType" -> "CRN",
        "idValue" -> testCrn,
        "date" -> testDateOfIncorp,
        "IDsVerificationStatus" -> "1"
      )
    ),
    "organisationName" -> testCompanyName,
    "shortOrgName" -> testShortOrgName,
    "tradingName" -> testTradingDetails.tradingName.get
  )
}
