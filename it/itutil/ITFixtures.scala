/*
 * Copyright 2017 HM Revenue & Customs
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
package itutil

import common.TransactionId
import enums.VatRegStatus
import models.{LimitedCompany, SoleTrader}
import models.api.returns._
import models.api.{returns, _}
import models.submission.{DateOfBirth, Director, OwnerProprietor, RoleInBusiness, UkCompany}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import java.nio.charset.StandardCharsets
import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.Base64

trait ITFixtures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val testDate: LocalDate = LocalDate.of(2017, 1, 1)
  val testDateTime: LocalDateTime = LocalDateTime.of(testDate, LocalTime.of(0, 0))
  val startDate = testDate
  val testRegId = "regId"
  val testInternalid = "INT-123-456-789"
  val testTransactionId = "transId"
  val vatScheme = VatScheme(testRegId, internalId = testInternalid, status = VatRegStatus.draft)
  val oldName = Name(first = Some("Bob"), middle = None, last = "Smith")
  val testTradingName = "trading-name"
  val testTradingDetails = TradingDetails(Some(testTradingName), true)
  val testAuthProviderId = "authProviderId"

  val testReturns: Returns = Returns(
    zeroRatedSupplies = Some(12.99),
    reclaimVatOnMostReturns = true,
    returnsFrequency = Quarterly,
    staggerStart = JanuaryStagger,
    startDate = Some(startDate),
    annualAccountingDetails = None
  )

  val frsDetails = FRSDetails(
    businessGoods = Some(BusinessGoods(12345678L, true)),
    startDate = Some(testDate),
    categoryOfBusiness = Some("123"),
    percent = 15,
    limitedCostTrader = Some(false)
  )

  val aasDetails = returns.AASDetails(
    paymentMethod = StandingOrder,
    paymentFrequency = MonthlyPayment
  )

  val testAASReturns: Returns = Returns(
    zeroRatedSupplies = Some(12.99),
    reclaimVatOnMostReturns = true,
    returnsFrequency = Annual,
    staggerStart = JanDecStagger,
    startDate = Some(startDate),
    annualAccountingDetails = Some(aasDetails)
  )

  val testFlatRateScheme = FlatRateScheme(joinFrs = true, Some(frsDetails))
  val EstimateValue: Long = 1000L
  val zeroRatedTurnoverEstimate: Long = 1000L
  val testCountry = Country(Some("GB"), None)
  val testAddress = Address("line1", "line2", None, None, Some("XX XX"), Some(testCountry), addressValidated = Some(true))
  val testFullAddress = Address("line1", "line2", Some("line3"), Some("line4"), Some("XX XX"), Some(testCountry), addressValidated = Some(true))
  val testContactDetails = DigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))
  val testDigitalContactOptional = DigitalContactOptional(Some("skylake@vilikariet.com"), Some("1234567890"), Some("1234567890"), Some(true))
  val testNino = "NB686868C"
  val testRole: RoleInBusiness = Director
  val testName = Name(first = Some("Forename"), middle = None, last = "Surname")
  val testFormerName = FormerName(name = Some(oldName), change = Some(testDate))
  val testCompanyName = "testCompanyName"
  val testDateOfBirth = DateOfBirth(testDate)
  val testCrn = "testCrn"
  val testCtUtr = "testCtUtr"
  val testSaUtr = "testSaUtr"
  val testDateOfIncorp = LocalDate.of(2020, 1, 2)
  val testBpSafeId = "testBpSafeId"
  val testWebsite = "www.foo.com"

  val testUnregisteredApplicantDetails: ApplicantDetails = ApplicantDetails(
    transactor = TransactorDetails(
      name = testName,
      nino = testNino,
      dateOfBirth = testDate
    ),
    entity = LimitedCompany(
      companyName = testCompanyName,
      companyNumber = testCrn,
      dateOfIncorporation = testDateOfIncorp,
      ctutr = testCtUtr,
      businessVerification = BvUnchallenged,
      registration = NotCalledStatus,
      identifiersMatch = true,
      bpSafeId = None
    ),
    roleInBusiness = testRole,
    currentAddress = testFullAddress,
    contact = testDigitalContactOptional,
    changeOfName = Some(testFormerName),
    previousAddress = Some(testFullAddress)
  )

  val testRegisteredApplicantDetails: ApplicantDetails = ApplicantDetails(
    transactor = TransactorDetails(
      name = testName,
      nino = testNino,
      dateOfBirth = testDate
    ),
    entity = LimitedCompany(
      companyName = testCompanyName,
      companyNumber = testCrn,
      dateOfIncorporation = testDateOfIncorp,
      ctutr = testCtUtr,
      identifiersMatch = true,
      businessVerification = BvPass,
      registration = RegisteredStatus,
      bpSafeId = Some(testBpSafeId)
    ),
    roleInBusiness = testRole,
    currentAddress = testAddress,
    contact = testDigitalContactOptional,
    changeOfName = None,
    previousAddress = None
  )

  val testRegisteredSoleTraderApplicantDetails: ApplicantDetails =
    testRegisteredApplicantDetails.copy(
      entity = SoleTrader(
        sautr = Some(testSaUtr),
        bpSafeId = Some(testBpSafeId),
        businessVerification = BvPass,
        registration = RegisteredStatus,
        identifiersMatch = true
      ),
      roleInBusiness = OwnerProprietor
    )

  val testBusinessContactDetails = BusinessContact(digitalContact = testContactDetails, website = None, ppob = testFullAddress, commsPreference = Email)
  val testFullBusinessContactDetails = BusinessContact(digitalContact = testContactDetails, website = Some(testWebsite), ppob = testFullAddress, commsPreference = Email)

  val testSicAndCompliance = SicAndCompliance(
    businessDescription = "businessDesc",
    labourCompliance = Some(ComplianceLabour(
      numOfWorkersSupplied = Some(1),
      intermediaryArrangement = Some(true),
      supplyWorkers = true)
    ),
    mainBusinessActivity = SicCode("12345", "sicDesc", "sicDetail"),
    businessActivities = List(SicCode("12345", "sicDesc", "sicDetail")))

  val testFullSicAndCompliance = SicAndCompliance(
    businessDescription = "businessDesc",
    labourCompliance = Some(ComplianceLabour(
      numOfWorkersSupplied = Some(1),
      intermediaryArrangement = Some(true),
      supplyWorkers = true)
    ),
    mainBusinessActivity = SicCode("12345", "sicDesc", "sicDetail"),
    businessActivities = List(
      SicCode("00002", "sicDesc", "sicDetail"),
      SicCode("00003", "sicDesc", "sicDetail"),
      SicCode("00004", "sicDesc", "sicDetail")
    )
  )

  val testTurnoverEstimates = TurnoverEstimates(12345678L)

  val testBankDetails = BankAccountDetails(
    name = "testBankName",
    sortCode = "11-11-11",
    number = "01234567"
  )
  val testSubmittedSortCode = "111111"

  val testThreshold = Threshold(mandatoryRegistration = true, Some(testDate), Some(testDate), Some(testDate))

  val testEligibilitySubmissionData: EligibilitySubmissionData = EligibilitySubmissionData(
    threshold = testThreshold,
    exceptionOrExemption = "0",
    estimates = TurnoverEstimates(123456),
    customerStatus = MTDfB,
    partyType = UkCompany
  )

  val testNrsSubmissionPayload = "testNrsSubmissionPayload"
  val testEncodedPayload: String = Base64.getEncoder.encodeToString(testNrsSubmissionPayload.getBytes(StandardCharsets.UTF_8))

  lazy val testVatScheme: VatScheme = VatScheme(testRegId, internalId = testInternalid, status = VatRegStatus.draft)

  lazy val testFullVatScheme: VatScheme = testVatScheme.copy(
    tradingDetails = Some(testTradingDetails),
    sicAndCompliance = Some(testFullSicAndCompliance),
    businessContact = Some(testFullBusinessContactDetails),
    bankAccount = Some(BankAccount(isProvided = true, Some(testBankDetails), None)),
    flatRateScheme = Some(testFlatRateScheme),
    applicantDetails = Some(testUnregisteredApplicantDetails),
    eligibilitySubmissionData = Some(testEligibilitySubmissionData),
    confirmInformationDeclaration = Some(true),
    returns = Some(testReturns),
    nrsSubmissionPayload = Some(testEncodedPayload)
  )

  lazy val testFullVatSchemeWithUnregisteredBusinessPartner: VatScheme =
    VatScheme(
      id = testRegId,
      internalId = testInternalid,
      transactionId = Some(TransactionId(testTransactionId)),
      tradingDetails = Some(testTradingDetails),
      returns = Some(testAASReturns),
      sicAndCompliance = Some(testSicAndCompliance),
      businessContact = Some(testBusinessContactDetails),
      bankAccount = Some(BankAccount(isProvided = true, Some(testBankDetails), None)),
      acknowledgementReference = Some("ackRef"),
      flatRateScheme = Some(testFlatRateScheme),
      status = VatRegStatus.draft,
      applicantDetails = Some(testUnregisteredApplicantDetails),
      eligibilitySubmissionData = Some(testEligibilitySubmissionData),
      confirmInformationDeclaration = Some(true),
      nrsSubmissionPayload = Some(testEncodedPayload)
    )

  lazy val testMinimalVatSchemeWithRegisteredBusinessPartner: VatScheme =
    VatScheme(
      id = testRegId,
      internalId = testInternalid,
      transactionId = Some(TransactionId(testTransactionId)),
      tradingDetails = Some(testTradingDetails),
      returns = Some(testReturns),
      sicAndCompliance = Some(testSicAndCompliance),
      businessContact = Some(testBusinessContactDetails),
      bankAccount = Some(BankAccount(isProvided = false, None, Some(BeingSetup))),
      acknowledgementReference = Some("ackRef"),
      flatRateScheme = Some(FlatRateScheme(joinFrs = false, None)),
      status = VatRegStatus.draft,
      applicantDetails = Some(testRegisteredApplicantDetails),
      eligibilitySubmissionData = Some(testEligibilitySubmissionData),
      confirmInformationDeclaration = Some(true)
    )

  lazy val testMinimalVatSchemeWithVerifiedSoleTrader: VatScheme =
    testMinimalVatSchemeWithRegisteredBusinessPartner.copy(
      applicantDetails = Some(testRegisteredSoleTraderApplicantDetails)
    )

  def testEmptyVatScheme(regId: String): VatScheme = VatScheme(
    id = regId,
    internalId = testInternalid,
    status = VatRegStatus.draft
  )

  object AuthTestData {

    import models.nonrepudiation.IdentityData
    import services.NonRepudiationService.NonRepudiationIdentityRetrievals
    import uk.gov.hmrc.auth.core.retrieve._
    import uk.gov.hmrc.auth.core.{ConfidenceLevel, CredentialStrength, User}

    val testExternalId = "testExternalId"
    val testAgentCode = "testAgentCode"
    val testConfidenceLevel = ConfidenceLevel.L200
    val testSautr = "testSautr"
    val testAuthName = uk.gov.hmrc.auth.core.retrieve.Name(Some("testFirstName"), Some("testLastName"))
    val testAuthDateOfBirth = org.joda.time.LocalDate.now()
    val testEmail = "testEmail"
    val testAgentInformation = AgentInformation(Some("testAgentId"), Some("testAgentCode"), Some("testAgentFriendlyName"))
    val testGroupIdentifier = "testGroupIdentifier"
    val testCredentialRole = User
    val testMdtpInformation = MdtpInformation("testDeviceId", "testSessionId")
    val testItmpName = ItmpName(Some("testGivenName"), Some("testMiddleName"), Some("testFamilyName"))
    val testItmpDateOfBirth = org.joda.time.LocalDate.now()
    val testItmpAddress = ItmpAddress(
      Some("testLine1"),
      None,
      None,
      None,
      None,
      Some("testPostcode"),
      None,
      None
    )
    val testCredentialStrength = CredentialStrength.strong
    val testLoginTimes = LoginTimes(org.joda.time.DateTime.now(), Some(org.joda.time.DateTime.now()))
    lazy val testAffinityGroup: AffinityGroup = AffinityGroup.Organisation
    lazy val testProviderId: String = "testProviderID"
    lazy val testProviderType: String = "GovernmentGateway"
    lazy val testCredentials: Credentials = Credentials(testProviderId, testProviderType)

    val testNonRepudiationIdentityData: IdentityData = IdentityData(
      Some(testInternalid),
      Some(testExternalId),
      Some(testAgentCode),
      Some(testCredentials),
      testConfidenceLevel,
      Some(testNino),
      Some(testSautr),
      Some(testAuthName),
      Some(testAuthDateOfBirth),
      Some(testEmail),
      testAgentInformation,
      Some(testGroupIdentifier),
      Some(testCredentialRole),
      Some(testMdtpInformation),
      Some(testItmpName),
      Some(testItmpDateOfBirth),
      Some(testItmpAddress),
      Some(testAffinityGroup),
      Some(testCredentialStrength),
      testLoginTimes
    )

    val identityJson: JsValue = Json.toJson(testNonRepudiationIdentityData)

    implicit class RetrievalCombiner[A](a: A) {
      def ~[B](b: B): A ~ B = new ~(a, b)
    }

    val testAuthRetrievals: NonRepudiationIdentityRetrievals =
      Some(testAffinityGroup) ~
        Some(testInternalid) ~
        Some(testExternalId) ~
        Some(testAgentCode) ~
        Some(testCredentials) ~
        testConfidenceLevel ~
        Some(testNino) ~
        Some(testSautr) ~
        Some(testAuthName) ~
        Some(testAuthDateOfBirth) ~
        Some(testEmail) ~
        testAgentInformation ~
        Some(testGroupIdentifier) ~
        Some(testCredentialRole) ~
        Some(testMdtpInformation) ~
        Some(testItmpName) ~
        Some(testItmpDateOfBirth) ~
        Some(testItmpAddress) ~
        Some(testCredentialStrength) ~
        testLoginTimes

  }

}
