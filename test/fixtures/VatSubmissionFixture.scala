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

package fixtures

import play.api.libs.json.{JsValue, Json}

trait VatSubmissionFixture {

  val vatSubmissionJsonWithBp: JsValue = Json.parse(
    """
      |{
      |  "messageType": "SubmissionCreate",
      |  "customerIdentification": {
      |    "tradingName": "trading-name",
      |    "tradersPartyType": "50",
      |    "primeBPSafeID": "testBpSafeId",
      |    "shortOrgName": "testCompanyName"
      |  },
      |  "declaration": {
      |    "declarationSigning": {
      |      "declarationCapacity": "03",
      |      "confirmInformationDeclaration": true
      |    },
      |    "applicantDetails": {
      |      "commDetails": {
      |        "email": "skylake@vilikariet.com"
      |      },
      |      "name": {
      |        "firstName": "Forename",
      |        "lastName": "Surname"
      |      },
      |      "dateOfBirth": "2018-01-01",
      |      "roleInBusiness": "03",
      |      "identifiers": [
      |        {
      |          "idValue": "AB123456A",
      |          "idType": "NINO",
      |          "IDsVerificationStatus": "1",
      |          "date": "2018-01-01"
      |        }
      |      ],
      |      "prevName": {
      |        "firstName": "Forename",
      |        "lastName": "Surname",
      |        "nameChangeDate": "2018-01-01"
      |      },
      |      "currAddress": {
      |        "line1": "line1",
      |        "line2": "line2",
      |        "postCode": "XX XX",
      |        "countryCode": "GB",
      |        "addressValidated": true
      |      }
      |    }
      |  },
      |  "subscription": {
      |    "corporateBodyRegistered": {
      |      "dateOfIncorporation": "2020-01-02",
      |      "companyRegistrationNumber": "testCrn",
      |      "countryOfIncorporation": "GB"
      |    },
      |    "reasonForSubscription": {
      |      "voluntaryOrEarlierDate": "2018-01-01",
      |      "relevantDate": "2020-10-07",
      |      "registrationReason": "0016",
      |      "exemptionOrException": "0"
      |    },
      |    "yourTurnover": {
      |      "VATRepaymentExpected": false,
      |      "turnoverNext12Months": 123456,
      |      "zeroRatedSupplies": 12.99
      |    },
      |    "schemes": {
      |      "startDate": "2018-01-01",
      |      "FRSCategory": "testCategory",
      |      "FRSPercentage": 15,
      |      "limitedCostTrader": false
      |    },
      |    "businessActivities": {
      |      "SICCodes": {
      |        "mainCode2": "00998",
      |        "primaryMainCode": "12345",
      |        "mainCode3": "00889"
      |      },
      |      "description": "this is my business description"
      |    }
      |  },
      |  "bankDetails": {
      |    "UK": {
      |      "accountName": "Test Bank Account",
      |      "sortCode": "010203",
      |      "accountNumber": "01023456"
      |    }
      |  },
      |  "compliance": {
      |    "numOfWorkersSupplied": 1000,
      |    "intermediaryArrangement": true,
      |    "supplyWorkers": true
      |  },
      |  "contact": {
      |    "commDetails": {
      |      "mobileNumber": "54321",
      |      "webAddress": "www.foo.com",
      |      "telephone": "12345",
      |      "email": "email@email.com",
      |      "commsPreference": "ZEL"
      |    },
      |    "address": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "postCode": "ZZ1 1ZZ",
      |      "countryCode": "GB"
      |    }
      |  },
      |  "admin": {
      |    "additionalInformation": {
      |      "customerStatus": "2"
      |    },
      |    "attachments": {
      |      "EORIrequested": true
      |    }
      |  },
      |  "periods": {
      |    "customerPreferredPeriodicity": "MA"
      |  }
      |}""".stripMargin)

  val vatSubmissionVoluntaryJsonWithBp: JsValue = Json.parse(
    """
      |{
      |  "messageType": "SubmissionCreate",
      |  "customerIdentification": {
      |    "tradingName": "trading-name",
      |    "tradersPartyType": "50",
      |    "primeBPSafeID": "testBpSafeId",
      |    "shortOrgName": "testCompanyName"
      |  },
      |  "declaration": {
      |    "declarationSigning": {
      |      "declarationCapacity": "03",
      |      "confirmInformationDeclaration": true
      |    },
      |    "applicantDetails": {
      |      "commDetails": {
      |        "email": "skylake@vilikariet.com"
      |      },
      |      "name": {
      |        "firstName": "Forename",
      |        "lastName": "Surname"
      |      },
      |      "dateOfBirth": "2018-01-01",
      |      "roleInBusiness": "03",
      |      "identifiers": [
      |        {
      |          "idValue": "AB123456A",
      |          "idType": "NINO",
      |          "IDsVerificationStatus": "1",
      |          "date": "2018-01-01"
      |        }
      |      ],
      |      "prevName": {
      |        "firstName": "Forename",
      |        "lastName": "Surname",
      |        "nameChangeDate": "2018-01-01"
      |      },
      |      "currAddress": {
      |        "line1": "line1",
      |        "line2": "line2",
      |        "postCode": "XX XX",
      |        "countryCode": "GB",
      |        "addressValidated": true
      |      }
      |    }
      |  },
      |  "subscription": {
      |    "corporateBodyRegistered": {
      |      "dateOfIncorporation": "2020-01-02",
      |      "companyRegistrationNumber": "testCrn",
      |      "countryOfIncorporation": "GB"
      |    },
      |    "reasonForSubscription": {
      |      "voluntaryOrEarlierDate": "2018-01-01",
      |      "relevantDate": "2018-01-01",
      |      "registrationReason": "0018",
      |      "exemptionOrException": "0"
      |    },
      |    "yourTurnover": {
      |      "VATRepaymentExpected": false,
      |      "turnoverNext12Months": 123456,
      |      "zeroRatedSupplies": 12.99
      |    },
      |    "schemes": {
      |      "startDate": "2018-01-01",
      |      "FRSCategory": "testCategory",
      |      "FRSPercentage": 15,
      |      "limitedCostTrader": false
      |    },
      |    "businessActivities": {
      |      "SICCodes": {
      |        "mainCode2": "00998",
      |        "primaryMainCode": "12345",
      |        "mainCode3": "00889"
      |      },
      |      "description": "this is my business description"
      |    }
      |  },
      |  "bankDetails": {
      |    "UK": {
      |      "accountName": "Test Bank Account",
      |      "sortCode": "010203",
      |      "accountNumber": "01023456"
      |    }
      |  },
      |  "compliance": {
      |    "numOfWorkersSupplied": 1000,
      |    "intermediaryArrangement": true,
      |    "supplyWorkers": true
      |  },
      |  "contact": {
      |    "commDetails": {
      |      "mobileNumber": "54321",
      |      "webAddress": "www.foo.com",
      |      "telephone": "12345",
      |      "email": "email@email.com",
      |      "commsPreference": "ZEL"
      |    },
      |    "address": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "postCode": "ZZ1 1ZZ",
      |      "countryCode": "GB"
      |    }
      |  },
      |  "admin": {
      |    "additionalInformation": {
      |      "customerStatus": "2"
      |    },
      |    "attachments": {
      |      "EORIrequested": true
      |    }
      |  },
      |  "periods": {
      |    "customerPreferredPeriodicity": "MA"
      |  }
      |}""".stripMargin)

  val vatSubmissionJson: JsValue = Json.parse(
    """
      |{
      |  "messageType": "SubmissionCreate",
      |  "customerIdentification": {
      |    "tradingName": "trading-name",
      |    "tradersPartyType": "50",
      |    "shortOrgName": "testCompanyName",
      |    "customerID": [
      |      {
      |        "idValue": "testCtUtr",
      |        "idType": "UTR",
      |        "IDsVerificationStatus": "3"
      |      },
      |      {
      |        "idValue": "testCrn",
      |        "idType": "CRN",
      |        "IDsVerificationStatus": "3",
      |        "date": "2020-01-02"
      |      }
      |    ],
      |    "name": {
      |      "firstName": "Forename",
      |      "lastName": "Surname"
      |    },
      |    "dateOfBirth": "2018-01-01"
      |  },
      |  "declaration": {
      |    "declarationSigning": {
      |      "declarationCapacity": "03",
      |      "confirmInformationDeclaration": true
      |    },
      |    "applicantDetails": {
      |      "commDetails": {
      |        "email": "skylake@vilikariet.com"
      |      },
      |      "name": {
      |        "firstName": "Forename",
      |        "lastName": "Surname"
      |      },
      |      "dateOfBirth": "2018-01-01",
      |      "roleInBusiness": "03",
      |      "identifiers": [
      |        {
      |          "idValue": "AB123456A",
      |          "idType": "NINO",
      |          "IDsVerificationStatus": "1",
      |          "date": "2018-01-01"
      |        }
      |      ],
      |      "prevName": {
      |        "firstName": "Forename",
      |        "lastName": "Surname",
      |        "nameChangeDate": "2018-01-01"
      |      },
      |      "currAddress": {
      |        "line1": "line1",
      |        "line2": "line2",
      |        "postCode": "XX XX",
      |        "countryCode": "GB",
      |        "addressValidated": true
      |      }
      |    }
      |  },
      |  "subscription": {
      |    "corporateBodyRegistered": {
      |      "dateOfIncorporation": "2020-01-02",
      |      "companyRegistrationNumber": "testCrn",
      |      "countryOfIncorporation": "GB"
      |    },
      |    "reasonForSubscription": {
      |      "voluntaryOrEarlierDate": "2018-01-01",
      |      "relevantDate": "2020-10-07",
      |      "registrationReason": "0016",
      |      "exemptionOrException": "0"
      |    },
      |    "yourTurnover": {
      |      "VATRepaymentExpected": false,
      |      "turnoverNext12Months": 123456,
      |      "zeroRatedSupplies": 12.99
      |    },
      |    "schemes": {
      |      "startDate": "2018-01-01",
      |      "FRSCategory": "testCategory",
      |      "FRSPercentage": 15,
      |      "limitedCostTrader": false
      |    },
      |    "businessActivities": {
      |      "SICCodes": {
      |        "mainCode2": "00998",
      |        "primaryMainCode": "12345",
      |        "mainCode3": "00889"
      |      },
      |      "description": "this is my business description"
      |    }
      |  },
      |  "bankDetails": {
      |    "UK": {
      |      "accountName": "Test Bank Account",
      |      "sortCode": "010203",
      |      "accountNumber": "01023456"
      |    }
      |  },
      |  "compliance": {
      |    "numOfWorkersSupplied": 1000,
      |    "intermediaryArrangement": true,
      |    "supplyWorkers": true
      |  },
      |  "contact": {
      |    "commDetails": {
      |      "mobileNumber": "54321",
      |      "webAddress": "www.foo.com",
      |      "telephone": "12345",
      |      "email": "email@email.com",
      |      "commsPreference": "ZEL"
      |    },
      |    "address": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "postCode": "ZZ1 1ZZ",
      |      "countryCode": "GB"
      |    }
      |  },
      |  "admin": {
      |    "additionalInformation": {
      |      "customerStatus": "2"
      |    },
      |    "attachments": {
      |      "EORIrequested": true
      |    }
      |  },
      |  "periods": {
      |    "customerPreferredPeriodicity": "MA"
      |  }
      |}""".stripMargin)

  val vatSubmissionVoluntaryJson: JsValue = Json.parse(
    """
      |{
      |  "messageType": "SubmissionCreate",
      |  "customerIdentification": {
      |    "tradingName": "trading-name",
      |    "tradersPartyType": "50",
      |    "shortOrgName": "testCompanyName",
      |    "customerID": [
      |      {
      |        "idValue": "testCtUtr",
      |        "idType": "UTR",
      |        "IDsVerificationStatus": "3"
      |      },
      |      {
      |        "idValue": "testCrn",
      |        "idType": "CRN",
      |        "IDsVerificationStatus": "3",
      |        "date": "2020-01-02"
      |      }
      |    ],
      |    "name": {
      |      "firstName": "Forename",
      |      "lastName": "Surname"
      |    },
      |    "dateOfBirth": "2018-01-01"
      |  },
      |  "declaration": {
      |    "declarationSigning": {
      |      "declarationCapacity": "03",
      |      "confirmInformationDeclaration": true
      |    },
      |    "applicantDetails": {
      |      "commDetails": {
      |        "email": "skylake@vilikariet.com"
      |      },
      |      "name": {
      |        "firstName": "Forename",
      |        "lastName": "Surname"
      |      },
      |      "dateOfBirth": "2018-01-01",
      |      "roleInBusiness": "03",
      |      "identifiers": [
      |        {
      |          "idValue": "AB123456A",
      |          "idType": "NINO",
      |          "IDsVerificationStatus": "1",
      |          "date": "2018-01-01"
      |        }
      |      ],
      |      "prevName": {
      |        "firstName": "Forename",
      |        "lastName": "Surname",
      |        "nameChangeDate": "2018-01-01"
      |      },
      |      "currAddress": {
      |        "line1": "line1",
      |        "line2": "line2",
      |        "postCode": "XX XX",
      |        "countryCode": "GB",
      |        "addressValidated": true
      |      }
      |    }
      |  },
      |  "subscription": {
      |    "corporateBodyRegistered": {
      |      "dateOfIncorporation": "2020-01-02",
      |      "companyRegistrationNumber": "testCrn",
      |      "countryOfIncorporation": "GB"
      |    },
      |    "reasonForSubscription": {
      |      "voluntaryOrEarlierDate": "2018-01-01",
      |      "relevantDate": "2018-01-01",
      |      "registrationReason": "0018",
      |      "exemptionOrException": "0"
      |    },
      |    "yourTurnover": {
      |      "VATRepaymentExpected": false,
      |      "turnoverNext12Months": 123456,
      |      "zeroRatedSupplies": 12.99
      |    },
      |    "schemes": {
      |      "startDate": "2018-01-01",
      |      "FRSCategory": "testCategory",
      |      "FRSPercentage": 15,
      |      "limitedCostTrader": false
      |    },
      |    "businessActivities": {
      |      "SICCodes": {
      |        "mainCode2": "00998",
      |        "primaryMainCode": "12345",
      |        "mainCode3": "00889"
      |      },
      |      "description": "this is my business description"
      |    }
      |  },
      |  "bankDetails": {
      |    "UK": {
      |      "accountName": "Test Bank Account",
      |      "sortCode": "010203",
      |      "accountNumber": "01023456"
      |    }
      |  },
      |  "compliance": {
      |    "numOfWorkersSupplied": 1000,
      |    "intermediaryArrangement": true,
      |    "supplyWorkers": true
      |  },
      |  "contact": {
      |    "commDetails": {
      |      "mobileNumber": "54321",
      |      "webAddress": "www.foo.com",
      |      "telephone": "12345",
      |      "email": "email@email.com",
      |      "commsPreference": "ZEL"
      |    },
      |    "address": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "postCode": "ZZ1 1ZZ",
      |      "countryCode": "GB"
      |    }
      |  },
      |  "admin": {
      |    "additionalInformation": {
      |      "customerStatus": "2"
      |    },
      |    "attachments": {
      |      "EORIrequested": true
      |    }
      |  },
      |  "periods": {
      |    "customerPreferredPeriodicity": "MA"
      |  }
      |}""".stripMargin)

  val noBpIdVatSubmissionJson = Json.parse(
    """
      |{
      |  "messageType": "SubmissionCreate",
      |  "customerIdentification": {
      |    "tradingName": "trading-name",
      |    "tradersPartyType": "50",
      |    "shortOrgName": "testCompanyName",
      |    "customerID": [
      |      {
      |        "idValue": "testCtUtr",
      |        "idType": "UTR",
      |        "IDsVerificationStatus": "3"
      |      },
      |      {
      |        "idValue": "testCrn",
      |        "idType": "CRN",
      |        "IDsVerificationStatus": "3",
      |        "date": "2020-01-02"
      |      }
      |    ]
      |  },
      |  "declaration": {
      |    "declarationSigning": {
      |      "declarationCapacity": "03",
      |      "confirmInformationDeclaration": true
      |    },
      |    "applicantDetails": {
      |      "commDetails": {
      |        "email": "skylake@vilikariet.com"
      |      },
      |      "name": {
      |        "firstName": "Forename",
      |        "lastName": "Surname"
      |      },
      |      "dateOfBirth": "2018-01-01",
      |      "roleInBusiness": "03",
      |      "identifiers": [
      |        {
      |          "idValue": "AB123456A",
      |          "idType": "NINO",
      |          "IDsVerificationStatus": "1",
      |          "date": "2018-01-01"
      |        }
      |      ],
      |      "prevName": {
      |        "firstName": "Forename",
      |        "lastName": "Surname",
      |        "nameChangeDate": "2018-01-01"
      |      },
      |      "currAddress": {
      |        "line1": "line1",
      |        "line2": "line2",
      |        "postCode": "XX XX",
      |        "countryCode": "GB",
      |        "addressValidated": true
      |      }
      |    }
      |  },
      |  "subscription": {
      |    "corporateBodyRegistered": {
      |      "dateOfIncorporation": "2020-01-02",
      |      "companyRegistrationNumber": "testCrn",
      |      "countryOfIncorporation": "GB"
      |    },
      |    "reasonForSubscription": {
      |      "voluntaryOrEarlierDate": "2018-01-01",
      |      "relevantDate": "2020-10-07",
      |      "registrationReason": "0016",
      |      "exemptionOrException": "0"
      |    },
      |    "yourTurnover": {
      |      "VATRepaymentExpected": false,
      |      "turnoverNext12Months": 123456,
      |      "zeroRatedSupplies": 12.99
      |    },
      |    "schemes": {
      |      "startDate": "2018-01-01",
      |      "FRSCategory": "testCategory",
      |      "FRSPercentage": 15,
      |      "limitedCostTrader": false
      |    },
      |    "businessActivities": {
      |      "SICCodes": {
      |        "mainCode2": "00998",
      |        "primaryMainCode": "12345",
      |        "mainCode3": "00889"
      |      },
      |      "description": "this is my business description"
      |    }
      |  },
      |  "bankDetails": {
      |    "UK": {
      |      "accountName": "Test Bank Account",
      |      "sortCode": "010203",
      |      "accountNumber": "01023456"
      |    }
      |  },
      |  "compliance": {
      |    "numOfWorkersSupplied": 1000,
      |    "intermediaryArrangement": true,
      |    "supplyWorkers": true
      |  },
      |  "contact": {
      |    "commDetails": {
      |      "mobileNumber": "54321",
      |      "webAddress": "www.foo.com",
      |      "telephone": "12345",
      |      "email": "email@email.com",
      |      "commsPreference": "ZEL"
      |    },
      |    "address": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "postCode": "ZZ1 1ZZ",
      |      "countryCode": "GB"
      |    }
      |  },
      |  "admin": {
      |    "additionalInformation": {
      |      "customerStatus": "2"
      |    },
      |    "attachments": {
      |      "EORIrequested": true
      |    }
      |  },
      |  "periods": {
      |    "customerPreferredPeriodicity": "MA"
      |  }
      |}""".stripMargin)

  val mongoJson: JsValue = Json.parse(
    """
      |{
      |  "messageType": "SubmissionCreate",
      |  "tradersPartyType": "50",
      |  "confirmInformationDeclaration": true,
      |  "companyRegistrationNumber": "testCrn",
      |  "applicantDetails": {
      |    "nino": "AB123456A",
      |    "roleInTheBusiness": "03",
      |    "name": {
      |      "first": "Forename",
      |      "last": "Surname"
      |    },
      |    "dateOfBirth": "2018-01-01",
      |    "companyName": "testCompanyName",
      |    "companyNumber": "testCrn",
      |    "dateOfIncorporation": "2020-01-02",
      |    "ctutr": "testCtUtr",
      |    "businessVerification": "FAIL",
      |    "registration": "REGISTRATION_NOT_CALLED",
      |    "identifiersMatch": true,
      |    "currentAddress": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "postcode": "XX XX",
      |      "country": {
      |         "code": "GB"
      |      },
      |      "addressValidated": true
      |    },
      |    "contact": {
      |      "email": "skylake@vilikariet.com"
      |    },
      |    "changeOfName": {
      |      "name": {
      |        "first": "Forename",
      |        "last": "Surname"
      |      },
      |      "change": "2018-01-01"
      |    },
      |    "countryOfIncorporation": "GB"
      |  },
      |  "bankDetails": {
      |    "isProvided": true,
      |    "details": {
      |      "name": "Test Bank Account",
      |      "sortCode": "010203",
      |      "number": "01023456"
      |    }
      |  },
      |  "sicAndCompliance": {
      |    "businessDescription": "this is my business description",
      |    "labourCompliance": {
      |      "numOfWorkersSupplied": 1000,
      |      "intermediaryArrangement": true,
      |      "supplyWorkers": true
      |    },
      |    "mainBusinessActivity": {
      |      "code": "12345",
      |      "desc": "the flu",
      |      "indexes": "sic details"
      |    },
      |    "businessActivities": [
      |      {
      |        "code": "00998",
      |        "desc": "otherBusiness desc 1",
      |        "indexes": "fooBar 1"
      |      },
      |      {
      |        "code": "00889",
      |        "desc": "otherBusiness desc 2",
      |        "indexes": "fooBar 2"
      |      }
      |    ]
      |  },
      |  "businessContact": {
      |    "digitalContact": {
      |      "email": "email@email.com",
      |      "tel": "12345",
      |      "mobile": "54321"
      |    },
      |    "website": "www.foo.com",
      |    "ppob": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "postcode": "ZZ1 1ZZ",
      |      "country": {
      |         "code": "GB"
      |      }
      |    },
      |    "contactPreference": "Email"
      |  },
      |  "tradingDetails": {
      |    "tradingName": "trading-name",
      |    "eoriRequested": true
      |  },
      |  "flatRateScheme": {
      |    "businessGoods": {
      |      "estimatedTotalSales": 1234567891011,
      |      "overTurnover": true
      |    },
      |    "startDate": "2018-01-01",
      |    "categoryOfBusiness": "testCategory",
      |    "percent": 15,
      |    "limitedCostTrader" : false
      |  },
      |  "returns": {
      |     "reclaimVatOnMostReturns" : false,
      |     "returnsFrequency" : "quarterly",
      |     "staggerStart" : "MA",
      |     "startDate" : "2018-01-01",
      |     "zeroRatedSupplies": 12.99
      |  },
      |  "eligibilitySubmissionData": {
      |    "threshold": {
      |      "mandatoryRegistration": true,
      |      "thresholdPreviousThirtyDays": "2020-10-07",
      |      "thresholdInTwelveMonths": "2020-10-07",
      |      "thresholdNextThirtyDays": "2020-10-07"
      |    },
      |    "exceptionOrExemption": "0",
      |    "estimates": {
      |      "turnoverEstimate": 123456
      |    },
      |    "customerStatus": "2"
      |  }
      |}""".stripMargin)

}
