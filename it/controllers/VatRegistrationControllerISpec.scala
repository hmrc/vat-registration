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

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlPathMatching, verify}
import connectors.stubs.NonRepudiationStub.stubNonRepudiationSubmission
import connectors.stubs.SdesNotifyStub.stubSdesNotification
import enums.VatRegStatus
import featureswitch.core.config.{FeatureSwitching, ShortOrgName, StubSubmission}
import itutil.{FakeTimeMachine, ITVatSubmissionFixture, IntegrationStubbing}
import models.api._
import models.nonrepudiation.NonRepudiationMetadata
import models.registration.sections.PartnersSection
import models.submission.IdVerificationStatus.toJsString
import models.submission.{DeclarationCapacity, _}
import models.{GroupRegistration, TogcCole, TransferOfAGoingConcern}
import org.scalatest.concurrent.Eventually.eventually
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global

class VatRegistrationControllerISpec extends IntegrationStubbing with FeatureSwitching with ITVatSubmissionFixture {

  val testNonRepudiationApiKey = "testNonRepudiationApiKey"
  override lazy val additionalConfig = Map("microservice.services.non-repudiation.api-key" -> testNonRepudiationApiKey)

  class Setup extends SetupHelper

  def testEncodedPayload(payload: String): String = Base64.getEncoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8))

  def testPayloadChecksum(payload: String): String = MessageDigest.getInstance("SHA-256")
    .digest(payload.getBytes(StandardCharsets.UTF_8))
    .map("%02x".format(_)).mkString

  FakeTimeMachine.hour = 0

  val formBundleId = "123412341234"
  val testSubmissionResponse = Json.obj("formBundle" -> formBundleId)

  val testAuthToken = "testAuthToken"
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

  lazy val testPartner: Partner = Partner(
    details = testSoleTraderEntity.copy(businessVerification = None),
    partyType = Individual,
    isLeadPartner = true
  )

  lazy val testUkCompanyPartner: Partner = testPartner.copy(
    details = testLtdCoEntity.copy(businessVerification = None, registration = FailedStatus),
    partyType = UkCompany
  )

  lazy val testScottishPartnershipPartner: Partner = testPartner.copy(
    details = testGeneralPartnershipEntity.copy(bpSafeId = None, businessVerification = None, registration = FailedStatus),
    partyType = ScotPartnership
  )

  lazy val generalPartnershipWithSoleTraderPartner: VatScheme = testFullVatScheme.copy(
    eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = Partnership)),
    partners = Some(PartnersSection(List(testPartner))),
    applicantDetails = Some(testUnregisteredApplicantDetails.copy(entity = testGeneralPartnershipEntity.copy(
      bpSafeId = None,
      businessVerification = Some(BvFail),
      registration = NotCalledStatus
    ))),
    tradingDetails = Some(testTradingDetails.copy(shortOrgName = Some(testShortOrgName)))
  )

  lazy val generalPartnershipWithUkCompanyPartner: VatScheme = testFullVatScheme.copy(
    eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = Partnership)),
    partners = Some(PartnersSection(List(testUkCompanyPartner))),
    applicantDetails = Some(testUnregisteredApplicantDetails.copy(entity = testGeneralPartnershipEntity.copy(
      bpSafeId = None,
      businessVerification = Some(BvFail),
      registration = NotCalledStatus
    ))),
    tradingDetails = Some(testTradingDetails.copy(shortOrgName = Some(testShortOrgName)))
  )

  lazy val limitedPartnershipWithScotPartnershipPartner: VatScheme = testFullVatScheme.copy(
    eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = LtdPartnership)),
    partners = Some(PartnersSection(List(testScottishPartnershipPartner))),
    applicantDetails = Some(testUnregisteredApplicantDetails.copy(entity = testGeneralPartnershipEntity.copy(
      companyNumber = Some(testCrn),
      dateOfIncorporation = Some(testDateOfIncorp),
      bpSafeId = None,
      businessVerification = Some(BvFail),
      registration = NotCalledStatus
    ))),
    tradingDetails = Some(testTradingDetails.copy(shortOrgName = Some(testShortOrgName)))
  )

  lazy val limitedLiabilityPartnership: VatScheme = limitedPartnershipWithScotPartnershipPartner.copy(
    eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = LtdLiabilityPartnership)),
    partners = None
  )

  lazy val vatGroupVatscheme: VatScheme = testFullVatScheme.copy(
    eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(registrationReason = GroupRegistration)),
    applicantDetails = Some(testUnregisteredApplicantDetails.copy(entity = testLtdCoEntity.copy(
      companyNumber = testCrn,
      dateOfIncorporation = Some(testDateOfIncorp),
      bpSafeId = None,
      businessVerification = Some(BvPass),
      registration = FailedStatus
    ))),
    tradingDetails = Some(testTradingDetails.copy(shortOrgName = Some(testShortOrgName)))
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
    disable(ShortOrgName)
  }

  "POST /new" should {
    "return CREATED if the daily quota has not been met" in new Setup {
      given
        .user.isAuthorised

      val res = await(client(controllers.routes.VatRegistrationController.newVatRegistration.url)
        .post(Json.obj())
      )

      res.status mustBe CREATED
    }
  }

  "POST /insert-s4l-scheme" should {
    "return CREATED if the vatScheme is stored successfully" in new Setup {
      given
        .user.isAuthorised

      val testVatSchemeJson: JsValue = Json.toJson(testFullVatScheme)(VatScheme.format())
      val res: WSResponse = await(client(controllers.routes.VatRegistrationController.insertVatScheme.url)
        .post(testVatSchemeJson)
      )

      res.status mustBe CREATED
      res.body mustBe testVatSchemeJson.toString
    }
  }

  "PUT /:regID/submit-registration" when {
    disable(StubSubmission)

    "the user is a Sole Trader" should {
      "return OK if the submission is successful where the submission is a sole trader and UTR and NINO are provided" in new Setup {
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(testSoleTraderVatScheme, repo.insert)

        stubPost("/vat/subscription", testVerifiedSoleTraderJsonWithUTR, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful with a bpSafeId" in new Setup {
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(testMinimalVatSchemeWithVerifiedSoleTrader, repo.insert)

        stubPost("/vat/subscription", testVerifiedSoleTraderJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful without a bpSafeId" in new Setup {
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(testSoleTraderVatScheme, repo.insert)

        stubPost("/vat/subscription", testVerifiedSoleTraderJsonWithUTR, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a Limited Company" should {
      "return OK if the submission is successful with an unregistered business partner" in new Setup {
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner, repo.insert)

        stubPost("/vat/subscription", testSubmissionJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK for an Agent led journey" in new Setup {
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(testAgentVatScheme, repo.insert)

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
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful with an unregistered business partner and a short org name different from companyName" in new Setup {
        enable(ShortOrgName)

        given
          .user.isAuthorised
          .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner.copy(
          tradingDetails = Some(testTradingDetails.copy(shortOrgName = Some(testShortOrgName)))
        ), repo.insert)

        stubPost("/vat/subscription", testSubmissionJsonWithShortOrgName, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful with an unregistered business partner and transactor details" in new Setup {
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(
          testFullVatSchemeWithUnregisteredBusinessPartner.copy(
            eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(isTransactor = true)),
            transactorDetails = Some(testTransactorDetails)
          ),
          repo.insert
        )

        stubPost("/vat/subscription", testTransactorSubmissionJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful where the business partner is already registered" in new Setup {
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(testMinimalVatSchemeWithRegisteredBusinessPartner, repo.insert)

        stubPost("/vat/subscription", testRegisteredBusinessPartnerSubmissionJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful where the business partner is already registered when the frs data is completely missing" in new Setup {
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(testMinimalVatSchemeWithRegisteredBusinessPartner.copy(flatRateScheme = None), repo.insert)

        stubPost("/vat/subscription", testRegisteredBusinessPartnerSubmissionJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a NETP" should {
      "return OK if the submission is successful without a bpSafeId" in new Setup {
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(testNetpVatScheme, repo.insert)

        stubPost("/vat/subscription", testNetpJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful with overseas details" in new Setup {
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(
          testNetpVatScheme.copy(applicantDetails = Some(testNetpApplicantDetails.copy(entity = testNetpEntityOverseas))),
          repo.insert
        )

        stubPost("/vat/subscription", testNetpJsonOverseas, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a Non UK Company" should {
      "return OK if the submission is successful without a bpSafeId" in new Setup {
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(testNonUkCompanyVatScheme, repo.insert)

        stubPost("/vat/subscription", testNonUkCompanyJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a Trust" should {
      "return OK if the submission is successful with a bpSafeId" in new Setup {
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(testMinimalVatSchemeWithTrust, repo.insert)

        stubPost("/vat/subscription", testVerifiedTrustJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a General Partnership" should {
      "return OK if the submission is successful where the submission contains a Sole Trader partner" in new Setup {
        enable(ShortOrgName)
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(generalPartnershipWithSoleTraderPartner, repo.insert)

        stubPost("/vat/subscription", testSubmissionJson(generalPartnershipCustomerId, Some(soleTraderLeadPartner)), OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }

      "return OK if the submission is successful where the submission contains a UK Company partner" in new Setup {
        enable(ShortOrgName)
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(generalPartnershipWithUkCompanyPartner, repo.insert)

        stubPost("/vat/subscription", testSubmissionJson(generalPartnershipCustomerId, Some(ukCompanyLeadPartner)), OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a Limited Partnership" should {
      "return OK if the submission is successful where the submission contains a Scottish Partnership partner" in new Setup {
        enable(ShortOrgName)
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(limitedPartnershipWithScotPartnershipPartner, repo.insert)

        stubPost("/vat/subscription", testSubmissionJson(limitedPartnershipCustomerId, Some(scottishPartnershipLeadPartner)), OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a Limited Liability Partnership" should {
      "return OK if the submission is successful" in new Setup {
        enable(ShortOrgName)
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(limitedLiabilityPartnership, repo.insert)

        stubPost("/vat/subscription", testSubmissionJson(limitedLiabilityPartnershipCustomerId, None), OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is a UK Company registering a VAT Group" should {
      "return OK if the submission is successful" in new Setup {
        enable(ShortOrgName)
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(vatGroupVatscheme, repo.insert)

        stubPost(
          "/vat/subscription",
          testSubmissionJson(vatGroupCustomerId, Some(ukCompanyLeadEntity), GroupRegistration, Some(corporateBodyRegisteredJson)),
          OK,
          Json.stringify(testSubmissionResponse)
        )
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is registering with a TOGC reg reason" should {
      "return OK if the submission is successful" in new Setup {
        enable(ShortOrgName)

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
          applicantDetails = Some(testUnregisteredApplicantDetails.copy(entity = testLtdCoEntity.copy(
            companyNumber = testCrn,
            dateOfIncorporation = Some(testDateOfIncorp),
            bpSafeId = None,
            businessVerification = Some(BvPass),
            registration = FailedStatus
          ))),
          tradingDetails = Some(testTradingDetails.copy(shortOrgName = Some(testShortOrgName)))
        )

        given
          .user.isAuthorised
          .regRepo.insertIntoDb(togcVatScheme, repo.insert)

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
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK
      }
    }

    "the user is registering with a digital attachments" should {
      "return OK if the submission is successful and call SDES notify" in new Setup {
        given
          .user.isAuthorised
          .regRepo.insertIntoDb(testMinimalVatSchemeWithVerifiedSoleTrader.copy(
          attachments = Some(Attachments(method = Attached)),
          sicAndCompliance = Some(testSicAndCompliance.copy(hasLandAndProperty = Some(true)))
        ), repo.insert)
          .upscanDetailsRepo.insertIntoDb(testUpscanDetails(testReference), upscanMongoRepository.insert)
          .upscanDetailsRepo.insertIntoDb(testUpscanDetails(testReference2), upscanMongoRepository.insert)

        val attachmentNrsRequestJson: JsObject = Json.obj(
          "payload" -> testEncodedPayload(testNrsSubmissionPayload),
          "metadata" -> (Json.toJson(testNonRepudiationMetadata).as[JsObject] ++ Json.obj("attachmentIds" -> Seq(testReference, testReference2)))
        )
        val testSubmissionJson: JsObject = testVerifiedSoleTraderJson.deepMerge(Json.obj("admin" -> Json.obj("attachments" -> Json.obj(
          "VAT5L" -> Json.toJson[AttachmentMethod](Attached)
        ))))

        stubPost("/vat/subscription", testSubmissionJson, OK, Json.stringify(testSubmissionResponse))
        stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
        stubPost("/hmrc/email", ACCEPTED, "")
        stubNonRepudiationSubmission(attachmentNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))
        stubSdesNotification(Json.toJson(testSdesPayload(testReference)))(NO_CONTENT)
        stubSdesNotification(Json.toJson(testSdesPayload(testReference2)))(NO_CONTENT)

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
          .withHttpHeaders("authorization" -> testAuthToken)
          .put(Json.obj("userHeaders" -> headerData))
        )

        res.status mustBe OK

        eventually {
          val requestsHappened = try {
            verify(2, postRequestedFor(urlPathMatching(s"/notification/fileready")))
            true
          } catch {
            case e: Exception => false
          }
          requestsHappened mustBe true
        }
      }
    }

    "return INTERNAL_SERVER_ERROR if the VAT scheme is missing data" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(VatScheme(testRegId, testInternalid, status = VatRegStatus.draft), repo.insert)

      val res = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe INTERNAL_SERVER_ERROR
    }

    "return INTERNAL_SERVER_ERROR if the subscription API is unavailable" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner, repo.insert)

      stubPost("/vat/subscription", testSubmissionJson, BAD_GATEWAY, Json.stringify(testSubmissionResponse))

      val res = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe INTERNAL_SERVER_ERROR
    }

    "return BAD_REQUEST if the subscription API returns BAD_REQUEST" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner, repo.insert)

      stubPost("/vat/subscription", testSubmissionJson, BAD_REQUEST, Json.stringify(testSubmissionResponse))

      val res = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe BAD_REQUEST
    }

    "return CONFLICT if the subscription API returns CONFLICT" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner, repo.insert)

      stubPost("/vat/subscription", testSubmissionJson, CONFLICT, Json.stringify(testSubmissionResponse))

      val res = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe CONFLICT
    }
  }

  "PATCH  /:regId/honesty-declaration" should {
    "return Ok if the honesty declaration is successfully stored" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val res = await(client(controllers.routes.VatRegistrationController.storeHonestyDeclaration(testRegId).url)
        .patch(Json.obj("honestyDeclaration" -> true))
      )

      res.status mustBe OK
      await(repo.findAll()).head.confirmInformationDeclaration mustBe Some(true)
    }
  }

}
