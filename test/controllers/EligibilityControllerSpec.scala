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

package controllers

import common.RegistrationId
import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.Eligibility
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest

import scala.concurrent.Future

class EligibilityControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import fakeApplication.materializer

  class Setup {
    val controller = new EligibilityControllerImpl (
      eligibilityService = mockEligibilityService,
      auth = mockAuthConnector
    )
    def userIsAuthorised(): Unit = AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
    def userIsNotAuthorised(): Unit = AuthorisationMocks.mockNotLoggedInOrAuthorised()

    def getsEligibilityData(): OngoingStubbing[Future[Option[Eligibility]]] = when(mockEligibilityService.getEligibility(any())(any()))
      .thenReturn(Future.successful(Some(validEligibility)))

    def getsNoEligibilityData(): OngoingStubbing[Future[Option[Eligibility]]] = when(mockEligibilityService.getEligibility(any())(any()))
      .thenReturn(Future.successful(None))

    def upsertsEligibility(): OngoingStubbing[Future[Eligibility]] = when(mockEligibilityService.upsertEligibility(any(), any())(any()))
      .thenReturn(Future.successful(upsertEligibility))

    def upsertEligibilityFails(): OngoingStubbing[Future[Eligibility]] = when(mockEligibilityService.upsertEligibility(any(), any())(any()))
      .thenReturn(Future.failed(new Exception))

    def upsertEligibilityNotFound(): OngoingStubbing[Future[Eligibility]] = when(mockEligibilityService.upsertEligibility(any(), any())(any()))
      .thenReturn(Future.failed(new MissingRegDocument(RegistrationId("testId"))))
  }

  val validEligibilityJson = Json.parse(
    """
      |{
      | "version": 1,
      | "result": "thisIsAValidReason"
      |}
    """.stripMargin).as[JsObject]

  val upsertEligibilityJson = Json.parse(
    """
      |{
      | "version": 1,
      | "result": "thisIsAnUpsert"
      |}
    """.stripMargin).as[JsObject]

  val invalidUpsertJson = Json.parse(
    """
      |{
      | "result": "thisIsAnUpsert"
      |}
    """.stripMargin).as[JsObject]

  "getEligibility" should {
    "returns a valid json if found for id" in new Setup {
      userIsAuthorised()
      getsEligibilityData()

      val result = controller.getEligibility("testId")(FakeRequest())

      status(result) shouldBe 200
      jsonBodyOf(await(result)) shouldBe validEligibilityJson
    }

    "returns 404 if none found" in new Setup {
      userIsAuthorised()
      getsNoEligibilityData()

      val result = controller.getEligibility("testId")(FakeRequest())

      status(result) shouldBe 404
    }

    "returns 403 if user is not authorised" in new Setup {
      userIsNotAuthorised()

      val result = controller.getEligibility("testId")(FakeRequest())

      status(result) shouldBe 403
    }
  }

  "Upsert Eligibility" should {

    "returns 403 if user is not authorised" in new Setup {
      userIsNotAuthorised()
      val result = controller.updateEligibility("testId")(FakeRequest().withBody[JsObject](upsertEligibilityJson))
      status(result) shouldBe 403
    }

    "returns 200 if successful" in new Setup {
      userIsAuthorised()
      upsertsEligibility()
      val result = controller.updateEligibility("testId")(FakeRequest().withBody[JsObject](upsertEligibilityJson))
      status(result) shouldBe 200
      jsonBodyOf(await(result)) shouldBe upsertEligibilityJson
    }

    "returns 400 if json received is invalid" in new Setup {
      userIsAuthorised()
      upsertEligibilityFails()
      val result = controller.updateEligibility("testId")(FakeRequest().withBody[JsObject](invalidUpsertJson))
      status(result) shouldBe 400
    }

    "returns 404 if the registration is not found" in new Setup {
      userIsAuthorised()
      upsertEligibilityNotFound()
      val result = controller.updateEligibility("testId")(FakeRequest().withBody[JsObject](upsertEligibilityJson))
      status(result) shouldBe 404
    }

    "returns 500 if an error occurs" in new Setup {
      userIsAuthorised()
      upsertEligibilityFails()
      val result = controller.updateEligibility("testId")(FakeRequest().withBody[JsObject](upsertEligibilityJson))
      status(result) shouldBe 500
    }
  }

}
