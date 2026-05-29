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

import featureswitch.core.config.{FeatureSwitching, SubmitBarsInvalidBankDetailsToAPI}
import fixtures.{VatRegistrationFixture, VatSubmissionFixture}
import helpers.VatRegSpec
import models.BuildFailure
import models.api.VatScheme
import models.submission.{EntitiesArrayType, Individual, PartnerEntity, PartyType}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.Request
import play.api.test.FakeRequest
import services.submission.buildermocks._

class SubmissionPayloadBuilderSpec
    extends VatRegSpec
    with VatRegistrationFixture
    with VatSubmissionFixture
    with MockAdminBlockBuilder
    with MockCustomerIdentificationBlockBuilder
    with MockContactBlockBuilder
    with MockPeriodsBlockBuilder
    with MockComplianceBlockBuilder
    with MockSubscriptionBlockBuilder
    with MockDeclarationBlockBuilder
    with MockAnnualAccountingBlockBuilder
    with MockEntitiesBlockBuilder
    with FeatureSwitching {

  private val mockBankDetailsBlockBuilder: BankDetailsBlockBuilder = mock[BankDetailsBlockBuilder]

  implicit val request: Request[_] = FakeRequest()

  object TestBuilder
      extends SubmissionPayloadBuilder(
        mockAdminBlockBuilder,
        mockDeclarationBlockBuilder,
        mockCustomerIdentificationBlockBuilder,
        mockContactBlockBuilder,
        mockBankDetailsBlockBuilder,
        mockPeriodsBlockBuilder,
        mockSubscriptionBlockBuilder,
        mockComplianceBlockBuilder,
        mockAnnualAccountingBlockBuilder,
        mockEntitiesBlockBuilder
      )

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(SubmitBarsInvalidBankDetailsToAPI)
  }

  val testAdminBlockJson: JsObject = Json.obj(
    "additionalInformation" -> Json.obj(
      "customerStatus" -> "2"
    ),
    "attachments" -> Json.obj(
      "EORIrequested" -> true
    )
  )

  val testDeclarationBlockJson: JsObject = Json.obj(
    "declarationSigning" -> Json.obj(
      "confirmInformationDeclaration" -> true,
      "declarationCapacity"           -> "03"
    ),
    "applicantDetails" -> Json.obj(
      "roleInBusiness" -> "Director",
      "name" -> Json.obj(
        "firstName" -> "Test",
        "lastName"  -> "Name"
      ),
      "dateOfBirth" -> "2000-10-20",
      "currAddress" -> Json.obj(
        "line1"       -> "line1",
        "line2"       -> "line2",
        "postCode"    -> "ZZ1 1ZZ",
        "countryCode" -> "GB"
      ),
      "commDetails" -> Json.obj(
        "email" -> "email@email.com"
      ),
      "identifiers" -> Json.arr(
        Json.obj(
          "idValue"               -> "AB123456A",
          "idType"                -> "NINO",
          "IDsVerificationStatus" -> "1",
          "date"                  -> "2018-01-01"
        )
      )
    )
  )

  val testCustomerIdentificationBlockJson: JsObject = Json.obj(
    "tradingName"      -> "trading-name",
    "tradersPartyType" -> "50",
    "primeBPSafeID"    -> "testBpSafeId",
    "shortOrgName"     -> "testCompanyName"
  )

  val testContactBlockJson: JsObject = Json.obj(
    "address" -> Json.obj(
      "line1"       -> "line1",
      "line2"       -> "line2",
      "postCode"    -> "ZZ1 1ZZ",
      "countryCode" -> "GB"
    ),
    "commDetails" -> Json.obj(
      "telephone"       -> "12345",
      "email"           -> "email@email.com",
      "commsPreference" -> "ZEL"
    )
  )

  val testPeriodsBlockJson: JsObject = Json.obj("customerPreferredPeriodicity" -> "MM")

  val testBankDetailsBlockJson: JsObject = Json.obj(
    "UK" -> Json.obj(
      "accountName"   -> "Test Bank Account",
      "sortCode"      -> "010203",
      "accountNumber" -> "01023456"
    )
  )

  val testComplianceBlockJson: JsObject = Json.obj(
    "numOfWorkersSupplied"    -> 1,
    "intermediaryArrangement" -> true,
    "supplyWorkers"           -> true
  )

  val testSubscriptionBlockJson: JsObject = Json.obj(
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "registrationReason"     -> "0016",
        "relevantDate"           -> "2020-10-07",
        "voluntaryOrEarlierDate" -> "2020-02-02",
        "exemptionOrException"   -> "0"
      ),
      "corporateBodyRegistered" -> Json.obj(
        "companyRegistrationNumber" -> "testCrn",
        "dateOfIncorporation"       -> "2020-01-02",
        "countryOfIncorporation"    -> "GB"
      ),
      "businessActivities" -> Json.obj(
        "description" -> "testDescription",
        "SICCodes" -> Json.obj(
          "primaryMainCode" -> "12345",
          "mainCode2"       -> "00002",
          "mainCode3"       -> "00003",
          "mainCode4"       -> "00004"
        )
      ),
      "yourTurnover" -> Json.obj(
        "turnoverNext12Months" -> 123456,
        "zeroRatedSupplies"    -> 12.99,
        "VATRepaymentExpected" -> true
      ),
      "schemes" -> Json.obj(
        "FRSCategory"       -> "testCategory",
        "FRSPercentage"     -> 15,
        "startDate"         -> "2020-02-02",
        "limitedCostTrader" -> false
      )
    )
  )

  val testAnnualAccountingBlockJson: JsObject = Json.obj(
    "submissionType" -> "01",
    "customerRequest" -> Json.obj(
      "paymentMethod"      -> "",
      "annualStagger"      -> "",
      "paymentFrequency"   -> "",
      "estimatedTurnover"  -> "",
      "requestedStartDate" -> ""
    )
  )

  val testEntitiesBlockJson: JsArray = Json.arr(
    Json.obj(
      "action"     -> 1,
      "entityType" -> Json.toJson[EntitiesArrayType](PartnerEntity),
      "partyType"  -> Json.toJson[PartyType](Individual),
      "businessContactDetails" -> Json.obj(
        "address"   -> Json.toJson(testAddress),
        "telephone" -> "testPhone"
      )
    )
  )

  val expectedJson: JsObject = Json.obj(
    "messageType"            -> "SubscriptionCreate",
    "admin"                  -> testAdminBlockJson,
    "declaration"            -> testDeclarationBlockJson,
    "customerIdentification" -> testCustomerIdentificationBlockJson,
    "contact"                -> testContactBlockJson,
    "subscription"           -> testSubscriptionBlockJson,
    "bankDetails"            -> testBankDetailsBlockJson,
    "periods"                -> testPeriodsBlockJson,
    "joinAA"                 -> testAnnualAccountingBlockJson,
    "compliance"             -> testComplianceBlockJson,
    "entities"               -> testEntitiesBlockJson
  )
  enable(SubmitBarsInvalidBankDetailsToAPI)
  "buildSubmissionPayload" should {
    "return a submission json object in a Right" when {
      "all required pieces of data are available in the database" in {
        mockBuildBankDetailsBlock(testFullVatScheme)(Right(testBankDetailsBlockJson))
        mockBuildAdminBlock(testFullVatScheme)(testAdminBlockJson)
        mockBuildDeclarationBlock(testFullVatScheme)(testDeclarationBlockJson)
        mockBuildCustomerIdentificationBlock(testFullVatScheme)(testCustomerIdentificationBlockJson)
        mockBuildContactBlock(testFullVatScheme)(testContactBlockJson)
        mockBuildSubscriptionBlock(testFullVatScheme)(testSubscriptionBlockJson)
        mockBuildPeriodsBlock(testFullVatScheme)(testPeriodsBlockJson)
        mockBuildAnnualAccountingBlock(testFullVatScheme)(Some(testAnnualAccountingBlockJson))
        mockBuildComplianceBlock(testFullVatScheme)(Some(testComplianceBlockJson))
        mockBuildEntitiesBlock(testFullVatScheme)(Some(testEntitiesBlockJson))

        val result = TestBuilder.buildSubmissionPayload(testFullVatScheme)

        result mustBe Right(expectedJson)
      }

      "there are no annual accounting answers in the database" in {
        val vatSchemeWithoutAnnualAccounting = testFullVatScheme.copy(business = None)
        mockBuildBankDetailsBlock(vatSchemeWithoutAnnualAccounting)(Right(testBankDetailsBlockJson))
        mockBuildAdminBlock(vatSchemeWithoutAnnualAccounting)(testAdminBlockJson)
        mockBuildDeclarationBlock(vatSchemeWithoutAnnualAccounting)(testDeclarationBlockJson)
        mockBuildCustomerIdentificationBlock(vatSchemeWithoutAnnualAccounting)(testCustomerIdentificationBlockJson)
        mockBuildContactBlock(vatSchemeWithoutAnnualAccounting)(testContactBlockJson)
        mockBuildSubscriptionBlock(vatSchemeWithoutAnnualAccounting)(testSubscriptionBlockJson)
        mockBuildPeriodsBlock(vatSchemeWithoutAnnualAccounting)(testPeriodsBlockJson)
        mockBuildComplianceBlock(vatSchemeWithoutAnnualAccounting)(Some(testComplianceBlockJson))
        mockBuildEntitiesBlock(vatSchemeWithoutAnnualAccounting)(Some(testEntitiesBlockJson))

        mockBuildAnnualAccountingBlock(vatSchemeWithoutAnnualAccounting)(None)

        val result = TestBuilder.buildSubmissionPayload(vatSchemeWithoutAnnualAccounting)

        result mustBe Right(expectedJson - "joinAA")
      }

      "there are no compliance answers in the database" in {
        val vatSchemeWithoutBusinessCompliance = testFullVatScheme.copy(business = None)
        mockBuildBankDetailsBlock(vatSchemeWithoutBusinessCompliance)(Right(testBankDetailsBlockJson))
        mockBuildAdminBlock(vatSchemeWithoutBusinessCompliance)(testAdminBlockJson)
        mockBuildDeclarationBlock(vatSchemeWithoutBusinessCompliance)(testDeclarationBlockJson)
        mockBuildCustomerIdentificationBlock(vatSchemeWithoutBusinessCompliance)(testCustomerIdentificationBlockJson)
        mockBuildContactBlock(vatSchemeWithoutBusinessCompliance)(testContactBlockJson)
        mockBuildSubscriptionBlock(vatSchemeWithoutBusinessCompliance)(testSubscriptionBlockJson)
        mockBuildPeriodsBlock(vatSchemeWithoutBusinessCompliance)(testPeriodsBlockJson)
        mockBuildAnnualAccountingBlock(vatSchemeWithoutBusinessCompliance)(Some(testAnnualAccountingBlockJson))
        mockBuildEntitiesBlock(vatSchemeWithoutBusinessCompliance)(Some(testEntitiesBlockJson))

        mockBuildComplianceBlock(vatSchemeWithoutBusinessCompliance)(None)

        val result = TestBuilder.buildSubmissionPayload(vatSchemeWithoutBusinessCompliance)

        result mustBe Right(expectedJson - "compliance")
      }

      "the entities section is not present" in {
        val vatSchemeWithoutEntities = testFullVatScheme.copy(entities = None)
        mockBuildBankDetailsBlock(vatSchemeWithoutEntities)(Right(testBankDetailsBlockJson))
        mockBuildAdminBlock(vatSchemeWithoutEntities)(testAdminBlockJson)
        mockBuildDeclarationBlock(vatSchemeWithoutEntities)(testDeclarationBlockJson)
        mockBuildCustomerIdentificationBlock(vatSchemeWithoutEntities)(testCustomerIdentificationBlockJson)
        mockBuildContactBlock(vatSchemeWithoutEntities)(testContactBlockJson)
        mockBuildSubscriptionBlock(vatSchemeWithoutEntities)(testSubscriptionBlockJson)
        mockBuildPeriodsBlock(vatSchemeWithoutEntities)(testPeriodsBlockJson)
        mockBuildComplianceBlock(vatSchemeWithoutEntities)(Some(testComplianceBlockJson))
        mockBuildAnnualAccountingBlock(vatSchemeWithoutEntities)(Some(testAnnualAccountingBlockJson))

        mockBuildEntitiesBlock(vatSchemeWithoutEntities)(None)

        val result = TestBuilder.buildSubmissionPayload(vatSchemeWithoutEntities)

        result mustBe Right(expectedJson - "entities")
      }
    }

    "return a BuildFailure in a Left" when {
      "BankDetailsBlockBuilder returns a failure" in {
        val expectedBuildFailureResponse = Left(BuildFailure("[BankDetailsBlockBuilder] Unable to build submission model: No BankAccount model"))
        mockBuildBankDetailsBlock(testVatScheme)(expectedBuildFailureResponse)
        val result = TestBuilder.buildSubmissionPayload(testVatScheme)

        result mustBe expectedBuildFailureResponse
      }
    }
  }

  def mockBuildBankDetailsBlock(vatScheme: VatScheme)(response: Either[BuildFailure, JsObject]): OngoingStubbing[Either[BuildFailure, JsObject]] =
    when(mockBankDetailsBlockBuilder.buildBankDetailsBlock(ArgumentMatchers.eq(vatScheme))).thenReturn(response)

}
