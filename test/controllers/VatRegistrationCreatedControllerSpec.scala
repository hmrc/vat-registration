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
import mocks.MockNewRegistrationService
import models.api._
import models.submission.Individual
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.LocalDate
import scala.concurrent.Future

class VatRegistrationCreatedControllerSpec extends VatRegSpec with VatRegistrationFixture with MockNewRegistrationService {

  import play.api.test.Helpers._

  class Setup {
    val controller: VatRegistrationController = new VatRegistrationController(
      mockVatRegistrationService,
      mockSubmissionService,
      mockRegistrationMongoRepository,
      mockAuthConnector,
      mockNewRegistrationService,
      stubControllerComponents()) {

      override val registrationRepository: RegistrationMongoRepository = mockRegistrationMongoRepository
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
    }
  }

  val registrationId = "reg-12345"

  "GET /" should {
    "call to retrieveVatScheme return Ok with VatScheme" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      ServiceMocks.mockRetrieveVatScheme(testRegId, testVatScheme)

      controller.retrieveVatScheme(testRegId)(FakeRequest()) returnsStatus OK
    }

    "call to retrieveVatScheme return ServiceUnavailable" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      ServiceMocks.mockRetrieveVatSchemeThrowsException(testRegId)

      controller.retrieveVatScheme(testRegId)(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
    }

    "newVatRegistration" should {
      "return CREATED if a new VAT scheme is successfully created" in new Setup {
        AuthorisationMocks.mockAuthenticated(testInternalid)

        mockNewRegistration(testInternalid)(Future.successful(testVatScheme))

        controller.newVatRegistration()(FakeRequest()) returnsStatus CREATED
      }

      "return FORBIDDEN if user not authenticated for newVatRegistration" in new Setup {
        AuthorisationMocks.mockAuthenticatedLoggedInNoCorrespondingData()

        controller.newVatRegistration(FakeRequest()) returnsStatus FORBIDDEN
      }

      "return INTERNAL_SERVER_ERROR if RegistrationService encounters any problems" in new Setup {
        AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
        mockNewRegistration(testInternalid)(Future.failed(new Exception("")))

        controller.newVatRegistration()(FakeRequest()) returnsStatus INTERNAL_SERVER_ERROR
      }
    }

    "updateBankAccountDetails" should {

      val bankAccount = testBankAccount

      "return a 200 if the update to mongo was successful" in new Setup {
        AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
        when(mockRegistrationMongoRepository.updateBankAccount(any(), any()))
          .thenReturn(Future.successful(bankAccount))

        val request: FakeRequest[JsObject] = FakeRequest().withBody(
          Json.obj(
            "isProvided" -> true,
            "details" -> Json.obj(
              "name" -> "testAccountName",
              "sortCode" -> "010203",
              "number" -> "01023456"
            )
          )
        )

        val result: Future[Result] = controller.updateBankAccountDetails(testRegId)(request)
        status(result) mustBe OK
      }
    }

    "fetchBankAccountDetails" should {
      val bankAccount = testBankAccount

      "return a 200 if the fetch from mongo was successful" in new Setup {
        AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
        when(mockRegistrationMongoRepository.fetchBankAccount(any()))
          .thenReturn(Future.successful(Some(bankAccount)))

        val expected: JsObject = Json.obj(
          "isProvided" -> true,
          "details" -> Json.obj(
            "name" -> "Test Bank Account",
            "sortCode" -> "010203",
            "number" -> "01023456"
          )
        )

        val result: Future[Result] = controller.fetchBankAccountDetails(testRegId)(FakeRequest())
        status(result) mustBe OK
        contentAsJson(result) mustBe expected
      }

      "return a 404 if the fetch from mongo returned nothing" in new Setup {
        AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
        when(mockRegistrationMongoRepository.fetchBankAccount(any()))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = controller.fetchBankAccountDetails(testRegId)(FakeRequest())
        status(result) mustBe NOT_FOUND

      }
    }

    "getAcknowledgementReference" should {

      "call getAcknowledgementReference return Ok with Acknowledgement Reference" in new Setup {
        AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
        ServiceMocks.mockGetAcknowledgementReference(testAckReference)

        controller.getAcknowledgementReference(testRegId)(FakeRequest()) returnsStatus OK
      }

      "call getAcknowledgementReference return ServiceUnavailable" in new Setup {
        AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
        ServiceMocks.mockGetAcknowledgementReferenceServiceUnavailable(exception)

        controller.getAcknowledgementReference(testRegId)(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
      }

      "call getAcknowledgementReference return AcknowledgementReferenceExists Error" in new Setup {
        AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
        ServiceMocks.mockGetAcknowledgementReferenceExistsError()

        controller.getAcknowledgementReference(testRegId)(FakeRequest()) returnsStatus CONFLICT
      }
    }

    "Calling getDocumentStatus" should {

      "return a Ok response when the user logged in" in new Setup {
        val json: JsValue = Json.parse(
          """
            | {
            |   "status": "draft",
            |   "ackRef": "testAckRef"
            | }
          """.stripMargin)
        AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
        ServiceMocks.mockGetDocumentStatus(json)

        val result: Future[Result] = controller.getDocumentStatus(testRegId)(FakeRequest())
        result returnsStatus OK
        contentAsJson(result) mustBe json
      }

      "return a Not Found response if there is no VAT Registration for the user's ID" in new Setup {
        AuthorisationMocks.mockAuthMongoResourceNotFound(testRegId, testInternalid)

        status(controller.getDocumentStatus(testRegId)(FakeRequest())) mustBe NOT_FOUND
      }
    }

    "deleteVatScheme" should {
      "call to deleteVatScheme return Ok with VatScheme" in new Setup {
        AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
        ServiceMocks.mockDeleteVatScheme(testRegId)

        status(controller.deleteVatScheme(testRegId)(FakeRequest())) mustBe OK
      }

      "call to deleteVatScheme return Internal server error" in new Setup {
        AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
        ServiceMocks.mockDeleteVatSchemeFail(testRegId)

        status(controller.deleteVatScheme(testRegId)(FakeRequest())) mustBe INTERNAL_SERVER_ERROR
      }

      "call to deleteVatScheme return Precondition failed" in new Setup {
        AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
        ServiceMocks.mockDeleteVatSchemeInvalidStatus(testRegId)

        status(controller.deleteVatScheme(testRegId)(FakeRequest())) mustBe PRECONDITION_FAILED
      }
    }
  }

  "Calling submitVATRegistration" should {
    "return a Forbidden response if the user is not logged in" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised(testRegId)

      val response: Future[Result] = controller.submitVATRegistration(testRegId)(FakeRequest().withBody(Json.obj()))
      status(response) mustBe Status.FORBIDDEN
    }

    "return an exception if the Submission Service can't make a DES submission" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      ServiceMocks.mockRetrieveVatScheme(testRegId, testVatScheme)


      when(mockSubmissionService.submitVatRegistration(
        ArgumentMatchers.eq(testRegId),
        ArgumentMatchers.eq(testUserHeaders)
      )(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.failed(UpstreamErrorResponse("message", 501)))

      val response: UpstreamErrorResponse = intercept[UpstreamErrorResponse](await(controller.submitVATRegistration(testRegId)(FakeRequest().withBody(
        Json.obj("userHeaders" -> testUserHeaders)
      ))))

      response mustBe UpstreamErrorResponse("message", 501)
    }

    "return an Ok response with acknowledgement reference for a valid submit" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      ServiceMocks.mockRetrieveVatScheme(testRegId, testVatScheme)

      when(mockSubmissionService.submitVatRegistration(
        ArgumentMatchers.eq(testRegId),
        ArgumentMatchers.eq(testUserHeaders)
      )(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.successful("BRVT00000000001"))

      val response: Future[Result] = controller.submitVATRegistration(testRegId)(FakeRequest().withBody(
        Json.obj("userHeaders" -> testUserHeaders)
      ))
      status(response) mustBe Status.OK
      contentAsJson(response) mustBe Json.toJson("BRVT00000000001")
    }
    "return an Ok response with acknowledgement reference for a valid submission with partners" in new Setup {
      val testPartner = Partner(testSoleTraderEntity, Individual, isLeadPartner = true)
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      ServiceMocks.mockRetrieveVatScheme(testRegId, testVatScheme.copy(partners = Some(List(testPartner))))

      when(mockSubmissionService.submitVatRegistration(
        ArgumentMatchers.eq(testRegId),
        ArgumentMatchers.eq(testUserHeaders)
      )(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.successful("BRVT00000000001"))

      val response: Future[Result] = controller.submitVATRegistration(testRegId)(FakeRequest().withBody(
        Json.obj("userHeaders" -> testUserHeaders)
      ))
      status(response) mustBe Status.OK
      contentAsJson(response) mustBe Json.toJson("BRVT00000000001")
    }
  }

  "call to getTurnoverEstimates" should {
    "return a 200 and TurnoverEstimates json when it is returned from the repository" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockVatRegistrationService.getTurnoverEstimates(any()))
        .thenReturn(Future.successful(Some(TurnoverEstimates(2024))))

      val result: Future[Result] = controller.getTurnoverEstimates(testRegId)(FakeRequest())
      val expectedJson: JsValue = Json.obj("turnoverEstimate" -> 2024)

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson
    }

    "return a 204 and no json when a None is returned from the repository" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockVatRegistrationService.getTurnoverEstimates(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getTurnoverEstimates(testRegId)(FakeRequest())
      status(result) mustBe 204
    }

    "return a 404 when a MissingRegDocument exception is thrown" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockVatRegistrationService.getTurnoverEstimates(any()))
        .thenReturn(Future.failed(MissingRegDocument(testRegId)))

      val result: Future[Result] = controller.getTurnoverEstimates(testRegId)(FakeRequest())
      status(result) mustBe 404
    }
  }

  "call to getThreshold" should {
    "return a 200 and TurnoverEstimates json when it is returned from the repository" in new Setup {
      val threshold: Threshold = Threshold(mandatoryRegistration = true,
        thresholdPreviousThirtyDays = Some(LocalDate.of(2016, 5, 12)),
        thresholdInTwelveMonths = Some(LocalDate.of(2016, 6, 25))
      )

      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockVatRegistrationService.getThreshold(any()))
        .thenReturn(Future.successful(Some(threshold)))

      val result: Future[Result] = controller.getThreshold(testRegId)(FakeRequest())
      val expectedJson: JsValue = Json.obj("mandatoryRegistration" -> true,
        "thresholdPreviousThirtyDays" -> "2016-05-12",
        "thresholdInTwelveMonths" -> "2016-06-25")

      status(result) mustBe 200
      contentAsJson(result) mustBe expectedJson
    }

    "return a 204 and no json when a None is returned from the repository" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockVatRegistrationService.getThreshold(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getThreshold(testRegId)(FakeRequest())
      status(result) mustBe 204
    }

    "return a 404 when a MissingRegDocument exception is thrown" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockVatRegistrationService.getThreshold(any()))
        .thenReturn(Future.failed(MissingRegDocument(testRegId)))

      val result: Future[Result] = controller.getThreshold(testRegId)(FakeRequest())
      status(result) mustBe 404
    }
  }

  "call to storeHonestyDeclaration" should {
    "return Ok and HonestyDeclaration json when it is returned from the repository" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      val testValue = true
      when(mockVatRegistrationService.storeHonestyDeclaration(any(), any()))
        .thenReturn(Future.successful(true))

      lazy val fakeRequest: FakeRequest[JsValue] =
        FakeRequest().withBody[JsValue](Json.parse(s"""{"honestyDeclaration":$testValue}"""))

      val result: Future[Result] = controller.storeHonestyDeclaration(testRegId)(fakeRequest)

      status(result) mustBe 200
    }
  }
}