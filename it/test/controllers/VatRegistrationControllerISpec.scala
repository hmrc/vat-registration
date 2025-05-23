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

package controllers


import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlMatching, verify}
import connectors.stubs.NonRepudiationStub.stubNonRepudiationSubmission
import connectors.stubs.SdesNotifyStub.stubSdesNotification
import featureswitch.core.config.{FeatureSwitching, StubSubmission}
import itutil.{FakeTimeMachine, ITVatSubmissionFixture, IntegrationStubbing}
import models.api._
import models.nonrepudiation.NonRepudiationMetadata
import models.submission.IdVerificationStatus.toJsString
import models.submission._
import models.{GroupRegistration, TogcCole, TransferOfAGoingConcern}
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.libs.json.{JsObject, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

class VatRegistrationControllerISpec extends IntegrationStubbing with FeatureSwitching with ITVatSubmissionFixture {

  val testNonRepudiationApiKey = "testNonRepudiationApiKey"
  override lazy val additionalConfig = Map("microservice.services.non-repudiation.api-key" -> testNonRepudiationApiKey)

  class Setup extends SetupHelper

  def testEncodedPayload(payload: String): String = Base64.getEncoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8))

  def testPayloadChecksum(payload: String): String = MessageDigest.getInstance("SHA-256")
    .digest(payload.getBytes(StandardCharsets.UTF_8))
    .map("%02x".format(_)).mkString

  FakeTimeMachine.hour = 9

  val formBundleId = "123412341234"
  val testSubmissionResponse = Json.obj("formBundle" -> formBundleId)
  val headerData = Map("testHeaderKey" -> "testHeaderValue")

  val testNonRepudiationMetadata: NonRepudiationMetadata = NonRepudiationMetadata(
    businessId = "vrs",
    notableEvent = "vat-registration",
    payloadContentType = "application/json",
    payloadSha256Checksum = testPayloadChecksum(testNrsSubmissionPayload),
    userSubmissionTimestamp = testDateTime,
    identityData = AuthTestData.testNonRepudiationIdentityData,
    userAuthToken = testAuthToken,
    headerData = headerData,
    searchKeys = Map("formBundleId" -> formBundleId)
  )

  val expectedNrsRequestJson: JsObject = Json.obj(
    "payload" -> testEncodedPayload(testNrsSubmissionPayload),
    "metadata" -> testNonRepudiationMetadata
  )

  lazy val testPartner: Entity = Entity(
    details = Some(testSoleTraderEntity.copy(businessVerification = None)),
    partyType = Individual,
    isLeadPartner = Some(true),
    address = None,
    email = None,
    telephoneNumber = None
  )

  lazy val testUkCompanyPartner: Entity = testPartner.copy(
    details = Some(testLtdCoEntity.copy(businessVerification = None, registration = FailedStatus)),
    partyType = UkCompany
  )

  lazy val testScottishPartnershipPartner: Entity = testPartner.copy(
    details = Some(testGeneralPartnershipEntity.copy(bpSafeId = None, businessVerification = None, registration = FailedStatus)),
    partyType = ScotPartnership,
    optScottishPartnershipName = Some(testCompanyName)
  )

  lazy val generalPartnershipWithSoleTraderPartner: VatScheme = testFullVatScheme.copy(
    eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = Partnership)),
    entities = Some(List(testPartner)),
    applicantDetails = Some(testUnregisteredApplicantDetails.copy(entity = Some(testGeneralPartnershipEntity.copy(
      bpSafeId = None,
      businessVerification = Some(BvFail),
      registration = NotCalledStatus
    )))),
    attachments = Some(Attachments(Some(Post))),
    business = Some(testBusiness.copy(shortOrgName = Some(testShortOrgName)))
  )

  lazy val generalPartnershipWithUkCompanyPartner: VatScheme = testFullVatScheme.copy(
    eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = Partnership)),
    entities = Some(List(testUkCompanyPartner)),
    applicantDetails = Some(testUnregisteredApplicantDetails.copy(entity = Some(testGeneralPartnershipEntity.copy(
      bpSafeId = None,
      businessVerification = Some(BvFail),
      registration = NotCalledStatus
    )))),
    attachments = Some(Attachments(Some(Post))),
    business = Some(testBusiness.copy(shortOrgName = Some(testShortOrgName)))
  )

  lazy val limitedPartnershipWithScotPartnershipPartner: VatScheme = testFullVatScheme.copy(
    eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = LtdPartnership)),
    entities = Some(List(testScottishPartnershipPartner)),
    applicantDetails = Some(testUnregisteredApplicantDetails.copy(entity = Some(testGeneralPartnershipEntity.copy(
      companyNumber = Some(testCrn),
      dateOfIncorporation = Some(testDateOfIncorp),
      bpSafeId = None,
      businessVerification = Some(BvFail),
      registration = NotCalledStatus
    )))),
    attachments = Some(Attachments(Some(Post))),
    business = Some(testBusiness.copy(shortOrgName = Some(testShortOrgName)))
  )

  lazy val limitedLiabilityPartnership: VatScheme = limitedPartnershipWithScotPartnershipPartner.copy(
    eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = LtdLiabilityPartnership)),
    entities = None,
    attachments = None
  )

  lazy val vatGroupVatScheme: VatScheme = testFullVatScheme.copy(
    eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(registrationReason = GroupRegistration)),
    applicantDetails = Some(testUnregisteredApplicantDetails.copy(entity = Some(testLtdCoEntity.copy(
      companyNumber = testCrn,
      dateOfIncorporation = Some(testDateOfIncorp),
      bpSafeId = None,
      businessVerification = Some(BvPass),
      registration = FailedStatus
    )))),
    attachments = Some(Attachments(Some(Post))),
    business = Some(testBusiness.copy(shortOrgName = Some(testShortOrgName)))
  )

  lazy val corporateBodyRegisteredJson = Json.obj(
    "corporateBodyRegistered" -> Json.obj(
      "companyRegistrationNumber" -> testCrn,
      "dateOfIncorporation" -> testDateOfIncorp,
      "countryOfIncorporation" -> "GB"
    )
  )

  override def afterEach(): Unit = {
    super.afterEach()
  }

  "PUT /:regID/submit-registration" when {
     disable(StubSubmission)
    "the user is a Sole Trader" should {
      "return OK if the submission is successful where the submission is a sole trader and UTR and NINO are provided" in new Setup {
        given.user.isAuthorised
        insertIntoDb(testSoleTraderVatScheme)

        stubPost("/vat/subscription", testVerifiedSoleTraderJsonWithUTR, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful with a bpSafeId" in new Setup {
        given.user.isAuthorised
        insertIntoDb(testMinimalVatSchemeWithVerifiedSoleTrader)

        stubPost("/vat/subscription", testVerifiedSoleTraderJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful without a bpSafeId" in new Setup {
        given.user.isAuthorised
        insertIntoDb(testSoleTraderVatScheme)

        stubPost("/vat/subscription", testVerifiedSoleTraderJsonWithUTR, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return appropriate response code when submitted application cannot be processed" in new Setup {
        given.user.isAuthorised
        insertIntoDb(
          testSoleTraderVatScheme.copy(applicantDetails = Some(
            testRegisteredSoleTraderApplicantDetailsNoBpSafeId.copy(personalDetails = Some(testPersonalDetails.copy(score = Some(testScore))))
          ))
        )

        stubPost("/vat/subscription", testVerifiedSoleTraderJsonWithUTR, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe UNPROCESSABLE_ENTITY
      }
    }

    "the user is a Limited Company" should {
      "return OK if the submission is successful with an unregistered business partner" in new Setup {
        given.user.isAuthorised
        insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner)

        stubPost("/vat/subscription", testSubmissionJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK for an Agent led journey" in new Setup {
        given.user.isAuthorised
        insertIntoDb(testAgentVatScheme)

        val agentTransactorJson = Json.obj(
          "declaration" -> Json.obj(
            "declarationSigning" -> Json.obj(
              "declarationCapacity" -> DeclarationCapacity.toJsString(AccountantAgent)
            ),
            "agentOrCapacitor" -> Json.obj(
              "individualName" -> Json.obj(
                "firstName" -> testFirstName,
                "lastName" -> testLastName
              ),
              "commDetails" -> Json.obj(
                "telephone" -> testTelephone,
                "email" -> testEmail
              ),
              "identification" -> Json.arr(
                Json.obj(
                  "idValue" -> testArn,
                  "idType" -> "ARN",
                  "IDsFailedOnlineVerification" -> toJsString(IdVerified)
                )
              )
            )
          )
        )

        stubPost("/vat/subscription", testSubmissionJson.as[JsObject].deepMerge(agentTransactorJson), OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful with an unregistered business partner and a short org name different from companyName" in new Setup {
        given.user.isAuthorised
        insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner.copy(
          business = Some(testBusiness.copy(shortOrgName = Some(testShortOrgName)))
        ))

        stubPost("/vat/subscription", testSubmissionJsonWithShortOrgName, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful with an unregistered business partner and transactor details" in new Setup {
        given.user.isAuthorised
        insertIntoDb(
          testFullVatSchemeWithUnregisteredBusinessPartner.copy(
            eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(isTransactor = true)),
            transactorDetails = Some(testTransactorDetails)
          )
        )

        stubPost("/vat/subscription", testTransactorSubmissionJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful where the business partner is already registered" in new Setup {
        given.user.isAuthorised
        insertIntoDb(testMinimalVatSchemeWithRegisteredBusinessPartner)

        stubPost("/vat/subscription", testRegisteredBusinessPartnerSubmissionJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful where the business partner is already registered when the frs data is completely missing" in new Setup {
        given.user.isAuthorised
        insertIntoDb(testMinimalVatSchemeWithRegisteredBusinessPartner.copy(flatRateScheme = None))

        stubPost("/vat/subscription", testRegisteredBusinessPartnerSubmissionJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a NETP" should {
      "return OK if the submission is successful without a bpSafeId" in new Setup {
        given.user.isAuthorised
        insertIntoDb(testNetpVatScheme)

        stubPost("/vat/subscription", testNetpJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful with overseas details" in new Setup {
        given.user.isAuthorised
        insertIntoDb(
          testNetpVatScheme.copy(applicantDetails = Some(testNetpApplicantDetails.copy(entity = Some(testNetpEntityOverseas))))
        )

        stubPost("/vat/subscription", testNetpJsonOverseas, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a Non UK Company" should {
      "return OK if the submission is successful without a bpSafeId" in new Setup {
        given.user.isAuthorised
        insertIntoDb(testNonUkCompanyVatScheme)

        stubPost("/vat/subscription", testNonUkCompanyJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a Trust" should {
      "return OK if the submission is successful with a bpSafeId" in new Setup {
        given.user.isAuthorised
        insertIntoDb(testMinimalVatSchemeWithTrust)

        stubPost("/vat/subscription", testVerifiedTrustJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a General Partnership" should {
      "return OK if the submission is successful where the submission contains a Sole Trader partner" in new Setup {
        given.user.isAuthorised
        insertIntoDb(generalPartnershipWithSoleTraderPartner)

        stubPost(
          "/vat/subscription",
          testSubmissionJson(generalPartnershipCustomerId, Some(soleTraderLeadPartner), attachmentList = List(VAT2)),
          OK,
          Json.stringify(testSubmissionResponse)
        )
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful where the submission contains a UK Company partner" in new Setup {
        given.user.isAuthorised
        insertIntoDb(generalPartnershipWithUkCompanyPartner)

        stubPost(
          "/vat/subscription",
          testSubmissionJson(generalPartnershipCustomerId, Some(ukCompanyLeadPartner), attachmentList = List(VAT2)),
          OK,
          Json.stringify(testSubmissionResponse)
        )
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a Limited Partnership" should {
      "return OK if the submission is successful where the submission contains a Scottish Partnership partner" in new Setup {
        given.user.isAuthorised
        insertIntoDb(limitedPartnershipWithScotPartnershipPartner)

        stubPost(
          "/vat/subscription",
          testSubmissionJson(limitedPartnershipCustomerId, Some(scottishPartnershipLeadPartner), attachmentList = List(VAT2)),
          OK,
          Json.stringify(testSubmissionResponse)
        )
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a Limited Liability Partnership" should {
      "return OK if the submission is successful" in new Setup {
        given.user.isAuthorised
        insertIntoDb(limitedLiabilityPartnership)

        stubPost("/vat/subscription", testSubmissionJson(limitedLiabilityPartnershipCustomerId, None), OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a UK Company registering a VAT Group" should {
      "return OK if the submission is successful" in new Setup {
        given.user.isAuthorised
        insertIntoDb(vatGroupVatScheme)

        stubPost(
          "/vat/subscription",
          testSubmissionJson(vatGroupCustomerId, Some(ukCompanyLeadEntity), GroupRegistration, Some(corporateBodyRegisteredJson), List(VAT51)),
          OK,
          Json.stringify(testSubmissionResponse)
        )
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is registering with a TOGC reg reason" should {
      "return OK if the submission is successful" in new Setup {

        lazy val togcBlock: TogcCole = TogcCole(
          dateOfTransfer = testDate,
          previousBusinessName = testPreviousBusinessName,
          vatRegistrationNumber = testVrn,
          wantToKeepVatNumber = true,
          agreedWithTermsForKeepingVat = Some(true)
        )
        lazy val togcVatScheme: VatScheme = testFullVatScheme.copy(
          eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(
            registrationReason = TransferOfAGoingConcern,
            togcCole = Some(togcBlock)
          )),
          applicantDetails = Some(testUnregisteredApplicantDetails.copy(entity = Some(testLtdCoEntity.copy(
            companyNumber = testCrn,
            dateOfIncorporation = Some(testDateOfIncorp),
            bpSafeId = None,
            businessVerification = Some(BvPass),
            registration = FailedStatus
          )))),
          business = Some(testBusiness.copy(shortOrgName = Some(testShortOrgName)))
        )

        given.user.isAuthorised
        insertIntoDb(togcVatScheme)

        stubPost(
          "/vat/subscription",
          testSubmissionJson(ukCompanyCustomerId, entities = None, TransferOfAGoingConcern, Some(togcBlockJson)),
          OK,
          Json.stringify(testSubmissionResponse)
        )
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is registering with a digital attachments" should {
      "return OK if the submission is successful and call SDES notify" in new Setup {
        given
          .user.isAuthorised
          .upscanDetailsRepo.insertIntoDb(testUpscanDetails(testReference), upscanMongoRepository.collection.insertOne)
          .upscanDetailsRepo.insertIntoDb(testUpscanDetails(testReference2), upscanMongoRepository.collection.insertOne)

        insertIntoDb(testMinimalVatSchemeWithVerifiedSoleTrader.copy(
          attachments = Some(Attachments(method = Some(Attached))),
          business = Some(testBusiness.copy(hasLandAndProperty = Some(true)))
        ))

        val attachmentNrsRequestJson: JsObject = Json.obj(
          "payload" -> testEncodedPayload(testNrsSubmissionPayload),
          "metadata" -> (Json.toJson(testNonRepudiationMetadata).as[JsObject] ++ Json.obj("attachmentIds" -> Seq(testReference, testReference2)))
        )
        val testSubmissionJson: JsObject = testVerifiedSoleTraderJson.deepMerge(Json.obj("admin" -> Json.obj("attachments" -> Json.obj(
          "VAT5L" -> Json.toJson[Option[AttachmentMethod]](Some(Attached))
        ))))

        stubPost("/vat/subscription", testSubmissionJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(attachmentNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))
        stubSdesNotification(Json.toJson(testSdesPayload(testReference)))(NO_CONTENT)
        stubSdesNotification(Json.toJson(testSdesPayload(testReference2)))(NO_CONTENT)

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK

        eventually(timeout(Span(1, Seconds)), interval(Span(50, Millis))) {
          val requestsHappened = try {
            verify(2, postRequestedFor(urlMatching("/notification/fileready")))
            true
          } catch {
            case _: Exception => false
          }
          requestsHappened mustBe true
        }
      }
    }

    "return INTERNAL_SERVER_ERROR if the VAT scheme is missing data" in new Setup {
      given.user.isAuthorised
      insertIntoDb(testEmptyVatScheme(testRegId))

      val res = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe INTERNAL_SERVER_ERROR
    }

    "return INTERNAL_SERVER_ERROR if the subscription API is unavailable" in new Setup {
      given.user.isAuthorised
      insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner)
      stubPost("/vat/subscription", testSubmissionJson, BAD_GATEWAY, Json.stringify(testSubmissionResponse))

      val res = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe INTERNAL_SERVER_ERROR
    }

    "return BAD_REQUEST if the subscription API returns BAD_REQUEST" in new Setup {
      given.user.isAuthorised
      insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner)
      stubPost("/vat/subscription", testSubmissionJson, BAD_REQUEST, Json.stringify(testSubmissionResponse))

      val res = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe BAD_REQUEST
    }

    "return CONFLICT if the subscription API returns CONFLICT" in new Setup {
      given.user.isAuthorised
      insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner)
      stubPost("/vat/subscription", testSubmissionJson, CONFLICT, Json.stringify(testSubmissionResponse))

      val res = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe CONFLICT
    }
  }

}
