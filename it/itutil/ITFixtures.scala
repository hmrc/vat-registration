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
import models.api.returns._
import models.sdes.PropertyExtractor._
import models.sdes._
import models.submission._
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import java.nio.charset.StandardCharsets
import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.Base64

trait ITFixtures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  lazy val testArn = "testArn"
  val testDate: LocalDate = LocalDate.of(2017, 1, 1)
  val testUtr = "testUtr"
  val testPostcode = "TF1 1NT"
  val testChrn = "testChrn"
  val testCasc = "testCasc"
  val testDateTime: LocalDateTime = LocalDateTime.of(testDate, LocalTime.of(0, 0))
  val startDate = testDate
  val testRegId = "regId"
  val testInternalid = "INT-123-456-789"
  val vatScheme = VatScheme(testRegId, internalId = testInternalid, status = VatRegStatus.draft)
  val oldName = Name(first = Some("Bob"), middle = None, last = "Smith")
  val testTradingName = "trading-name"
  val testShortOrgName = "testShortOrgName"
  val testTradingDetails = TradingDetails(Some(testTradingName), Some(true), None, Some(true))
  val testAuthProviderId = "authProviderId"
  val testWarehouseNumber = "tst123456789012"
  val testWarehouseName = "testWarehouseName"
  val testTurnover = 10000
  val testNorthernIrelandProtocol: NIPCompliance = NIPCompliance(
    ConditionalValue(answer = true, Some(testTurnover)),
    ConditionalValue(answer = true, Some(testTurnover))
  )
  val testPreviousBusinessName = "testPreviousBusinessName"
  val testVrn = "testVrn"

  val testReturns: Returns = Returns(
    turnoverEstimate = testTurnover,
    appliedForExemption = None,
    zeroRatedSupplies = Some(12.99),
    reclaimVatOnMostReturns = true,
    returnsFrequency = Quarterly,
    staggerStart = JanuaryStagger,
    startDate = Some(startDate),
    annualAccountingDetails = None,
    overseasCompliance = None,
    northernIrelandProtocol = Some(testNorthernIrelandProtocol),
    hasTaxRepresentative = Some(false)
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
    turnoverEstimate = testTurnover,
    appliedForExemption = None,
    zeroRatedSupplies = Some(12.99),
    reclaimVatOnMostReturns = true,
    returnsFrequency = Annual,
    staggerStart = JanDecStagger,
    startDate = Some(startDate),
    annualAccountingDetails = Some(aasDetails),
    overseasCompliance = None,
    northernIrelandProtocol = Some(testNorthernIrelandProtocol),
    hasTaxRepresentative = Some(false)
  )
  lazy val testFirstName = "testFirstName"
  lazy val testLastName = "testLastName"
  val testFlatRateScheme = FlatRateScheme(joinFrs = true, Some(frsDetails))
  val EstimateValue: Long = 1000L
  val zeroRatedTurnoverEstimate: Long = 1000L
  val testCountry = Country(Some("GB"), None)
  val testAddress = Address("line1", Some("line2"), None, None, None, Some("XX XX"), Some(testCountry), addressValidated = Some(true))
  val testFullAddress = Address("line1", Some("line2"), Some("line3"), Some("line4"), Some("line5"), Some("XX XX"), Some(testCountry), addressValidated = Some(true))
  val testOverseasAddress = testFullAddress.copy(country = Some(Country(Some("EE"), None)), addressValidated = Some(false))
  val testDigitalContactOptional = DigitalContactOptional(Some("skylake@vilikariet.com"), Some("1234567890"), Some("1234567890"), Some(true))
  val testNino = "NB686868C"
  val testTrn = "testTrn"
  val testRole = Director
  val testName = Name(first = Some("Forename"), middle = None, last = "Surname")
  val testProperName = Name(first = Some(testFirstName), middle = None, last = testLastName)
  val testFormerName = FormerName(hasFormerName = Some(true), name = Some(oldName), change = Some(testDate))
  val testCompanyName = "testCompanyName"
  val testDateOfBirth = DateOfBirth(testDate)
  val testCrn = "testCrn"
  val testCtUtr = "testCtUtr"
  val testSaUtr = "testSaUtr"
  val testDateOfIncorp = LocalDate.of(2020, 1, 2)
  val testBpSafeId = "testBpSafeId"
  val testWebsite = "www.foo.com"

  lazy val testOrganisationName = "testOrganisationName"
  lazy val testEmail = "test@test.com"
  lazy val testTelephone = "1234567890"

  val testUnregisteredApplicantDetails: ApplicantDetails = ApplicantDetails(
    personalDetails = PersonalDetails(
      name = testName,
      nino = Some(testNino),
      arn = None,
      trn = None,
      identifiersMatch = true,
      dateOfBirth = Some(testDate)
    ),
    entity = IncorporatedEntity(
      companyName = Some(testCompanyName),
      companyNumber = testCrn,
      dateOfIncorporation = Some(testDateOfIncorp),
      ctutr = Some(testCtUtr),
      businessVerification = Some(BvUnchallenged),
      registration = NotCalledStatus,
      identifiersMatch = true,
      bpSafeId = None,
      chrn = None
    ),
    roleInBusiness = testRole,
    currentAddress = testFullAddress,
    contact = testDigitalContactOptional,
    changeOfName = Some(testFormerName),
    previousAddress = Some(testFullAddress)
  )

  val testRegisteredApplicantDetails: ApplicantDetails = ApplicantDetails(
    personalDetails = PersonalDetails(
      name = testName,
      nino = Some(testNino),
      trn = None,
      arn = None,
      identifiersMatch = true,
      dateOfBirth = Some(testDate)
    ),
    entity = IncorporatedEntity(
      companyName = Some(testCompanyName),
      companyNumber = testCrn,
      dateOfIncorporation = Some(testDateOfIncorp),
      ctutr = Some(testCtUtr),
      identifiersMatch = true,
      businessVerification = Some(BvPass),
      registration = RegisteredStatus,
      bpSafeId = Some(testBpSafeId),
      chrn = None
    ),
    roleInBusiness = testRole,
    currentAddress = testAddress,
    contact = testDigitalContactOptional,
    changeOfName = None,
    previousAddress = None
  )

  val testRegisteredSoleTraderApplicantDetails: ApplicantDetails =
    testRegisteredApplicantDetails.copy(
      entity = SoleTraderIdEntity(
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
      ),
      roleInBusiness = OwnerProprietor
    )

  val testRegisteredSoleTraderApplicantDetailsNoBpSafeId: ApplicantDetails =
    testRegisteredApplicantDetails.copy(
      entity = SoleTraderIdEntity(
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
      ),
      roleInBusiness = OwnerProprietor
    )

  val testBusinessContactDetails = BusinessContact(email = Some("test@test.com"), telephoneNumber = Some("12345678910"), mobile = Some("12345678910"), website = None, ppob = testFullAddress, commsPreference = Email, hasWebsite = Some(false))
  val testFullBusinessContactDetails = BusinessContact(email = Some("test@test.com"), telephoneNumber = Some("12345678910"), mobile = Some("12345678910"), website = Some(testWebsite), ppob = testFullAddress, commsPreference = Email, hasWebsite = Some(true))

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

  val testBankDetails = BankAccountDetails(
    name = "testBankName",
    sortCode = "11-11-11",
    number = "01234567",
    status = ValidStatus
  )
  val testSubmittedSortCode = "111111"

  val testThreshold = Threshold(mandatoryRegistration = true, Some(testDate), Some(testDate), Some(testDate))

  val testEligibilitySubmissionData: EligibilitySubmissionData = EligibilitySubmissionData(
    threshold = testThreshold,
    appliedForException = None,
    partyType = UkCompany,
    registrationReason = ForwardLook,
    isTransactor = false
  )

  val testEligibilitySubmissionDataSoleTrader: EligibilitySubmissionData = EligibilitySubmissionData(
    threshold = testThreshold,
    appliedForException = None,
    partyType = Individual,
    registrationReason = ForwardLook,
    isTransactor = false
  )

  val testNrsSubmissionPayload = "testNrsSubmissionPayload"
  val testEncodedPayload: String = Base64.getEncoder.encodeToString(testNrsSubmissionPayload.getBytes(StandardCharsets.UTF_8))

  lazy val testVatScheme: VatScheme = VatScheme(testRegId, internalId = testInternalid, status = VatRegStatus.draft, createdDate = Some(testDate))

  lazy val testFullVatScheme: VatScheme = testVatScheme.copy(
    tradingDetails = Some(testTradingDetails),
    sicAndCompliance = Some(testFullSicAndCompliance),
    businessContact = Some(testFullBusinessContactDetails),
    bankAccount = Some(BankAccount(isProvided = true, Some(testBankDetails), None, None)),
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
      tradingDetails = Some(testTradingDetails),
      returns = Some(testAASReturns),
      sicAndCompliance = Some(testSicAndCompliance),
      businessContact = Some(testBusinessContactDetails),
      bankAccount = Some(BankAccount(isProvided = true, Some(testBankDetails), None, None)),
      acknowledgementReference = Some("ackRef"),
      flatRateScheme = Some(testFlatRateScheme),
      status = VatRegStatus.draft,
      applicantDetails = Some(testUnregisteredApplicantDetails),
      eligibilitySubmissionData = Some(testEligibilitySubmissionData),
      confirmInformationDeclaration = Some(true),
      nrsSubmissionPayload = Some(testEncodedPayload)
    )

  val testAgentTransactorDetails = TransactorDetails(
    personalDetails = PersonalDetails(
      name = Name(Some(testFirstName), None, testLastName),
      nino = None,
      trn = None,
      arn = Some(testArn),
      identifiersMatch = true,
      dateOfBirth = None
    ),
    telephone = testTelephone,
    email = testEmail,
    isPartOfOrganisation = None,
    emailVerified = true,
    address = None,
    declarationCapacity = DeclarationCapacityAnswer(AccountantAgent)
  )

  lazy val testAgentVatScheme: VatScheme =
    VatScheme(
      id = testRegId,
      internalId = testInternalid,
      tradingDetails = Some(testTradingDetails),
      transactorDetails = Some(testAgentTransactorDetails),
      returns = Some(testAASReturns),
      sicAndCompliance = Some(testSicAndCompliance),
      businessContact = Some(testBusinessContactDetails),
      bankAccount = Some(BankAccount(isProvided = true, Some(testBankDetails), None, None)),
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
      tradingDetails = Some(testTradingDetails),
      returns = Some(testReturns),
      sicAndCompliance = Some(testSicAndCompliance),
      businessContact = Some(testBusinessContactDetails),
      bankAccount = Some(BankAccount(isProvided = false, None, None, Some(BeingSetup))),
      acknowledgementReference = Some("ackRef"),
      flatRateScheme = Some(FlatRateScheme(joinFrs = false, None)),
      status = VatRegStatus.draft,
      applicantDetails = Some(testRegisteredApplicantDetails),
      eligibilitySubmissionData = Some(testEligibilitySubmissionData),
      confirmInformationDeclaration = Some(true),
      nrsSubmissionPayload = Some(testEncodedPayload)
    )

  lazy val testMinimalVatSchemeWithVerifiedSoleTrader: VatScheme =
    testMinimalVatSchemeWithRegisteredBusinessPartner.copy(
      applicantDetails = Some(testRegisteredSoleTraderApplicantDetails),
      eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = Individual)),
      nrsSubmissionPayload = Some(testEncodedPayload)
    )

  lazy val testMinimalVatSchemeWithTrust: VatScheme =
    testMinimalVatSchemeWithRegisteredBusinessPartner.copy(
      applicantDetails = Some(testRegisteredApplicantDetails.copy(entity = testTrustEntity)),
      eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = Trust)),
      nrsSubmissionPayload = Some(testEncodedPayload)
    )

  lazy val testSoleTraderVatScheme: VatScheme =
    testFullVatScheme.copy(
      applicantDetails = Some(testRegisteredSoleTraderApplicantDetailsNoBpSafeId),
      eligibilitySubmissionData = Some(testEligibilitySubmissionDataSoleTrader),
      nrsSubmissionPayload = Some(testEncodedPayload)
    )

  val testSoleTraderEntity = SoleTraderIdEntity(
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

  val testLtdCoEntity = IncorporatedEntity(
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
    id = regId,
    internalId = testInternalid,
    status = VatRegStatus.draft,
    createdDate = Some(testDate)
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

  val testNetpReturns: Returns = Returns(
    turnoverEstimate = testTurnover,
    appliedForExemption = None,
    zeroRatedSupplies = Some(12.99),
    reclaimVatOnMostReturns = true,
    returnsFrequency = Quarterly,
    staggerStart = JanuaryStagger,
    startDate = None,
    annualAccountingDetails = None,
    overseasCompliance = Some(OverseasCompliance(
      true,
      Some(true),
      StoringWithinUk,
      Some(true),
      Some(testWarehouseNumber),
      Some(testWarehouseName)
    )),
    None,
    hasTaxRepresentative = Some(false)
  )

  val testNetpTradingDetails: TradingDetails = TradingDetails(
    Some(testTradingName),
    None,
    None,
    None
  )

  val testNetpEntity = SoleTraderIdEntity(
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

  val testNetpEntityOverseas = SoleTraderIdEntity(
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
    isTransactor = false
  )

  val testNetpTransactorDetails: PersonalDetails = PersonalDetails(
    name = testName,
    nino = None,
    arn = None,
    trn = Some(testTrn),
    identifiersMatch = false,
    dateOfBirth = Some(testDate)
  )

  val testNetpApplicantDetails: ApplicantDetails =
    testRegisteredApplicantDetails.copy(
      entity = testNetpEntity,
      personalDetails = testNetpTransactorDetails,
      currentAddress = testOverseasAddress,
      roleInBusiness = OwnerProprietor
    )

  val testNetpBusinessContact: BusinessContact = testFullBusinessContactDetails.copy(
    ppob = testOverseasAddress
  )

  lazy val testNetpVatScheme: VatScheme =
    testFullVatScheme.copy(
      applicantDetails = Some(testNetpApplicantDetails),
      bankAccount = None,
      eligibilitySubmissionData = Some(testNetpEligibilitySubmissionData),
      returns = Some(testNetpReturns),
      tradingDetails = Some(testNetpTradingDetails),
      flatRateScheme = None,
      businessContact = Some(testNetpBusinessContact),
      attachments = Some(Attachments(Post))
    )

  lazy val testNonUkCompanyEligibilitySubmissionData: EligibilitySubmissionData =
    testNetpEligibilitySubmissionData.copy(
      partyType = NonUkNonEstablished
    )

  lazy val testNonUkCompanyEntity = MinorEntity(
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
      entity = testNonUkCompanyEntity,
      personalDetails = testNetpTransactorDetails,
      currentAddress = testOverseasAddress,
      roleInBusiness = Director
    )

  lazy val testNonUkCompanyVatScheme: VatScheme =
    testNetpVatScheme.copy(
      applicantDetails = Some(testNonUkCompanyApplicantDetails),
      eligibilitySubmissionData = Some(testNonUkCompanyEligibilitySubmissionData)
    )

  val testPersonalDetails = PersonalDetails(testProperName, Some(testNino), trn = None, arn = None, identifiersMatch = true, Some(testDate))

  lazy val testTransactorDetails = TransactorDetails(
    personalDetails = testPersonalDetails,
    isPartOfOrganisation = Some(true),
    organisationName = Some(testOrganisationName),
    telephone = testTelephone,
    email = testEmail,
    emailVerified = true,
    address = Some(testFullAddress),
    declarationCapacity = DeclarationCapacityAnswer(AuthorisedEmployee)
  )

  lazy val testOtherBusinessInvolvement = OtherBusinessInvolvement(
    businessName = testCompanyName,
    hasVrn = true,
    vrn = Some(testVrn),
    hasUtr = Some(true),
    utr = Some(testUtr),
    stillTrading = true
  )

  val testReference = "testReference"
  val testReference2 = "testReference2"

  val testDownloadUrl = "testDownloadUrl"
  val testFileName = "testFileName"
  val testMimeType = "testMimeType"
  val testTimeStamp = LocalDateTime.now()
  val testChecksum = "1234567890"
  val testSize = 123
  val testFormBundleId = "123412341234"
  val testNonRepudiationSubmissionId = "testNonRepudiationSubmissionId"
  val testCorrelationid = "testCorrelationid"

  def testSdesPayload(attachmentReference: String): SdesNotification = SdesNotification(
    informationType = "S18",
    file = FileDetails(
      recipientOrSender = "123456789012",
      name = testFileName,
      location = testDownloadUrl,
      checksum = Checksum(
        algorithm = checksumAlgorithm,
        value = testChecksum
      ),
      size = testSize,
      properties = List(
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
