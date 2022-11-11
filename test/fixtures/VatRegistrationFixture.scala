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

package fixtures

import enums.VatRegStatus
import models._
import models.api._
import models.api.vatapplication._
import models.submission._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.retrieve.Credentials

import java.nio.charset.StandardCharsets
import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.util.Base64

trait VatRegistrationFixture {
  lazy val testNino = "AB123456A"
  lazy val testRole = Director
  lazy val testArn = "testArn"
  lazy val testRegId = "testRegId"
  lazy val testInternalId = "INT-123-456-789"
  lazy val testAckReference = "BRPY000000000001"
  lazy val testDate: LocalDate = LocalDate.of(2018, 1, 1)
  lazy val testDateTime: LocalDateTime = LocalDateTime.of(testDate, LocalTime.MIDNIGHT)
  lazy val testDateOfBirth = testDate
  lazy val testCompanyName = "testCompanyName"
  lazy val testCrn = "testCrn"
  lazy val testVrn = "testVrn"
  lazy val testUtr = "testUtr"
  lazy val testChrn = "testChrn"
  lazy val testCasc = "testCasc"
  lazy val testTrn = "testTrn"
  lazy val testDateOFIncorp: LocalDate = LocalDate.of(2020, 1, 2)
  lazy val testAddress = Address("line1", Some("line2"), None, None, None, Some("ZZ1 1ZZ"), Some(Country(Some("GB"), Some("UK"))), addressValidated = Some(true))
  lazy val testPostcode = "ZZ1 1ZZ"
  lazy val testSicCode = SicCode("88888", "description", "description")
  lazy val testName = Name(first = Some("Forename"), middle = None, last = "Surname")
  lazy val testOldName = Name(first = Some("Bob"), middle = None, last = "Smith")
  lazy val testPreviousName = FormerName(hasFormerName = Some(true), name = Some(testOldName), change = Some(testDate))
  lazy val testVatScheme: VatScheme = VatScheme(testRegId, internalId = testInternalId, status = VatRegStatus.draft, createdDate = testDate)
  lazy val exception = new Exception("Exception")
  lazy val testVoluntaryThreshold = Threshold(mandatoryRegistration = false, None, None, None)
  lazy val testMandatoryThreshold = Threshold(
    mandatoryRegistration = true,
    Some(LocalDate.of(2020, 10, 7)),
    Some(LocalDate.of(2020, 10, 7)),
    Some(LocalDate.of(2020, 10, 7))
  )

  lazy val testDigitalContactOptional = DigitalContactOptional(Some("skylake@vilikariet.com"), None, None)
  lazy val testBankName = "Test Bank Account"
  lazy val testSortCode = "010203"
  lazy val testBankNumber = "01023456"
  lazy val testBankDetails = BankAccountDetails(testBankName, testSortCode, testBankNumber, ValidStatus)
  lazy val testOverseasBankName = "Test Overseas Bank Account"
  lazy val testBic = "010203"
  lazy val testIban = "01023456"
  lazy val testBankDetailsOverseas = BankAccountOverseasDetails(testOverseasBankName, testBic, testIban)
  lazy val testFormerName = FormerName(hasFormerName = Some(true), Some(testName), Some(testDate))
  lazy val testVatApplicationDetails = VatApplication(
    Some(true), Some(true), Some(testTurnover), None, Some(12.99), Some(false), Some(Quarterly),
    Some(JanuaryStagger), Some(testDate), None, None, None, None, None
  )
  lazy val zeroRatedSupplies: BigDecimal = 12.99
  lazy val testBpSafeId = "testBpSafeId"
  lazy val testFirstName = "testFirstName"
  lazy val testLastName = "testLastName"
  lazy val testWebsite = "www.foo.com"
  lazy val testProviderId: String = "testProviderID"
  lazy val testProviderType: String = "GovernmentGateway"
  lazy val testCredentials: Credentials = Credentials(testProviderId, testProviderType)
  lazy val testAffinityGroup: AffinityGroup = AffinityGroup.Organisation
  lazy val testAuthToken = "testAuthToken"

  val testTradingName = "trading-name"
  val testShortOrgName = "short-org-name"
  val testUserHeaders = Map("testKey" -> "testValue")

  lazy val testEligibilitySubmissionData: EligibilitySubmissionData = EligibilitySubmissionData(
    threshold = testMandatoryThreshold,
    None,
    partyType = UkCompany,
    registrationReason = ForwardLook,
    isTransactor = false,
    calculatedDate = testMandatoryThreshold.thresholdInTwelveMonths
  )

  val testLtdCoEntity = IncorporatedEntity(
    companyName = Some(testCompanyName),
    companyNumber = testCrn,
    ctutr = Some(testUtr),
    dateOfIncorporation = Some(testDateOFIncorp),
    businessVerification = Some(BvFail),
    registration = NotCalledStatus,
    identifiersMatch = true,
    chrn = None
  )

  val testSoleTraderEntity = SoleTraderIdEntity(
    testFirstName,
    testLastName,
    testDate,
    Some(testNino),
    sautr = Some(testUtr),
    businessVerification = Some(BvPass),
    registration = FailedStatus,
    identifiersMatch = true
  )

  val testGeneralPartnershipEntity: PartnershipIdEntity = PartnershipIdEntity(
    Some(testUtr),
    companyNumber = None,
    Some(testCompanyName),
    dateOfIncorporation = None,
    Some(testPostcode),
    chrn = None,
    Some(testBpSafeId),
    businessVerification = Some(BvPass),
    registration = RegisteredStatus,
    identifiersMatch = true
  )

  val testTrustEntity: MinorEntity = MinorEntity(
    Some(testCompanyName),
    Some(testUtr),
    None,
    None,
    Some(testPostcode),
    Some(testChrn),
    Some(testCasc),
    businessVerification = Some(BvPass),
    registration = RegisteredStatus,
    bpSafeId = Some(testBpSafeId),
    identifiersMatch = true
  )

  lazy val validApplicantDetails: ApplicantDetails = ApplicantDetails(
    personalDetails = PersonalDetails(
      name = testName,
      nino = Some(testNino),
      trn = None,
      arn = None,
      identifiersMatch = true,
      dateOfBirth = Some(testDate),
      None
    ),
    entity = testLtdCoEntity,
    roleInBusiness = testRole,
    currentAddress = testAddress,
    contact = testDigitalContactOptional,
    changeOfName = Some(testFormerName),
    previousAddress = None
  )

  lazy val unverifiedUserApplicantDetails: ApplicantDetails = ApplicantDetails(
    personalDetails = PersonalDetails(
      name = testName,
      nino = Some(testNino),
      trn = None,
      arn = None,
      identifiersMatch = false,
      dateOfBirth = Some(testDate),
      None
    ),
    entity = testLtdCoEntity,
    roleInBusiness = testRole,
    currentAddress = testAddress,
    contact = testDigitalContactOptional,
    changeOfName = Some(testFormerName),
    previousAddress = None
  )

  lazy val testEmail = "test@test.com"
  lazy val testTelephone = "1234567890"
  lazy val testOrgName = "testOrganisationName"
  lazy val validTransactorDetails: TransactorDetails = TransactorDetails(
    personalDetails = PersonalDetails(
      name = testName,
      nino = Some(testNino),
      trn = None,
      arn = None,
      identifiersMatch = true,
      dateOfBirth = Some(testDate),
      None
    ),
    isPartOfOrganisation = Some(true),
    organisationName = Some(testOrgName),
    telephone = testTelephone,
    email = testEmail,
    emailVerified = true,
    address = Some(testAddress),
    declarationCapacity = DeclarationCapacityAnswer(AuthorisedEmployee)
  )

  lazy val otherBusinessActivitiesSicAndCompiliance: List[SicCode] =
    SicCode("00998", "otherBusiness desc 1", "otherBusiness desc 1") :: SicCode("00889", "otherBusiness desc 2", "otherBusiness desc 2") :: Nil

  lazy val testBankAccount = BankAccount(isProvided = true, details = Some(testBankDetails), None, None)
  lazy val testBankAccountOverseas = BankAccount(isProvided = true, None, overseasDetails = Some(testBankDetailsOverseas), None)
  lazy val testBankAccountNotProvided = BankAccount(isProvided = false, details = None, overseasDetails = None, reason = Some(BeingSetup))

  lazy val validAASDetails: AASDetails = AASDetails(
    paymentMethod = StandingOrder,
    paymentFrequency = MonthlyPayment
  )

  lazy val testTurnover = 10000

  lazy val validAASApplicationDeatils: VatApplication = VatApplication(
    Some(true), Some(true),
    Some(testTurnover),
    None,
    Some(12.99),
    claimVatRefunds = Some(false),
    Some(Annual),
    Some(JanDecStagger),
    Some(testDate),
    Some(validAASDetails),
    None,
    None,
    None,
    None
  )

  val testWarehouseNumber = "test12345678"
  val testWarehouseName = "testWarehouseName"

  val testOverseasVatApplicationDetails: VatApplication = testVatApplicationDetails.copy(
    startDate = None,
    overseasCompliance = Some(OverseasCompliance(
      goodsToOverseas = true,
      goodsToEu = Some(true),
      storingGoodsForDispatch = StoringWithinUk,
      usingWarehouse = Some(true),
      fulfilmentWarehouseNumber = Some(testWarehouseNumber),
      fulfilmentWarehouseName = Some(testWarehouseName)
    )))

  lazy val validFullFlatRateScheme: FlatRateScheme = FlatRateScheme(
    joinFrs = Some(true),
    overBusinessGoods = Some(true),
    estimateTotalSales = Some(BigDecimal(1234567891)),
    overBusinessGoodsPercent = Some(true),
    useThisRate = Some(true),
    frsStart = Some(testDate),
    categoryOfBusiness = Some("testCategory"),
    percent = Some(15),
    limitedCostTrader = Some(false)
  )
  lazy val validEmptyFlatRateScheme: FlatRateScheme = FlatRateScheme(joinFrs = Some(false))
  lazy val invalidEmptyFlatRateScheme: FlatRateScheme = FlatRateScheme(joinFrs = Some(true))

  lazy val testBusinessDescription = "testBusinessDescription"
  lazy val testLabourCompliance: ComplianceLabour = ComplianceLabour(numOfWorkersSupplied = Some(1000), intermediaryArrangement = None, supplyWorkers = true)
  lazy val testSicCode1 = "12345"
  lazy val testSicDesc1 = "testMainSicDesc"
  lazy val testSicDisplay1 = "testMainSicDisplay"
  lazy val testSic1 = SicCode(testSicCode1, testSicDesc1, testSicDesc1)
  lazy val testSicCode2 = "23456"
  lazy val testSicDesc2 = "testSicDesc2"
  lazy val testSicDisplay2 = "testSicDisplay2"
  lazy val testSic2 = SicCode(testSicCode2, testSicDesc2, testSicDesc2)
  lazy val testSicCode3 = "34567"
  lazy val testSicDesc3 = "testSicDesc3"
  lazy val testSicDisplay3 = "testSicDisplay3"
  lazy val testSic3 = SicCode(testSicCode3, testSicDesc3, testSicDesc3)
  lazy val testBusinessActivities: List[SicCode] = List(testSic1, testSic2, testSic3)
  lazy val testBusiness: Business = Business(
    hasTradingName = Some(true),
    tradingName = Some(testTradingName),
    shortOrgName = None,
    ppobAddress = Some(testAddress),
    email = Some(testEmail),
    telephoneNumber = Some(testTelephone),
    hasWebsite = Some(true),
    website = Some(testWebsite),
    contactPreference = Some(Email),
    welshLanguage = Some(false),
    hasLandAndProperty = Some(false),
    businessDescription = Some(testBusinessDescription),
    businessActivities = Some(testBusinessActivities),
    mainBusinessActivity = Some(testSic1),
    labourCompliance = Some(testLabourCompliance),
    otherBusinessInvolvement = Some(false)
  )

  val testSubmissionPayload = "testSubmissionPayload"
  val testEncodedPayload: String = Base64.getEncoder.encodeToString(testSubmissionPayload.getBytes(StandardCharsets.UTF_8))

  lazy val testFullVatScheme: VatScheme = testVatScheme.copy(
    bankAccount = Some(testBankAccount),
    flatRateScheme = Some(validFullFlatRateScheme),
    applicantDetails = Some(validApplicantDetails),
    eligibilitySubmissionData = Some(testEligibilitySubmissionData),
    confirmInformationDeclaration = Some(true),
    nrsSubmissionPayload = Some(testEncodedPayload),
    business = Some(testBusiness),
    vatApplication = Some(testVatApplicationDetails)
  )

  lazy val validFullBusinessDetails: Business = Business(hasTradingName = Some(true), tradingName = Some(testTradingName),
    shortOrgName = None, ppobAddress = None, email = None, telephoneNumber = None, hasWebsite = None, website = None,
    contactPreference = None, welshLanguage = None, hasLandAndProperty = None, businessDescription = None, businessActivities = None,
    mainBusinessActivity = None, labourCompliance = None, otherBusinessInvolvement = None)

  lazy val validFullOtherBusinessInvolvement: OtherBusinessInvolvement = OtherBusinessInvolvement(businessName = testCompanyName, hasVrn = true, vrn = Some(testVrn), hasUtr = Some(true), utr = Some(testUtr), stillTrading = true)

  object AuthTestData {

    import models.nonrepudiation.IdentityData
    import services.NonRepudiationService.NonRepudiationIdentityRetrievals
    import uk.gov.hmrc.auth.core.retrieve._
    import uk.gov.hmrc.auth.core.{ConfidenceLevel, CredentialStrength, User}

    val testInternalId = "testInternalId"
    val testExternalId = "testExternalId"
    val testAgentCode = "testAgentCode"
    val testConfidenceLevel = ConfidenceLevel.L200
    val testSautr = "testSautr"
    val testAuthName = uk.gov.hmrc.auth.core.retrieve.Name(Some("testFirstName"), Some("testLastName"))
    val testAuthDateOfBirth = LocalDate.now()
    val testEmail = "testEmail"
    val testAgentInformation = AgentInformation(Some("testAgentId"), Some("testAgentCode"), Some("testAgentFriendlyName"))
    val testGroupIdentifier = "testGroupIdentifier"
    val testCredentialRole = User
    val testMdtpInformation = MdtpInformation("testDeviceId", "testSessionId")
    val testItmpName = ItmpName(Some("testGivenName"), Some("testMiddleName"), Some("testFamilyName"))
    val testItmpDateOfBirth = LocalDate.now()
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
    val testLoginTimes = LoginTimes(LocalDateTime.now.toInstant(ZoneOffset.UTC), Some(LocalDateTime.now.toInstant(ZoneOffset.UTC)))

    val testNonRepudiationIdentityData: IdentityData = IdentityData(
      Some(testInternalId),
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

    implicit class RetrievalCombiner[A](a: A) {
      def ~[B](b: B): A ~ B = new ~(a, b)
    }

    val testAuthRetrievals: NonRepudiationIdentityRetrievals =
      Some(testAffinityGroup) ~
        Some(testInternalId) ~
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

  val testPersonalDetails = PersonalDetails(testName, Some(testNino), trn = None, arn = None, identifiersMatch = true, Some(testDate), Some(100))

}
