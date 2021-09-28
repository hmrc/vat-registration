
package itutil

import models.api.returns.{StoringGoodsForDispatch, StoringWithinUk}
import models.api.{AttachmentOptions, Post}
import models.submission._
import play.api.libs.json.{JsObject, Json}

trait ITVatSubmissionFixture extends ITFixtures {

  val testBusinessDescription = "testBusinessDescription"

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
      //No prime BP safe ID included as not registered on ETMP
      "shortOrgName" -> testCompanyName,
      //name, dateOfBirth not included as company
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
        "addressValidated" -> true //false if manually entered by user
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testContactDetails.tel,
        "mobileNumber" -> testContactDetails.mobile,
        "email" -> testContactDetails.email,
        //"webAddress" -> Do we need this?
        "commsPreference" -> "ZEL" //electronic
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
        "description" -> testSicAndCompliance.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testSicAndCompliance.mainBusinessActivity.id
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testEligibilitySubmissionData.estimates.turnoverEstimate,
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
        "estimatedTurnover" -> 123456.00,
        "reqStartDate" -> "2017-01-01"
      )
    ),
    "compliance" -> Json.obj(
      "supplyWorkers" -> testSicAndCompliance.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testSicAndCompliance.labourCompliance.get.numOfWorkersSupplied.get,
      "intermediaryArrangement" -> testSicAndCompliance.labourCompliance.get.intermediaryArrangement.get
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
        "addressValidated" -> true //false if manually entered by user
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testContactDetails.tel,
        "mobileNumber" -> testContactDetails.mobile,
        "email" -> testContactDetails.email,
        //"webAddress" -> Do we need this?
        "commsPreference" -> "ZEL" //electronic
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
        "description" -> testSicAndCompliance.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testSicAndCompliance.mainBusinessActivity.id
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testEligibilitySubmissionData.estimates.turnoverEstimate,
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
      "supplyWorkers" -> testSicAndCompliance.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testSicAndCompliance.labourCompliance.get.numOfWorkersSupplied.get,
      "intermediaryArrangement" -> testSicAndCompliance.labourCompliance.get.intermediaryArrangement.get
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
        "declarationCapacity" -> "03" //currently defaulted company director
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
        "telephone" -> testContactDetails.tel,
        "mobileNumber" -> testContactDetails.mobile,
        "email" -> testContactDetails.email,
        "commsPreference" -> "ZEL"
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
        "description" -> testSicAndCompliance.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testSicAndCompliance.mainBusinessActivity.id
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testEligibilitySubmissionData.estimates.turnoverEstimate,
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
      "supplyWorkers" -> testSicAndCompliance.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testSicAndCompliance.labourCompliance.get.numOfWorkersSupplied.get,
      "intermediaryArrangement" -> testSicAndCompliance.labourCompliance.get.intermediaryArrangement.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> Json.toJson(OwnerProprietor)(RoleInBusiness.toJsString),
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
        "declarationCapacity" -> Json.toJson(OwnerProprietor)(RoleInBusiness.toJsString)
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
        "identityEvidence" -> Json.toJson[AttachmentOptions](Post)
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
        "telephone" -> testContactDetails.tel,
        "mobileNumber" -> testContactDetails.mobile,
        "email" -> testContactDetails.email,
        "commsPreference" -> "ZEL"
      )
    ),
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason" -> "0003",
        "relevantDate" -> testDate,
        "exemptionOrException" -> "0"
      ),
      "businessActivities" -> Json.obj(
        "description" -> testSicAndCompliance.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testSicAndCompliance.mainBusinessActivity.id,
          "mainCode2" -> "00002",
          "mainCode3" -> "00003",
          "mainCode4" -> "00004"
        ),
        "goodsToOverseas" -> true,
        "goodsToCustomerEU" -> true,
        "storingGoodsForDispatch" -> Json.toJson[StoringGoodsForDispatch](StoringWithinUk),
        "fulfilmentWarehouse" -> true,
        "FHDDSWarehouseNumber" -> testWarehouseNumber,
        "nameOfWarehouse" -> testWarehouseName
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testEligibilitySubmissionData.estimates.turnoverEstimate,
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
      "supplyWorkers" -> testSicAndCompliance.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testSicAndCompliance.labourCompliance.get.numOfWorkersSupplied.get,
      "intermediaryArrangement" -> testSicAndCompliance.labourCompliance.get.intermediaryArrangement.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> Json.toJson(OwnerProprietor)(RoleInBusiness.toJsString),
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
        "dateOfBirth" -> testDate,
        "identifiers" -> Json.arr(
          Json.obj(
            "date" -> testDate,
            "idType" -> "TEMPNI",
            "idValue" -> testTrn,
            "IDsVerificationStatus" -> "2"
          )
        )
      ),
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true,
        "declarationCapacity" -> Json.toJson(OwnerProprietor)(RoleInBusiness.toJsString)
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
        "telephone" -> testContactDetails.tel,
        "mobileNumber" -> testContactDetails.mobile,
        "email" -> testContactDetails.email,
        "commsPreference" -> "ZEL"
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
        "description" -> testSicAndCompliance.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testSicAndCompliance.mainBusinessActivity.id,
          "mainCode2" -> "00002",
          "mainCode3" -> "00003",
          "mainCode4" -> "00004"
        )
      ),
      "schemes" -> Json.obj(
        "FRSCategory" -> "123",
        "FRSPercentage" -> 15,
        "limitedCostTrader" -> false,
        "startDate" -> "2017-01-01"
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testEligibilitySubmissionData.estimates.turnoverEstimate,
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
      "supplyWorkers" -> testSicAndCompliance.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testSicAndCompliance.labourCompliance.get.numOfWorkersSupplied.get,
      "intermediaryArrangement" -> testSicAndCompliance.labourCompliance.get.intermediaryArrangement.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> Json.toJson(OwnerProprietor)(RoleInBusiness.toJsString),
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
        "declarationCapacity" -> Json.toJson(OwnerProprietor)(RoleInBusiness.toJsString)
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
        "telephone" -> testContactDetails.tel,
        "mobileNumber" -> testContactDetails.mobile,
        "email" -> testContactDetails.email,
        "commsPreference" -> "ZEL"
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
        "description" -> testSicAndCompliance.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testSicAndCompliance.mainBusinessActivity.id
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testEligibilitySubmissionData.estimates.turnoverEstimate,
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
      "supplyWorkers" -> testSicAndCompliance.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testSicAndCompliance.labourCompliance.get.numOfWorkersSupplied.get,
      "intermediaryArrangement" -> testSicAndCompliance.labourCompliance.get.intermediaryArrangement.get
    ),
    "declaration" -> Json.obj(
      "applicantDetails" -> Json.obj(
        "roleInBusiness" -> Json.toJson(Director)(RoleInBusiness.toJsString),
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
        "declarationCapacity" -> Json.toJson(Director)(RoleInBusiness.toJsString)
      )
    )
  )

  val testVerifiedSoleTraderWithPartnerJson: JsObject = Json.obj(
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
      "tradersPartyType" -> "61",
      "tradingName" -> "trading-name",
      "customerID" -> Json.arr(
        Json.obj(
          "idValue" -> "testUtr",
          "idType" -> "UTR",
          "IDsVerificationStatus" -> "1"
        )
      ),
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
        "addressValidated" -> true //false if manually entered by user
      ),
      "commDetails" -> Json.obj(
        "telephone" -> testContactDetails.tel,
        "mobileNumber" -> testContactDetails.mobile,
        "email" -> testContactDetails.email,
        //"webAddress" -> Do we need this?
        "commsPreference" -> "ZEL" //electronic
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
      "businessActivities" -> Json.obj(
        "description" -> testSicAndCompliance.businessDescription,
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> testSicAndCompliance.mainBusinessActivity.id,
          "mainCode2" -> "00002",
          "mainCode3" -> "00003",
          "mainCode4" -> "00004"
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> testEligibilitySubmissionData.estimates.turnoverEstimate,
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
    ),
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
      "supplyWorkers" -> testSicAndCompliance.labourCompliance.get.supplyWorkers,
      "numOfWorkersSupplied" -> testSicAndCompliance.labourCompliance.get.numOfWorkersSupplied.get,
      "intermediaryArrangement" -> testSicAndCompliance.labourCompliance.get.intermediaryArrangement.get
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
          "addressValidated" -> true //false if manually entered by user
        ),
        "prevAddress" -> Json.obj(
          "line1" -> testFullAddress.line1,
          "line2" -> testFullAddress.line2,
          "line3" -> testFullAddress.line3,
          "line4" -> testFullAddress.line4,
          "line5" -> testFullAddress.line5,
          "postCode" -> testFullAddress.postcode,
          "countryCode" -> "GB",
          "addressValidated" -> true //false if manually entered by user
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
    ),
    "entities" -> Json.arr(
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
          )
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
            "telephone" -> testBusinessContactDetails.digitalContact.tel.get
          )
        )
      )
    )
  )
}
