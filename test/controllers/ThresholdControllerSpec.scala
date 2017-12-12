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

import java.time.LocalDate

import common.RegistrationId
import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.{Eligibility, Threshold}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest

import scala.concurrent.Future

class ThresholdControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import fakeApplication.materializer

  class Setup {
    val controller = new ThresholdControllerImpl (
      thresholdService = mockThresholdService,
      auth = mockAuthConnector
    )
    def userIsAuthorised(): Unit = AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
    def userIsNotAuthorised(): Unit = AuthorisationMocks.mockNotLoggedInOrAuthorised()

    def getsThresholdData(): OngoingStubbing[Future[Option[Threshold]]] = when(mockThresholdService.getThreshold(any())(any()))
      .thenReturn(Future.successful(Some(validThreshold)))

    def getsNoThresholdData(): OngoingStubbing[Future[Option[Threshold]]] = when(mockThresholdService.getThreshold(any())(any()))
      .thenReturn(Future.successful(None))

    def upsertsThreshold(): OngoingStubbing[Future[Threshold]] = when(mockThresholdService.upsertThreshold(any(), any())(any()))
      .thenReturn(Future.successful(upsertThreshold))

    def upsertThresholdFails(): OngoingStubbing[Future[Threshold]] = when(mockThresholdService.upsertThreshold(any(), any())(any()))
      .thenReturn(Future.failed(new Exception))

    def upsertThresholdNotFound(): OngoingStubbing[Future[Threshold]] = when(mockThresholdService.upsertThreshold(any(), any())(any()))
      .thenReturn(Future.failed(new MissingRegDocument(RegistrationId("testId"))))
  }

  val validThresholdJson = Json.parse(
    s"""
      |{
      | "mandatoryRegistration": false,
      | "voluntaryReason": "voluntaryReason",
      | "overThresholdDate": "${LocalDate.now()}",
      | "expectedOverThresholdDate": "${LocalDate.now()}"
      |}
    """.stripMargin).as[JsObject]

  val upsertTresholdJson = Json.parse(
    s"""
      |{
      | "mandatoryRegistration": true,
      | "overThresholdDate": "${LocalDate.now()}",
      | "expectedOverThresholdDate": "${LocalDate.now()}"
      |}
    """.stripMargin).as[JsObject]

  val invalidUpsertJson = Json.parse(
    """
      |{
      | "mandatoryRegistration": "true-2"
      |}
    """.stripMargin).as[JsObject]

  "getThreshold" should {
    "returns a valid json if found for id" in new Setup {
      userIsAuthorised()
      getsThresholdData()

      val result = controller.getThreshold("testId")(FakeRequest())

      status(result) shouldBe 200
      jsonBodyOf(await(result)) shouldBe validThresholdJson
    }

    "returns 404 if none found" in new Setup {
      userIsAuthorised()
      getsNoThresholdData()

      val result = controller.getThreshold("testId")(FakeRequest())

      status(result) shouldBe 404
    }

    "returns 403 if user is not authorised" in new Setup {
      userIsNotAuthorised()

      val result = controller.getThreshold("testId")(FakeRequest())

      status(result) shouldBe 403
    }
  }

  "Upsert Threshold" should {

    "returns 403 if user is not authorised" in new Setup {
      userIsNotAuthorised()
      val result = controller.updateThreshold("testId")(FakeRequest().withBody[JsObject](upsertTresholdJson))
      status(result) shouldBe 403
    }

    "returns 200 if successful" in new Setup {
      userIsAuthorised()
      upsertsThreshold()
      val result = controller.updateThreshold("testId")(FakeRequest().withBody[JsObject](upsertTresholdJson))
      status(result) shouldBe 200
      jsonBodyOf(await(result)) shouldBe upsertTresholdJson
    }

    "returns 400 if json received is invalid" in new Setup {
      userIsAuthorised()
      upsertThresholdFails()
      val result = controller.updateThreshold("testId")(FakeRequest().withBody[JsObject](invalidUpsertJson))
      status(result) shouldBe 400
    }

    "returns 404 if the registration is not found" in new Setup {
      userIsAuthorised()
      upsertThresholdNotFound()
      val result = controller.updateThreshold("testId")(FakeRequest().withBody[JsObject](upsertTresholdJson))
      status(result) shouldBe 404
    }

    "returns 500 if an error occurs" in new Setup {
      userIsAuthorised()
      upsertThresholdFails()
      val result = controller.updateThreshold("testId")(FakeRequest().withBody[JsObject](upsertTresholdJson))
      status(result) shouldBe 500
    }
  }
}
