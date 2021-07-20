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

import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.IncorporatedEntity
import models.api.{ApplicantDetails, BvFail, BvPass, NotCalledStatus, RegisteredStatus, TransactorDetails}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.RegistrationMongoRepository

import scala.concurrent.Future

class ApplicantDetailsControllerSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val controller: ApplicantDetailsController = new ApplicantDetailsController(mockApplicantDetailsService, mockAuthConnector, stubControllerComponents()) {
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
    }
  }

  val upsertApplicantDetails: ApplicantDetails = ApplicantDetails(
    roleInBusiness = testRole,
    transactor = TransactorDetails(
      name = testName,
      nino = testNino,
      dateOfBirth = testDate
    ),
    entity = IncorporatedEntity(
      companyName = testCompanyName,
      companyNumber = testCrn,
      ctutr = testUtr,
      dateOfIncorporation = testDateOFIncorp,
      businessVerification = BvFail,
      registration = NotCalledStatus,
      identifiersMatch = true,
      bpSafeId = Some(testBpSafeId),
      chrn = None
    ),
    currentAddress = testAddress,
    contact = testDigitalContactOptional,
    changeOfName = None,
    previousAddress = None
  )

  val upsertApplicantDetailsJson: JsValue = Json.toJson(upsertApplicantDetails)
  val validApplicantDetailsJson: JsValue = Json.toJson(validApplicantDetails)

  "getApplicantDetailsData" should {
    "returns a valid json if found for id" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockApplicantDetailsService.getApplicantDetailsData(any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))

      val result: Future[Result] = controller.getApplicantDetailsData(testRegId)(FakeRequest())

      status(result) mustBe 200
      contentAsJson(result) mustBe validApplicantDetailsJson
    }

    "returns 204 if none found" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockApplicantDetailsService.getApplicantDetailsData(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getApplicantDetailsData(testRegId)(FakeRequest())

      status(result) mustBe 204
    }

    "returns 404 if none found" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockApplicantDetailsService.getApplicantDetailsData(any()))
        .thenReturn(Future.failed(MissingRegDocument(testRegId)))

      val result: Future[Result] = controller.getApplicantDetailsData(testRegId)(FakeRequest())
      status(result) mustBe 404
    }

    "returns 403 if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(testRegId, testInternalid)

      val result: Future[Result] = controller.getApplicantDetailsData(testRegId)(FakeRequest())
      status(result) mustBe 403
    }
  }

  "updateApplicantDetailsData" should {
    "returns 403 if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(testRegId, testInternalid)

      val result: Future[Result] = controller.updateApplicantDetailsData(testRegId)(FakeRequest().withBody(upsertApplicantDetailsJson))
      status(result) mustBe 403
    }

    "returns 200 if successful" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockApplicantDetailsService.updateApplicantDetailsData(any(), any()))
        .thenReturn(Future.successful(upsertApplicantDetails))

      val result: Future[Result] = controller.updateApplicantDetailsData(testRegId)(FakeRequest().withBody(upsertApplicantDetailsJson))
      status(result) mustBe 200
      contentAsJson(result) mustBe upsertApplicantDetailsJson
    }

    "returns 404 if the registration is not found" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockApplicantDetailsService.updateApplicantDetailsData(any(), any()))
        .thenReturn(Future.failed(MissingRegDocument(testRegId)))

      val result: Future[Result] = controller.updateApplicantDetailsData(testRegId)(FakeRequest().withBody(upsertApplicantDetailsJson))
      status(result) mustBe 404
    }

    "returns 500 if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockApplicantDetailsService.updateApplicantDetailsData(any(), any()))
        .thenReturn(Future.failed(new Exception))

      val result: Future[Result] = controller.updateApplicantDetailsData(testRegId)(FakeRequest().withBody(upsertApplicantDetailsJson))
      status(result) mustBe 500
    }
  }
}