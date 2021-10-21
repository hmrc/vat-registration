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
import models.IncorporatedIdEntity
import models.api.{ApplicantDetails, BvFail, NotCalledStatus, TransactorDetails}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.RegistrationMongoRepository

import scala.concurrent.Future
import models.submission._

class ApplicantDetailsControllerSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val controller: ApplicantDetailsController = new ApplicantDetailsController(
      mockApplicantDetailsService,
      mockAuthConnector,
      mockVatRegistrationService,
      stubControllerComponents()
    ) {
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
    }
  }

  val upsertApplicantDetails: ApplicantDetails = ApplicantDetails(
    roleInBusiness = testRole,
    transactor = TransactorDetails(
      name = testName,
      nino = Some(testNino),
      trn = None,
      identifiersMatch = true,
      dateOfBirth = testDate
    ),
    entity = IncorporatedIdEntity(
      companyName = testCompanyName,
      companyNumber = testCrn,
      ctutr = Some(testUtr),
      dateOfIncorporation = Some(testDateOFIncorp),
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

  val upsertApplicantDetailsJson: JsValue = Json.toJson(upsertApplicantDetails)(ApplicantDetails.writes)
  val validApplicantDetailsJson: JsValue = Json.toJson(validApplicantDetails)(ApplicantDetails.writes)

  "getApplicantDetailsData" should {
    "returns a valid json if found for id" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      when(mockApplicantDetailsService.getApplicantDetailsData(any(), any())).thenReturn(Future.successful(Some(validApplicantDetails)))
      when(mockVatRegistrationService.getPartyType(any())).thenReturn(Future.successful(Some(UkCompany)))

      val result: Future[Result] = controller.getApplicantDetailsData(testRegId)(FakeRequest())

      status(result) mustBe OK
      contentAsJson(result) mustBe validApplicantDetailsJson
    }

    "returns NO_CONTENT if none found" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      when(mockApplicantDetailsService.getApplicantDetailsData(any(), any())).thenReturn(Future.successful(None))
      when(mockVatRegistrationService.getPartyType(any())).thenReturn(Future.successful(Some(UkCompany)))

      val result: Future[Result] = controller.getApplicantDetailsData(testRegId)(FakeRequest())

      status(result) mustBe NO_CONTENT
    }

    "returns NOT_FOUND if none found" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      when(mockApplicantDetailsService.getApplicantDetailsData(any(), any())).thenReturn(Future.failed(MissingRegDocument(testRegId)))
      when(mockVatRegistrationService.getPartyType(any())).thenReturn(Future.successful(Some(UkCompany)))

      val result: Future[Result] = controller.getApplicantDetailsData(testRegId)(FakeRequest())
      status(result) mustBe NOT_FOUND
    }

    "returns FORBIDDEN if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(testRegId, testInternalId)

      val result: Future[Result] = controller.getApplicantDetailsData(testRegId)(FakeRequest())
      status(result) mustBe FORBIDDEN
    }
  }

  "updateApplicantDetailsData" should {
    "returns FORBIDDEN if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(testRegId, testInternalId)

      val result: Future[Result] = controller.updateApplicantDetailsData(testRegId)(FakeRequest().withBody(upsertApplicantDetailsJson))
      status(result) mustBe FORBIDDEN
    }

    "returns OK if successful" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      when(mockApplicantDetailsService.updateApplicantDetailsData(any(), any())).thenReturn(Future.successful(upsertApplicantDetails))
      when(mockVatRegistrationService.getPartyType(any())).thenReturn(Future.successful(Some(UkCompany)))

      val result: Future[Result] = controller.updateApplicantDetailsData(testRegId)(FakeRequest().withBody(upsertApplicantDetailsJson))
      status(result) mustBe OK
      contentAsJson(result) mustBe upsertApplicantDetailsJson
    }

    "returns NOT_FOUND if the registration is not found" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      when(mockApplicantDetailsService.updateApplicantDetailsData(any(), any())).thenReturn(Future.failed(MissingRegDocument(testRegId)))
      when(mockVatRegistrationService.getPartyType(any())).thenReturn(Future.successful(Some(UkCompany)))

      val result: Future[Result] = controller.updateApplicantDetailsData(testRegId)(FakeRequest().withBody(upsertApplicantDetailsJson))
      status(result) mustBe NOT_FOUND
    }

    "returns INTERNAL_SERVER_ERROR if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      when(mockApplicantDetailsService.updateApplicantDetailsData(any(), any())).thenReturn(Future.failed(new Exception))
      when(mockVatRegistrationService.getPartyType(any())).thenReturn(Future.successful(Some(UkCompany)))

      val result: Future[Result] = controller.updateApplicantDetailsData(testRegId)(FakeRequest().withBody(upsertApplicantDetailsJson))
      status(result) mustBe INTERNAL_SERVER_ERROR
    }
  }
}