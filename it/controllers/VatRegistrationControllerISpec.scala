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

import com.github.tomakehurst.wiremock.client.WireMock.{postRequestedFor, urlEqualTo, verify}
import connectors.stubs.NonRepudiationStub.stubNonRepudiationSubmission
import enums.VatRegStatus
import featureswitch.core.config.{CheckYourAnswersNrsSubmission, FeatureSwitching, StubSubmission}
import itutil.{FakeTimeMachine, ITVatSubmissionFixture, IntegrationStubbing}
import models.api.VatScheme
import models.nonrepudiation.NonRepudiationMetadata
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

  override def afterEach(): Unit = {
    super.afterEach()
    disable(CheckYourAnswersNrsSubmission)
  }

  def testEncodedPayload(payload: String): String = Base64.getEncoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8))

  def testPayloadChecksum(payload: String): String = MessageDigest.getInstance("SHA-256")
    .digest(payload.getBytes(StandardCharsets.UTF_8))
    .map("%02x".format(_)).mkString

  FakeTimeMachine.hour = 0

  val testAuthToken = "testAuthToken"
  val headerData = Map("testHeaderKey" -> "testHeaderValue")
  val testNonRepudiationSubmissionId = "testNonRepudiationSubmissionId"

  val testNonRepudiationMetadata: NonRepudiationMetadata = NonRepudiationMetadata(
    businessId = "vrs",
    notableEvent = "vat-registration",
    payloadContentType = "application/json",
    payloadSha256Checksum = testPayloadChecksum(testNrsSubmissionPayload),
    userSubmissionTimestamp = testDateTime,
    identityData = AuthTestData.testNonRepudiationIdentityData,
    userAuthToken = testAuthToken,
    headerData = headerData,
    searchKeys = Map("postCode" -> testAddress.postcode.get)
  )

  val expectedNrsRequestJson: JsObject = Json.obj(
    "payload" -> testEncodedPayload(testNrsSubmissionPayload),
    "metadata" -> testNonRepudiationMetadata
  )

  "POST /new" should {
    "return CREATED if the daily quota has not been met" in new Setup {
      given
        .user.isAuthorised

      val res = await(client(controllers.routes.VatRegistrationController.newVatRegistration().url)
        .post(Json.obj())
      )

      res.status mustBe CREATED
    }
  }

  "POST /insert-s4l-scheme" should {
    "return CREATED if the vatScheme is stored successfully" in new Setup {
      given
        .user.isAuthorised

      val testVatSchemeJson: JsValue = Json.toJson(testFullVatScheme)
      val res: WSResponse = await(client(controllers.routes.VatRegistrationController.insertVatScheme().url)
        .post(testVatSchemeJson)
      )

      res.status mustBe CREATED
      res.body mustBe testVatSchemeJson.toString
    }
  }

  "PUT /:regID/submit-registration" should {
    "return OK if the submission is successful with an unregistered business partner" in new Setup {
      enable(StubSubmission)

      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner, repo.insert)

      stubPost("/vatreg/test-only/vat/subscription", testSubmissionJson, OK, "")

      val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe OK
    }

    "return OK if the submission is successful with an unregistered business partner when the nrs feature switch is on" in new Setup {
      enable(StubSubmission)
      enable(CheckYourAnswersNrsSubmission)

      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner, repo.insert)

      stubPost("/auth/authorise", OK, AuthTestData.identityJson.toString())
      stubPost("/vatreg/test-only/vat/subscription", testSubmissionJson, OK, "")
      stubNonRepudiationSubmission(expectedNrsRequestJson, testNonRepudiationApiKey)(ACCEPTED, Json.obj("nrSubmissionId" -> testNonRepudiationSubmissionId))

      val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .withHttpHeaders("authorization" -> testAuthToken)
        .put(Json.obj("userHeaders" -> headerData))
      )

      eventually {
        verify(postRequestedFor(urlEqualTo("/submission")))
      }
      res.status mustBe OK
    }

    "return OK if the submission is successful for a sole trader with a bpSafeId" in new Setup {
      enable(StubSubmission)

      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testMinimalVatSchemeWithVerifiedSoleTrader, repo.insert)

      stubPost("/vatreg/test-only/vat/subscription", testVerifiedSoleTraderJson, OK, "")

      val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe OK
    }

    "return OK if the submission is successful where the business partner is already registered" in new Setup {
      enable(StubSubmission)

      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testMinimalVatSchemeWithRegisteredBusinessPartner, repo.insert)

      stubPost("/vatreg/test-only/vat/subscription", testRegisteredBusinessPartnerSubmissionJson, OK, "")

      val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe OK
    }

    "return OK if the submission is successful where the business partner is already registered when the frs data is completely missing" in new Setup {
      enable(StubSubmission)

      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testMinimalVatSchemeWithRegisteredBusinessPartner.copy(flatRateScheme = None), repo.insert)

      stubPost("/vatreg/test-only/vat/subscription", testRegisteredBusinessPartnerSubmissionJson, OK, "")

      val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe OK
    }

    "return INTERNAL_SERVER_ERROR if the VAT scheme is missing data" in new Setup {
      enable(StubSubmission)

      given
        .user.isAuthorised
        .regRepo.insertIntoDb(VatScheme(testRegId, testInternalid, status = VatRegStatus.draft), repo.insert)
        .subscriptionApi.respondsWith(OK)

      val res = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe INTERNAL_SERVER_ERROR
    }

    "return INTERNAL_SERVER_ERROR if the subscription API is unavailable" in new Setup {
      enable(StubSubmission)

      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner, repo.insert)
        .subscriptionApi.respondsWith(BAD_GATEWAY)

      val res = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe INTERNAL_SERVER_ERROR
    }

    "return INTERNAL_SERVER_ERROR if the subscription API returns BAD_REQUEST" in new Setup {
      enable(StubSubmission)

      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner, repo.insert)
        .subscriptionApi.respondsWith(BAD_REQUEST)

      val res = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe INTERNAL_SERVER_ERROR
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
