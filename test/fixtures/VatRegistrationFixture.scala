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
import models._
import models.api._
import models.api.vatapplication._
import models.nonrepudiation.NonRepudiationAttachment
import models.submission._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.retrieve.Credentials

import java.nio.charset.StandardCharsets
import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.util.Base64

trait VatRegistrationFixture {
  lazy val testNino: String = "AB123456A"
  lazy val testRole: RoleInTheBusiness = Director
  lazy val testArn: String = "testArn"
  lazy val testRegId: String = "testRegId"
  val testNrAttachmentId: String = "testNrAttachmentId"
  lazy val testInternalId: String = "INT-123-456-789"
  lazy val testAckReference: String = "BRPY000000000001"
  lazy val testDate: LocalDate         = LocalDate.of(2018, 1, 1)
  lazy val testDateTime: LocalDateTime = LocalDateTime.of(testDate, LocalTime.MIDNIGHT)
  lazy val testDateOfBirth: LocalDate = testDate
  lazy val testCompanyName: String = "testCompanyName"
  lazy val testCrn: String = "testCrn"
  lazy val testVrn: String = "testVrn"
  lazy val testUtr: String = "testUtr"
  lazy val testChrn: String = "testChrn"
  lazy val testCasc: String = "testCasc"
  lazy val testTrn: String = "testTrn"
  lazy val testDateOFIncorp: LocalDate = LocalDate.of(2020, 1, 2)
  lazy val testAddress: Address = Address(
    "line1",
    Some("line2"),
    None,
    None,
    None,
    Some("ZZ1 1ZZ"),
    Some(Country(Some("GB"), Some("UK"))),
    addressValidated = Some(true)
  )
  lazy val testPostcode: String = "ZZ1 1ZZ"
  lazy val testSicCode: SicCode = SicCode("88888", "description", "description")
  lazy val testName: Name = Name(first = Some("Forename"), middle = None, last = "Surname")
  lazy val testOldName: Name = Name(first = Some("Bob"), middle = None, last = "Smith")
  lazy val testPreviousName: FormerName = FormerName(hasFormerName = Some(true), name = Some(testOldName), change = Some(testDate))
  protected lazy val testVatScheme: VatScheme    =
    VatScheme(testRegId, internalId = testInternalId, status = VatRegStatus.draft, createdDate = testDate)
  lazy val exception: Exception = new Exception("Exception")
  lazy val testVoluntaryThreshold: Threshold = Threshold(mandatoryRegistration = false, None, None, None)
  lazy val testMandatoryThreshold: Threshold = Threshold(
    mandatoryRegistration = true,
    Some(LocalDate.of(2020, 10, 7)),
    Some(LocalDate.of(2020, 10, 7)),
    Some(LocalDate.of(2020, 10, 7))
  )

  lazy val testContact: Contact = Contact(Some("skylake@vilikariet.com"), None, None)
  lazy val testBankName: String = "Test Bank Account"
  lazy val testSortCode: String = "010203"
  lazy val testBankNumber: String = "01023456"
  lazy val testBankDetails: BankAccountDetails = BankAccountDetails(testBankName, testSortCode, testBankNumber, ValidStatus)
  lazy val testFormerName: FormerName = FormerName(hasFormerName = Some(true), Some(testName), Some(testDate))

  lazy val testStandardRateSupplies: Int = 1000
  lazy val testReducedRateSupplies: Int = 2000
  lazy val testZeroRateSupplies: Int = 500
  lazy val testTurnover: Int = 3500
  lazy val testAcceptTurnoverEstimate: Boolean = true

  lazy val testVatApplicationDetails: VatApplication = VatApplication(
    Some(true),
    Some(true),
    Some(testStandardRateSupplies),
    Some(testReducedRateSupplies),
    Some(testZeroRateSupplies),
    Some(testTurnover),
    Some(testAcceptTurnoverEstimate),
    None,
    Some(false),
    Some(Quarterly),
    Some(JanuaryStagger),
    Some(testDate),
    None,
    None,
    None,
    None,
    None
  )
  lazy val zeroRatedSupplies: BigDecimal    = 500
  lazy val testBpSafeId: String = "testBpSafeId"
  lazy val testFirstName: String = "testFirstName"
  lazy val testLastName: String = "testLastName"
  lazy val testWebsite: String = "www.foo.com"
  lazy val testProviderId: String           = "testProviderID"
  lazy val testProviderType: String         = "GovernmentGateway"
  lazy val testCredentials: Credentials     = Credentials(testProviderId, testProviderType)
  lazy val testAffinityGroup: AffinityGroup = AffinityGroup.Organisation
  lazy val testAuthToken: String = "testAuthToken"

  val testTradingName: String = "trading-name"
  val testShortOrgName: String = "short-org-name"
  val testUserHeaders: Map[String, String] = Map("testKey" -> "testValue")

  lazy val testEligibilitySubmissionData: EligibilitySubmissionData = EligibilitySubmissionData(
    threshold = testMandatoryThreshold,
    appliedForException = Some(false),
    partyType = UkCompany,
    registrationReason = ForwardLook,
    isTransactor = false,
    calculatedDate = testMandatoryThreshold.thresholdInTwelveMonths,
    fixedEstablishmentInManOrUk = true
  )

  val testLtdCoEntity: IncorporatedEntity = IncorporatedEntity(
    companyName = Some(testCompanyName),
    companyNumber = testCrn,
    ctutr = Some(testUtr),
    dateOfIncorporation = Some(testDateOFIncorp),
    businessVerification = Some(BvFail),
    registration = NotCalledStatus,
    identifiersMatch = true,
    chrn = None
  )

  val testSoleTraderEntity: SoleTraderIdEntity = SoleTraderIdEntity(
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
    personalDetails = Some(
      PersonalDetails(
        name = testName,
        nino = Some(testNino),
        trn = None,
        arn = None,
        identifiersMatch = true,
        dateOfBirth = Some(testDate),
        None
      )
    ),
    entity = Some(testLtdCoEntity),
    roleInTheBusiness = Some(testRole),
    currentAddress = Some(testAddress),
    contact = testContact,
    changeOfName = testFormerName,
    previousAddress = None
  )

  lazy val unverifiedUserApplicantDetails: ApplicantDetails = ApplicantDetails(
    personalDetails = Some(
      PersonalDetails(
        name = testName,
        nino = Some(testNino),
        trn = None,
        arn = None,
        identifiersMatch = false,
        dateOfBirth = Some(testDate),
        None
      )
    ),
    entity = Some(testLtdCoEntity),
    roleInTheBusiness = Some(testRole),
    currentAddress = Some(testAddress),
    contact = testContact,
    changeOfName = testFormerName,
    previousAddress = None
  )

  lazy val testEmail: String = "test@test.com"
  lazy val testTelephone: String = "1234567890"
  lazy val testOrgName: String = "testOrganisationName"
  lazy val validTransactorDetails: TransactorDetails = TransactorDetails(
    personalDetails = Some(
      PersonalDetails(
        name = testName,
        nino = Some(testNino),
        trn = None,
        arn = None,
        identifiersMatch = true,
        dateOfBirth = Some(testDate),
        None
      )
    ),
    isPartOfOrganisation = Some(true),
    organisationName = Some(testOrgName),
    telephone = Some(testTelephone),
    email = Some(testEmail),
    emailVerified = Some(true),
    address = Some(testAddress),
    declarationCapacity = Some(DeclarationCapacityAnswer(AuthorisedEmployee))
  )

  protected lazy val otherBusinessActivitiesSicAndCompiliance: List[SicCode] =
    SicCode("00998", "otherBusiness desc 1", "otherBusiness desc 1") :: SicCode(
      "00889",
      "otherBusiness desc 2",
      "otherBusiness desc 2"
    ) :: Nil

  lazy val testBankAccount: BankAccount = BankAccount(isProvided = true, details = Some(testBankDetails), None)
  lazy val testBankAccountNotProvided: BankAccount = BankAccount(isProvided = false, details = None, reason = Some(BeingSetup))

  protected lazy val validAASDetails: AASDetails = AASDetails(
    paymentMethod = Some(StandingOrder),
    paymentFrequency = Some(MonthlyPayment)
  )

  lazy val validAASApplicationDeatils: VatApplication = VatApplication(
    eoriRequested = Some(true),
    tradeVatGoodsOutsideUk = Some(true),
    standardRateSupplies = Some(testStandardRateSupplies),
    reducedRateSupplies = Some(testReducedRateSupplies),
    zeroRatedSupplies = Some(testZeroRateSupplies),
    turnoverEstimate = Some(testTurnover),
    acceptTurnOverEstimate = Some(testAcceptTurnoverEstimate),
    appliedForExemption = None,
    claimVatRefunds = Some(false),
    returnsFrequency = Some(Annual),
    staggerStart = Some(JanDecStagger),
    startDate = Some(testDate),
    annualAccountingDetails = Some(validAASDetails),
    overseasCompliance = None,
    northernIrelandProtocol = None,
    hasTaxRepresentative = None,
    currentlyTrading = None
  )

  val testWarehouseNumber: String = "test12345678"
  val testWarehouseName: String = "testWarehouseName"

  val testOverseasVatApplicationDetails: VatApplication = testVatApplicationDetails.copy(
    startDate = None,
    overseasCompliance = Some(
      OverseasCompliance(
        goodsToOverseas = Some(true),
        goodsToEu = Some(true),
        storingGoodsForDispatch = Some(StoringWithinUk),
        usingWarehouse = Some(true),
        fulfilmentWarehouseNumber = Some(testWarehouseNumber),
        fulfilmentWarehouseName = Some(testWarehouseName)
      )
    )
  )

  lazy val validFullFlatRateScheme: FlatRateScheme    = FlatRateScheme(
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
  lazy val validEmptyFlatRateScheme: FlatRateScheme   = FlatRateScheme(joinFrs = Some(false))
  lazy val invalidEmptyFlatRateScheme: FlatRateScheme = FlatRateScheme(joinFrs = Some(true))

  lazy val testBusinessDescription: String = "testBusinessDescription"
  lazy val testLabourCompliance: ComplianceLabour =
    ComplianceLabour(numOfWorkersSupplied = Some(1000), intermediaryArrangement = None, supplyWorkers = Some(true))
  lazy val testSicCode1: String = "12345"
  lazy val testSicDesc1: String = "testMainSicDesc"
  lazy val testSicDisplay1: String = "testMainSicDisplay"
  lazy val testSic1: SicCode = SicCode(testSicCode1, testSicDesc1, testSicDesc1)
  lazy val testSicCode2: String = "23456"
  lazy val testSicDesc2: String = "testSicDesc2"
  lazy val testSicDisplay2: String = "testSicDisplay2"
  lazy val testSic2: SicCode = SicCode(testSicCode2, testSicDesc2, testSicDesc2)
  lazy val testSicCode3: String = "34567"
  lazy val testSicDesc3: String = "testSicDesc3"
  lazy val testSicDisplay3: String = "testSicDisplay3"
  lazy val testSic3: SicCode = SicCode(testSicCode3, testSicDesc3, testSicDesc3)
  lazy val testBusinessActivities: List[SicCode]  = List(testSic1, testSic2, testSic3)
  lazy val testBusiness: Business                 = Business(
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

  val testSubmissionPayload: String = "testSubmissionPayload"
  protected val testEncodedPayload: String =
    Base64.getEncoder.encodeToString(testSubmissionPayload.getBytes(StandardCharsets.UTF_8))

  lazy val testFullVatScheme: VatScheme = testVatScheme.copy(
    bankAccount = Some(testBankAccount),
    flatRateScheme = Some(validFullFlatRateScheme),
    applicantDetails = Some(validApplicantDetails),
    eligibilitySubmissionData = Some(testEligibilitySubmissionData),
    confirmInformationDeclaration = Some(true),
    nrsSubmissionPayload = Some(testEncodedPayload),
    business = Some(testBusiness),
    vatApplication = Some(testVatApplicationDetails),
    attachments = Some(Attachments(method=Some(Attached)))
  )

  lazy val validFullBusinessDetails: Business = Business(
    hasTradingName = Some(true),
    tradingName = Some(testTradingName),
    shortOrgName = None,
    ppobAddress = None,
    email = None,
    telephoneNumber = None,
    hasWebsite = None,
    website = None,
    contactPreference = None,
    welshLanguage = None,
    hasLandAndProperty = None,
    businessDescription = None,
    businessActivities = None,
    mainBusinessActivity = None,
    labourCompliance = None,
    otherBusinessInvolvement = None
  )

  lazy val validFullOtherBusinessInvolvement: OtherBusinessInvolvement = OtherBusinessInvolvement(
    businessName = Some(testCompanyName),
    hasVrn = Some(true),
    vrn = Some(testVrn),
    hasUtr = Some(true),
    utr = Some(testUtr),
    stillTrading = Some(true)
  )

  object AuthTestData {

    import models.nonrepudiation.IdentityData
    import services.NonRepudiationService.NonRepudiationIdentityRetrievals
    import uk.gov.hmrc.auth.core.retrieve._
    import uk.gov.hmrc.auth.core.{ConfidenceLevel, CredentialStrength, User}

    val testInternalId: String = "testInternalId"
    val testExternalId: String = "testExternalId"
    val testAgentCode: String = "testAgentCode"
    val testConfidenceLevel: ConfidenceLevel = ConfidenceLevel.L200
    val testSautr: String = "testSautr"
    val testAuthName: Name = uk.gov.hmrc.auth.core.retrieve.Name(Some("testFirstName"), Some("testLastName"))
    val testAuthDateOfBirth: LocalDate = LocalDate.now()
    val testEmail: String = "testEmail"
    val testAgentInformation: AgentInformation =
      AgentInformation(Some("testAgentId"), Some("testAgentCode"), Some("testAgentFriendlyName"))
    val testGroupIdentifier: String = "testGroupIdentifier"
    val testCredentialRole: User.type = User
    val testMdtpInformation: MdtpInformation = MdtpInformation("testDeviceId", "testSessionId")
    val testItmpName: ItmpName = ItmpName(Some("testGivenName"), Some("testMiddleName"), Some("testFamilyName"))
    val testItmpDateOfBirth: LocalDate = LocalDate.now()
    val testItmpAddress: ItmpAddress = ItmpAddress(
      Some("testLine1"),
      None,
      None,
      None,
      None,
      Some("testPostcode"),
      None,
      None
    )
    val testCredentialStrength: String = CredentialStrength.strong
    val testLoginTimes: LoginTimes =
      LoginTimes(LocalDateTime.now.toInstant(ZoneOffset.UTC), Some(LocalDateTime.now.toInstant(ZoneOffset.UTC)))

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

  val testNonRepudiationAttachment: NonRepudiationAttachment = NonRepudiationAttachment(
    attachmentUrl = "downloadUrl",
    attachmentId = "testReference",
    attachmentSha256Checksum = "checksum",
    attachmentContentType = "fileMimeType",
    nrSubmissionId = "testNonRepudiationSubmissionId",
  )

  val testEmptyUpscanDetails: UpscanDetails = UpscanDetails(
    Some(testRegId),
    "testReference",
    Some(PrimaryIdentityEvidence),
    None,
    InProgress,
    None,
    None,
  )

  val testUpscanDetails: UpscanDetails = UpscanDetails(
    Some(testRegId),
    "testReference",
    Some(PrimaryIdentityEvidence),
    Some("downloadUrl"),
    InProgress,
    Some(UploadDetails("fileName", "fileMimeType", LocalDateTime.MIN, "checksum", 1)),
    None,
  )

  val testPersonalDetails: PersonalDetails = PersonalDetails(
    testName,
    Some(testNino),
    trn = None,
    arn = None,
    identifiersMatch = true,
    Some(testDate),
    Some(100)
  )

}
