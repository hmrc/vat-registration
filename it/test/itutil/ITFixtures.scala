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

import enums.VatRegStatus
import models._
import models.api._
import models.api.vatapplication._
import models.sdes.PropertyExtractor._
import models.sdes._
import models.submission._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import java.nio.charset.StandardCharsets
import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.util.Base64

trait ITFixtures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  lazy val testArn = "testArn"
  val testScore = 100
  val testDate: LocalDate = LocalDate.of(2025, 5, 13)
  val testUtr = "testUtr"
  val testPostcode = "TF1 1NT"
  val testChrn = "testChrn"
  val testCasc = "testCasc"
  val testDateTime: LocalDateTime = LocalDateTime.of(testDate, LocalTime.of(9, 0))
  val startDate = testDate
  val testRegId = "regId"
  val testInternalid = "INT-123-456-789"
  val vatScheme = VatScheme(
    registrationId = testRegId,
    internalId = testInternalid,
    status = VatRegStatus.draft,
    createdDate = testDate,

  )
  val oldName = Name(first = Some("Bob"), middle = None, last = "Smith")
  val testTradingName = "trading-name"
  val testShortOrgName = "testShortOrgName"
  val testAuthProviderId = "authProviderId"
  val testWarehouseNumber = "tst123456789012"
  val testWarehouseName = "testWarehouseName"
  val testTurnover = 10000
  val testZeroRatedSupplies = 500
  val testNorthernIrelandProtocol: NIPCompliance = NIPCompliance(
    Some(ConditionalValue(answer = true, Some(testTurnover))),
    Some(ConditionalValue(answer = true, Some(testTurnover)))
  )
  val testPreviousBusinessName = "testPreviousBusinessName"
  val testVrn = "testVrn"

  val testVatApplication: VatApplication = VatApplication(
    Some(true), Some(true),
    standardRateSupplies = None,
    reducedRateSupplies = None,
    zeroRatedSupplies = Some(testZeroRatedSupplies),
    turnoverEstimate = Some(testTurnover),
    acceptTurnOverEstimate = Some(true),
    appliedForExemption = None,
    claimVatRefunds = Some(true),
    returnsFrequency = Some(Quarterly),
    staggerStart = Some(JanuaryStagger),
    startDate = Some(startDate),
    annualAccountingDetails = None,
    overseasCompliance = None,
    northernIrelandProtocol = Some(testNorthernIrelandProtocol),
    hasTaxRepresentative = Some(false),
    currentlyTrading = None
  )

  val aasDetails = vatapplication.AASDetails(
    paymentMethod = Some(StandingOrder),
    paymentFrequency = Some(MonthlyPayment)
  )

  val testAASVatApplicationDetails: VatApplication = VatApplication(
    Some(true), Some(true),
    turnoverEstimate = Some(testTurnover),
    appliedForExemption = None,
    zeroRatedSupplies = Some(testZeroRatedSupplies),
    claimVatRefunds = Some(true),
    returnsFrequency = Some(Annual),
    staggerStart = Some(JanDecStagger),
    startDate = Some(startDate),
    annualAccountingDetails = Some(aasDetails),
    overseasCompliance = None,
    northernIrelandProtocol = Some(testNorthernIrelandProtocol),
    hasTaxRepresentative = Some(false),
    currentlyTrading = None
  )

  lazy val testFirstName = "testFirstName"
  lazy val testLastName = "testLastName"
  val testFlatRateScheme: FlatRateScheme = FlatRateScheme(
    joinFrs = Some(true),
    overBusinessGoods = Some(true),
    estimateTotalSales = Some(BigDecimal(12345678)),
    overBusinessGoodsPercent = Some(true),
    useThisRate = Some(true),
    frsStart = Some(testDate),
    categoryOfBusiness = Some("123"),
    percent = Some(15),
    limitedCostTrader = Some(false)
  )
  val EstimateValue: Long = 1000L
  val zeroRatedTurnoverEstimate: Long = 1000L
  val testCountry: Country = Country(Some("GB"), None)
  val testAddress: Address = Address("line1", Some("line2"), None, None, None, Some("XX XX"), Some(testCountry), addressValidated = Some(true))
  val testFullAddress: Address = Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("line5"), Some("XX XX"), Some(testCountry), addressValidated = Some(true))
  val testOverseasAddress: Address = testFullAddress.copy(country = Some(Country(Some("EE"), None)), addressValidated = Some(false))
  val testContact: Contact = Contact(Some("skylake@vilikariet.com"), Some("1234567890"), Some(true))
  val testNino: String = "NB686868C"
  val testTrn: String = "testTrn"
  val testRole: RoleInTheBusiness = Director
  val testName: Name = Name(first = Some("Forename"), middle = None, last = "Surname")
  val testProperName: Name = Name(first = Some(testFirstName), middle = None, last = testLastName)
  val testFormerName: FormerName = FormerName(hasFormerName = Some(true), name = Some(oldName), change = Some(testDate))
  val testCompanyName: String = "testCompanyName"
  val testDateOfBirth: LocalDate = testDate
  val testCrn: String = "testCrn"
  val testCtUtr: String = "testCtUtr"
  val testSaUtr: String = "testSaUtr"
  val testDateOfIncorp: LocalDate = LocalDate.of(2020, 1, 2)
  val testBpSafeId: String = "testBpSafeId"
  val testWebsite: String = "www.foo.com"

  lazy val testOrganisationName: String = "testOrganisationName"
  lazy val testEmail: String = "test@test.com"
  lazy val testTelephone: String = "1234567890"

  val testUnregisteredApplicantDetails: ApplicantDetails = ApplicantDetails(
    personalDetails = Some(PersonalDetails(
      name = testName,
      nino = Some(testNino),
      arn = None,
      trn = None,
      identifiersMatch = true,
      dateOfBirth = Some(testDate),
      score = None
    )),
    entity = Some(IncorporatedEntity(
      companyName = Some(testCompanyName),
      companyNumber = testCrn,
      dateOfIncorporation = Some(testDateOfIncorp),
      ctutr = Some(testCtUtr),
      businessVerification = Some(BvUnchallenged),
      registration = NotCalledStatus,
      identifiersMatch = true,
      bpSafeId = None,
      chrn = None
    )),
    roleInTheBusiness = Some(testRole),
    currentAddress = Some(testFullAddress),
    contact = testContact,
    changeOfName = testFormerName,
    previousAddress = Some(testFullAddress)
  )

  val testRegisteredApplicantDetails: ApplicantDetails = ApplicantDetails(
    personalDetails = Some(PersonalDetails(
      name = testName,
      nino = Some(testNino),
      trn = None,
      arn = None,
      identifiersMatch = true,
      dateOfBirth = Some(testDate),
      score = None
    )),
    entity = Some(IncorporatedEntity(
      companyName = Some(testCompanyName),
      companyNumber = testCrn,
      dateOfIncorporation = Some(testDateOfIncorp),
      ctutr = Some(testCtUtr),
      identifiersMatch = true,
      businessVerification = Some(BvPass),
      registration = RegisteredStatus,
      bpSafeId = Some(testBpSafeId),
      chrn = None
    )),
    roleInTheBusiness = Some(testRole),
    currentAddress = Some(testAddress),
    contact = testContact,
    changeOfName = FormerName(),
    previousAddress = None
  )

  val testRegisteredSoleTraderApplicantDetails: ApplicantDetails =
    testRegisteredApplicantDetails.copy(
      entity = Some(SoleTraderIdEntity(
        testFirstName,
        testLastName,
        testDate,
        Some(testNino),
        sautr = Some(testSaUtr),
        trn = None,
        bpSafeId = Some(testBpSafeId),
        businessVerification = Some(BvPass),
        registration = RegisteredStatus,
        identifiersMatch = true
      )),
      roleInTheBusiness = Some(OwnerProprietor)
    )

  val testRegisteredSoleTraderApplicantDetailsNoBpSafeId: ApplicantDetails =
    testRegisteredApplicantDetails.copy(
      entity = Some(SoleTraderIdEntity(
        testFirstName,
        testLastName,
        testDate,
        Some(testNino),
        sautr = Some(testSaUtr),
        trn = None,
        bpSafeId = None,
        businessVerification = Some(BvPass),
        registration = FailedStatus,
        identifiersMatch = true
      )),
      roleInTheBusiness = Some(OwnerProprietor)
    )

  lazy val testBusinessDescription: String = "testBusinessDescription"
  lazy val testLabourCompliance: ComplianceLabour = ComplianceLabour(numOfWorkersSupplied = Some(1000), intermediaryArrangement = None, supplyWorkers = Some(true))
  lazy val testSicCode1: String = "12345"
  lazy val testSicDesc1: String = "testMainSicDesc"
  lazy val testSicDisplay1: String = "testMainSicDisplay"
  lazy val testSic1: SicCode = SicCode(testSicCode1, testSicDesc1, testSicDisplay1)
  lazy val testSicCode2 = "23456"
  lazy val testSicDesc2 = "testSicDesc2"
  lazy val testSicDisplay2 = "testSicDisplay2"
  lazy val testSic2: SicCode = SicCode(testSicCode2, testSicDesc2, testSicDisplay2)
  lazy val testSicCode3 = "34567"
  lazy val testSicDesc3: String = "testSicDesc3"
  lazy val testSicDisplay3 = "testSicDisplay3"
  lazy val testSic3: SicCode = SicCode(testSicCode3, testSicDesc3, testSicDisplay3)
  lazy val testBusinessActivities: List[SicCode] = List(testSic1, testSic2, testSic3)
  protected lazy val testBusiness: Business = Business(
    hasTradingName = Some(true),
    tradingName = Some(testTradingName),
    shortOrgName = None,
    ppobAddress = Some(testFullAddress),
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

  val testBankDetails: BankAccountDetails = BankAccountDetails(
    name = "testBankName",
    sortCode = "11-11-11",
    number = "01234567",
    status = ValidStatus
  )
  val testSubmittedSortCode = "111111"

  val testThreshold: Threshold = Threshold(mandatoryRegistration = true, Some(testDate), Some(testDate), Some(testDate))

  val testEligibilitySubmissionData: EligibilitySubmissionData = EligibilitySubmissionData(
    threshold = testThreshold,
    appliedForException = None,
    partyType = UkCompany,
    registrationReason = ForwardLook,
    isTransactor = false,
    fixedEstablishmentInManOrUk = true
  )

  val testEligibilitySubmissionDataSoleTrader: EligibilitySubmissionData = EligibilitySubmissionData(
    threshold = testThreshold,
    appliedForException = None,
    partyType = Individual,
    registrationReason = ForwardLook,
    isTransactor = false,
    fixedEstablishmentInManOrUk = true
  )

  val testNrsSubmissionPayload = "testNrsSubmissionPayload"
  val testEncodedPayload: String = Base64.getEncoder.encodeToString(testNrsSubmissionPayload.getBytes(StandardCharsets.UTF_8))

  lazy val testVatScheme: VatScheme = VatScheme(testRegId, internalId = testInternalid, status = VatRegStatus.draft, createdDate = testDate)

  lazy val testFullVatScheme: VatScheme = testVatScheme.copy(
    bankAccount = Some(BankAccount(isProvided = true, Some(testBankDetails), None)),
    flatRateScheme = Some(testFlatRateScheme),
    applicantDetails = Some(testUnregisteredApplicantDetails),
    eligibilitySubmissionData = Some(testEligibilitySubmissionData),
    confirmInformationDeclaration = Some(true),
    vatApplication = Some(testVatApplication),
    nrsSubmissionPayload = Some(testEncodedPayload),
    business = Some(testBusiness)
  )

  lazy val testFullVatSchemeWithUnregisteredBusinessPartner: VatScheme =
    VatScheme(
      registrationId = testRegId,
      internalId = testInternalid,
      createdDate = testDate,
      vatApplication = Some(testAASVatApplicationDetails),
      bankAccount = Some(BankAccount(isProvided = true, Some(testBankDetails), None)),
      acknowledgementReference = Some("ackRef"),
      flatRateScheme = Some(testFlatRateScheme),
      status = VatRegStatus.draft,
      applicantDetails = Some(testUnregisteredApplicantDetails),
      eligibilitySubmissionData = Some(testEligibilitySubmissionData),
      confirmInformationDeclaration = Some(true),
      nrsSubmissionPayload = Some(testEncodedPayload),
      business = Some(testBusiness)
    )

  val testAgentTransactorDetails: TransactorDetails = TransactorDetails(
    personalDetails = Some(PersonalDetails(
      name = Name(Some(testFirstName), None, testLastName),
      nino = None,
      trn = None,
      arn = Some(testArn),
      identifiersMatch = true,
      dateOfBirth = None,
      score = None
    )),
    telephone = Some(testTelephone),
    email = Some(testEmail),
    isPartOfOrganisation = None,
    organisationName = None,
    emailVerified = Some(true),
    address = None,
    declarationCapacity = Some(DeclarationCapacityAnswer(AccountantAgent))
  )

  lazy val testAgentVatScheme: VatScheme =
    VatScheme(
      registrationId = testRegId,
      internalId = testInternalid,
      createdDate = testDate,
      transactorDetails = Some(testAgentTransactorDetails),
      vatApplication = Some(testAASVatApplicationDetails),
      bankAccount = Some(BankAccount(isProvided = true, Some(testBankDetails), None)),
      acknowledgementReference = Some("ackRef"),
      flatRateScheme = Some(testFlatRateScheme),
      status = VatRegStatus.draft,
      applicantDetails = Some(testUnregisteredApplicantDetails),
      eligibilitySubmissionData = Some(testEligibilitySubmissionData),
      confirmInformationDeclaration = Some(true),
      nrsSubmissionPayload = Some(testEncodedPayload),
      business = Some(testBusiness)
    )

  lazy val testMinimalVatSchemeWithRegisteredBusinessPartner: VatScheme =
    VatScheme(
      registrationId = testRegId,
      internalId = testInternalid,
      createdDate = testDate,
      vatApplication = Some(testVatApplication),
      bankAccount = Some(BankAccount(isProvided = false, None, Some(BeingSetup))),
      acknowledgementReference = Some("ackRef"),
      flatRateScheme = Some(FlatRateScheme(joinFrs = Some(false))),
      status = VatRegStatus.draft,
      applicantDetails = Some(testRegisteredApplicantDetails),
      eligibilitySubmissionData = Some(testEligibilitySubmissionData),
      confirmInformationDeclaration = Some(true),
      nrsSubmissionPayload = Some(testEncodedPayload),
      business = Some(testBusiness)
    )

  lazy val testMinimalVatSchemeWithVerifiedSoleTrader: VatScheme =
    testMinimalVatSchemeWithRegisteredBusinessPartner.copy(
      applicantDetails = Some(testRegisteredSoleTraderApplicantDetails),
      eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = Individual)),
      nrsSubmissionPayload = Some(testEncodedPayload)
    )

  lazy val testMinimalVatSchemeWithTrust: VatScheme =
    testMinimalVatSchemeWithRegisteredBusinessPartner.copy(
      applicantDetails = Some(testRegisteredApplicantDetails.copy(entity = Some(testTrustEntity))),
      eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = Trust)),
      nrsSubmissionPayload = Some(testEncodedPayload)
    )

  lazy val testSoleTraderVatScheme: VatScheme =
    testFullVatScheme.copy(
      applicantDetails = Some(testRegisteredSoleTraderApplicantDetailsNoBpSafeId),
      eligibilitySubmissionData = Some(testEligibilitySubmissionDataSoleTrader),
      nrsSubmissionPayload = Some(testEncodedPayload)
    )

  val testSoleTraderEntity: SoleTraderIdEntity = SoleTraderIdEntity(
    testFirstName,
    testLastName,
    testDate,
    Some(testNino),
    sautr = Some(testUtr),
    trn = None,
    businessVerification = Some(BvPass),
    registration = FailedStatus,
    identifiersMatch = true
  )

  val testLtdCoEntity: IncorporatedEntity = IncorporatedEntity(
    companyName = Some(testCompanyName),
    companyNumber = testCrn,
    ctutr = Some(testUtr),
    dateOfIncorporation = Some(testDateOfIncorp),
    businessVerification = Some(BvFail),
    registration = NotCalledStatus,
    identifiersMatch = true,
    chrn = None
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

  def testEmptyVatScheme(regId: String): VatScheme = VatScheme(
    registrationId = regId,
    internalId = testInternalid,
    status = VatRegStatus.draft,
    createdDate = testDate
  )

  object AuthTestData {

    import models.nonrepudiation.IdentityData
    import services.NonRepudiationService.NonRepudiationIdentityRetrievals
    import uk.gov.hmrc.auth.core.retrieve._
    import uk.gov.hmrc.auth.core.{ConfidenceLevel, CredentialStrength, User}

    val testExternalId = "testExternalId"
    val testAgentCode = "testAgentCode"
    val testConfidenceLevel: ConfidenceLevel = ConfidenceLevel.L200
    val testSautr = "testSautr"
    val testAuthName: Name = uk.gov.hmrc.auth.core.retrieve.Name(Some("testFirstName"), Some("testLastName"))
    val testAuthDateOfBirth: LocalDate = LocalDate.now()
    val testEmail: String = "testEmail"
    val testAgentInformation: AgentInformation = AgentInformation(Some("testAgentId"), Some("testAgentCode"), Some("testAgentFriendlyName"))
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
    val testLoginTimes: LoginTimes = LoginTimes(LocalDateTime.now().toInstant(ZoneOffset.UTC), Some(LocalDateTime.now().toInstant(ZoneOffset.UTC)))
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

  val testNetpVatApplication: VatApplication = VatApplication(
    None, None,
    standardRateSupplies = None,
    reducedRateSupplies = None,
    zeroRatedSupplies = Some(testZeroRatedSupplies),
    turnoverEstimate = Some(testTurnover),
    acceptTurnOverEstimate = Some(true),
    appliedForExemption = None,
    claimVatRefunds = Some(true),
    returnsFrequency = Some(Quarterly),
    staggerStart = Some(JanuaryStagger),
    startDate = None,
    annualAccountingDetails = None,
    overseasCompliance = Some(OverseasCompliance(
      Some(true),
      Some(true),
      Some(StoringWithinUk),
      Some(true),
      Some(testWarehouseNumber),
      Some(testWarehouseName)
    )),
    None,
    hasTaxRepresentative = Some(false),
    currentlyTrading = None
  )

  val testNetpEntity: SoleTraderIdEntity = SoleTraderIdEntity(
    testFirstName,
    testLastName,
    testDate,
    None,
    Some(testSaUtr),
    Some(testTrn),
    businessVerification = Some(BvPass),
    registration = FailedStatus,
    identifiersMatch = true
  )

  val testNetpEntityOverseas: SoleTraderIdEntity = SoleTraderIdEntity(
    testFirstName,
    testLastName,
    testDate,
    None,
    Some(testSaUtr),
    Some(testTrn),
    businessVerification = Some(BvUnchallenged),
    registration = NotCalledStatus,
    identifiersMatch = true,
    overseas = Some(OverseasIdentifierDetails("1234", "FR"))
  )

  val testNetpEligibilitySubmissionData: EligibilitySubmissionData = EligibilitySubmissionData(
    threshold = Threshold(mandatoryRegistration = true, None, None, None, Some(testDate)),
    appliedForException = None,
    partyType = NETP,
    registrationReason = NonUk,
    isTransactor = false,
    fixedEstablishmentInManOrUk = false
  )

  val testNetpTransactorDetails: PersonalDetails = PersonalDetails(
    name = testName,
    nino = None,
    arn = None,
    trn = Some(testTrn),
    identifiersMatch = false,
    dateOfBirth = Some(testDate),
    score = None
  )

  val testNetpApplicantDetails: ApplicantDetails =
    testRegisteredApplicantDetails.copy(
      entity = Some(testNetpEntity),
      personalDetails = Some(testNetpTransactorDetails),
      currentAddress = Some(testOverseasAddress),
      roleInTheBusiness = Some(OwnerProprietor)
    )

  lazy val testNetpVatScheme: VatScheme =
    testFullVatScheme.copy(
      applicantDetails = Some(testNetpApplicantDetails),
      bankAccount = None,
      eligibilitySubmissionData = Some(testNetpEligibilitySubmissionData),
      vatApplication = Some(testNetpVatApplication),
      flatRateScheme = None,
      attachments = Some(Attachments(Some(Post))),
      business = Some(testBusiness.copy(ppobAddress = Some(testOverseasAddress)))
    )

  lazy val testNonUkCompanyEligibilitySubmissionData: EligibilitySubmissionData =
    testNetpEligibilitySubmissionData.copy(
      partyType = NonUkNonEstablished
    )

  lazy val testNonUkCompanyEntity: MinorEntity = MinorEntity(
    Some(testCompanyName),
    Some(testCtUtr),
    None,
    Some(OverseasIdentifierDetails("1234", "FR")),
    None,
    None,
    None,
    businessVerification = Some(BvUnchallenged),
    registration = NotCalledStatus,
    identifiersMatch = true
  )

  lazy val testNonUkCompanyApplicantDetails: ApplicantDetails =
    testRegisteredApplicantDetails.copy(
      entity = Some(testNonUkCompanyEntity),
      personalDetails = Some(testNetpTransactorDetails),
      currentAddress = Some(testOverseasAddress),
      roleInTheBusiness = Some(Director)
    )

  lazy val testNonUkCompanyVatScheme: VatScheme =
    testNetpVatScheme.copy(
      applicantDetails = Some(testNonUkCompanyApplicantDetails),
      eligibilitySubmissionData = Some(testNonUkCompanyEligibilitySubmissionData)
    )

  val testPersonalDetails: PersonalDetails = PersonalDetails(testProperName, Some(testNino), trn = None, arn = None, identifiersMatch = true, Some(testDate), None)

  lazy val testTransactorDetails: TransactorDetails = TransactorDetails(
    personalDetails = Some(testPersonalDetails),
    isPartOfOrganisation = Some(true),
    organisationName = Some(testOrganisationName),
    telephone = Some(testTelephone),
    email = Some(testEmail),
    emailVerified = Some(true),
    address = Some(testFullAddress),
    declarationCapacity = Some(DeclarationCapacityAnswer(AuthorisedEmployee))
  )

  lazy val testOtherBusinessInvolvement: OtherBusinessInvolvement = OtherBusinessInvolvement(
    businessName = Some(testCompanyName),
    hasVrn = Some(true),
    vrn = Some(testVrn),
    hasUtr = Some(true),
    utr = Some(testUtr),
    stillTrading = Some(true)
  )

  val testReference = "testReference"
  val testReference2 = "testReference2"

  val testDownloadUrl = "testDownloadUrl"
  val testFileName = "testFileName"
  val testMimeType = "testMimeType"
  val testTimeStamp: LocalDateTime = LocalDateTime.now()
  val testChecksum = "1234567890"
  val testSize = 123
  val testFormBundleId = "123412341234"
  val testNonRepudiationSubmissionId = "testNonRepudiationSubmissionId"
  val testCorrelationid = "testCorrelationid"

  def testSdesPayload(attachmentReference: String): SdesNotification = SdesNotification(
    informationType = "1655996667080",
    file = FileDetails(
      recipientOrSender = "400063095160",
      name = s"$testFormBundleId-$testFileName",
      location = testDownloadUrl,
      checksum = Checksum(
        algorithm = checksumAlgorithm,
        value = testChecksum
      ),
      size = testSize,
      properties = List(
        Property(
          name = locationKey,
          value = testDownloadUrl
        ),
        Property(
          name = mimeTypeKey,
          value = testMimeType
        ),
        Property(
          name = prefixedFormBundleKey,
          value = s"VRS$testFormBundleId"
        ),
        Property(
          name = formBundleKey,
          value = testFormBundleId
        ),
        Property(
          name = attachmentReferenceKey,
          value = attachmentReference
        ),
        Property(
          name = submissionDateKey,
          value = testTimeStamp.format(dateTimeFormatter)
        ),
        Property(
          name = nrsSubmissionKey,
          value = testNonRepudiationSubmissionId
        )
      )
    ),
    audit = AuditDetals(
      correlationID = testCorrelationid
    )
  )

  def testUpscanDetails(reference: String): UpscanDetails = UpscanDetails(
    Some(testRegId),
    reference,
    Some(PrimaryIdentityEvidence),
    Some(testDownloadUrl),
    Ready,
    Some(UploadDetails(
      fileName = testFileName,
      fileMimeType = testMimeType,
      uploadTimestamp = testTimeStamp,
      checksum = testChecksum,
      size = testSize
    )),
    None
  )
}
