/*
 * Copyright 2018 HM Revenue & Customs
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
import models.api._
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import repositories.RegistrationMongoRepository
import services.{RegistrationService, SubmissionService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class VatRegistrationControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import play.api.test.Helpers._

  val vatLodgingOfficer       = LodgingOfficer(
    currentAddress            = Some(scrsAddress),
    dob                       = LocalDate.of(1980, 1, 1),
    nino                      = "NB666666C",
    role                      = "director",
    name                      = name,
    changeOfName              = Some(changeOfName),
    currentOrPreviousAddress  = Some(currentOrPreviousAddress),
    contact                   = Some(contact),
    ivPassed                  = None,
    details                   = None
  )

  class SetupMocks(mockSubmission: Boolean) {
    val controller = new VatRegistrationController{
      override val registrationService: RegistrationService = mockRegistrationService
      override val submissionService: SubmissionService = mockSubmissionService
      override val registrationRepository: RegistrationMongoRepository = mockRegistrationMongoRepository
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
      override lazy val authConnector: AuthConnector = mockAuthConnector
      override private[controllers] def useMockSubmission = mockSubmission
    }
  }

  class Setup extends SetupMocks(false)
  class SetupWithMockSubmission extends SetupMocks(true)

  val registrationId = "reg-12345"

  "GET /" should {

    "return 403 if user not authenticated for newVatRegistration" in new Setup {
      AuthorisationMocks.mockAuthenticatedLoggedInNoCorrespondingData()

      controller.newVatRegistration(FakeRequest()) returnsStatus FORBIDDEN
    }

    "return 201 if a new VAT scheme is successfully created" in new Setup {
      AuthorisationMocks.mockAuthenticated(internalid)

      ServiceMocks.mockSuccessfulCreateNewRegistration(regId,internalid)
      controller.newVatRegistration()(FakeRequest()) returnsStatus CREATED
    }

    "call to retrieveVatScheme return Ok with VatScheme" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      ServiceMocks.mockRetrieveVatScheme(regId, vatScheme)

      controller.retrieveVatScheme(regId)(FakeRequest()) returnsStatus OK
    }

    "call to retrieveVatScheme return ServiceUnavailable" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      ServiceMocks.mockRetrieveVatSchemeThrowsException(regId)

      controller.retrieveVatScheme(regId)(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
    }

    "return 503 if RegistrationService encounters any problems" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      ServiceMocks.mockFailedCreateNewRegistration(regId,internalid)

      controller.newVatRegistration()(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
    }

    "return 503 if RegistrationService encounters any problems with the DB" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      ServiceMocks.mockFailedCreateNewRegistrationWithDbError(regId,internalid)

      controller.newVatRegistration()(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
    }
    "updateBankAccountDetails" should {

      val registrationId = "reg-12345"

      val accountNumber = "12345678"
      val sortCode = "12-34-56"
      val bankAccountDetails = BankAccountDetails("testAccountName", sortCode, accountNumber)
      val bankAccount = BankAccount(true,Some(bankAccountDetails))

      when(mockRegistrationMongo.store).thenReturn(mockRegistrationMongoRepository)

      "return a 200 if the update to mongo was successful" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value,internalid)
        when(mockRegistrationMongoRepository.updateBankAccount(any(), any())(any()))
          .thenReturn(Future.successful(bankAccount))

        val request = FakeRequest().withBody(
          Json.obj(
            "isProvided" -> true,
            "details" -> Json.obj(
              "name"     -> "testAccountName",
              "sortCode" -> sortCode,
              "number"   -> accountNumber
            )
          )
        )

        val result = controller.updateBankAccountDetails(regId.value)(request)
        status(result) shouldBe OK
      }
    }

    "fetchBankAccountDetails" should {
      val registrationId = "reg-12345"
      val accountNumber = "12345678"
      val sortCode = "12-34-56"
      val bankAccountDetails = BankAccountDetails("testAccountName", sortCode, accountNumber)
      val bankAccount = BankAccount(true, Some(bankAccountDetails))

        "return a 200 if the fetch from mongo was successful" in new Setup {
          AuthorisationMocks.mockAuthorised(regId.value, internalid)
          when(mockRegistrationMongoRepository.fetchBankAccount(any())(any()))
            .thenReturn(Future.successful(Some(bankAccount)))

          val expected = Json.obj(
            "isProvided" -> true,
            "details" -> Json.obj(
              "name" -> "testAccountName",
              "sortCode" -> sortCode,
              "number" -> accountNumber
            )
          )

          val result = controller.fetchBankAccountDetails(regId.value)(FakeRequest())
          status(result) shouldBe OK
          await(contentAsJson(result)) shouldBe expected
        }

        "return a 404 if the fetch from mongo returned nothing" in new Setup {
          AuthorisationMocks.mockAuthorised(regId.value, internalid)
          when(mockRegistrationMongoRepository.fetchBankAccount(any())(any()))
            .thenReturn(Future.successful(None))

          val result = controller.fetchBankAccountDetails(regId.value)(FakeRequest())
          status(result) shouldBe NOT_FOUND

        }
      }

    "fetchReturns" should {
      val registrationId = "reg-12345"
      val date = StartDate(Some(LocalDate.of(2017, 1, 1)))
      val returns = Returns(true, "quarterly", Some("jan"), date)

      val expected = Json.obj(
        "reclaimVatOnMostReturns" -> true,
        "frequency" -> "quarterly",
        "staggerStart" -> "jan",
        "start" -> date
      )

      "return a OK if the returns is present in the database" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        when(mockRegistrationMongoRepository.fetchReturns(any())(any()))
          .thenReturn(Future.successful(Some(returns)))

        val result = controller.fetchReturns(regId.value)(FakeRequest())
        status(result) shouldBe OK
        await(contentAsJson(result)) shouldBe expected
      }

      "return a NOT_FOUND if the returns is not present" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        when(mockRegistrationMongoRepository.fetchReturns(any())(any()))
          .thenReturn(Future.successful(None))

        val result = controller.fetchReturns(regId.value)(FakeRequest())
        status(result) shouldBe NOT_FOUND
      }
    }

    "fetchTurnoverEstimates" should {

      val vatTaxable = 1000L
      val turnoverEstimates = TurnoverEstimates(vatTaxable)

      "return a 200 and TurnoverEstimates json when it is returned from the repository" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        when(mockRegistrationMongoRepository.fetchTurnoverEstimates(any())(any()))
          .thenReturn(Future.successful(Some(turnoverEstimates)))

        val result = await(controller.fetchTurnoverEstimates(regId.value)(FakeRequest()))
        val expectedJson: JsValue = Json.obj("vatTaxable" -> 1000)

        status(result) shouldBe 200
        contentAsJson(result) shouldBe expectedJson
      }

      "return a 204 and no json when a None is returned from the repository" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        when(mockRegistrationMongoRepository.fetchTurnoverEstimates(any())(any()))
          .thenReturn(Future.successful(None))

        val result = await(controller.fetchTurnoverEstimates(regId.value)(FakeRequest()))
        status(result) shouldBe 204
      }

      "return a 404 when a MissingRegDocument exception is thrown" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        when(mockRegistrationMongoRepository.fetchTurnoverEstimates(any())(any()))
          .thenReturn(Future.failed(MissingRegDocument(regId)))

        val result = await(controller.fetchTurnoverEstimates(regId.value)(FakeRequest()))
        status(result) shouldBe 404
      }
    }

    "updateTurnoverEstimates" should {

      val registrationId = "reg-12345"
      val vatTaxable = 1000L
      val turnoverEstimates = TurnoverEstimates(vatTaxable)

      when(mockRegistrationMongo.store).thenReturn(mockRegistrationMongoRepository)

      "return a 200 if the update to mongo was successful" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        when(mockRegistrationMongoRepository.updateTurnoverEstimates(any(), any())(any()))
          .thenReturn(Future.successful(turnoverEstimates))

        val request = FakeRequest().withBody(
          Json.obj(
            "vatTaxable" -> vatTaxable
          )
        )

        val result = controller.updateTurnoverEstimates(regId.value)(request)
        status(result) shouldBe OK
      }
    }

    "updateReturns" should {

      import Returns._

      val registrationId = "reg-12345"
      val startDate = StartDate(Some(LocalDate of (1990, 10, 10)))
      val returns: Returns = Returns(reclaimVatOnMostReturns = true, MONTHLY, Some(JAN), startDate)

      when(mockRegistrationMongo.store).thenReturn(mockRegistrationMongoRepository)

      "return a 200 if the update to mongo is successful" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        when(mockRegistrationMongoRepository.updateReturns(any(), any())(any()))
          .thenReturn(Future.successful(returns))

        val request = FakeRequest().withBody(Json.obj(
          "reclaimVatOnMostReturns" -> true,
          "frequency" -> MONTHLY,
          "staggerStart" -> JAN,
          "start" -> Some(startDate))
        )

        val result = controller.updateReturns(regId.value)(request)
        status(result) shouldBe OK
      }
    }

    "getAcknowledgementReference" should {

      "call getAcknowledgementReference return Ok with Acknowledgement Reference" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        ServiceMocks.mockGetAcknowledgementReference(ackRefNumber)

        controller.getAcknowledgementReference(regId)(FakeRequest()) returnsStatus OK
      }

      "call getAcknowledgementReference return ServiceUnavailable" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        ServiceMocks.mockGetAcknowledgementReferenceServiceUnavailable(exception)

        controller.getAcknowledgementReference(regId)(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
      }

      "call getAcknowledgementReference return AcknowledgementReferenceExists Error" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        ServiceMocks.mockGetAcknowledgementReferenceExistsError()

        controller.getAcknowledgementReference(regId)(FakeRequest()) returnsStatus CONFLICT
      }

      "return the fake acknowledgement reference for a regID if mock submission is enabled" in new SetupWithMockSubmission {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        val result = controller.getAcknowledgementReference(regId)(FakeRequest())

        result returnsStatus OK
        await(contentAsJson(result)) shouldBe Json.toJson("BRVT000000" + regId)
      }
    }

    "Calling getDocumentStatus" should {

      "return a Ok response when the user logged in" in new Setup {
        val json = Json.parse(
          """
            | {
            |   "status": "draft",
            |   "ackRef": "testAckRef"
            | }
          """.stripMargin)
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        ServiceMocks.mockGetDocumentStatus(json)

        val result = controller.getDocumentStatus(regId)(FakeRequest())
        result returnsStatus OK
        await(contentAsJson(result)) shouldBe json
      }

      "return a Not Found response if there is no VAT Registration for the user's ID" in new Setup {
        AuthorisationMocks.mockAuthMongoResourceNotFound(regId.value,internalid)

        status(controller.getDocumentStatus(regId)(FakeRequest())) shouldBe NOT_FOUND
      }
    }

    "deleteVatScheme" should {
      "call to deleteVatScheme return Ok with VatScheme" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        ServiceMocks.mockDeleteVatScheme("testId")

        status(controller.deleteVatScheme("testId")(FakeRequest())) shouldBe OK
      }

      "call to deleteVatScheme return Internal server error" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        ServiceMocks.mockDeleteVatSchemeFail("testId")

        status(controller.deleteVatScheme("testId")(FakeRequest())) shouldBe INTERNAL_SERVER_ERROR
      }

      "call to deleteVatScheme return Precondition failed" in new Setup {
        AuthorisationMocks.mockAuthorised(regId.value, internalid)
        ServiceMocks.mockDeleteVatSchemeInvalidStatus("testId")

        status(controller.deleteVatScheme("testId")(FakeRequest())) shouldBe PRECONDITION_FAILED
      }
    }
  }

  "Calling submitVATRegistration" should {
    "return a Forbidden response if the user is not logged in" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised(regId.value)

      val response = controller.submitVATRegistration(regId)(FakeRequest())
      status(response) shouldBe Status.FORBIDDEN
    }

    "return a BadRequest response when the Submission Service can't make a DES submission" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      ServiceMocks.mockRetrieveVatScheme(regId, vatScheme)

      val idMatcher: RegistrationId = RegistrationId(anyString())
      System.setProperty("feature.mockSubmission", "false")

      when(mockSubmissionService.submitVatRegistration(idMatcher)(any[HeaderCarrier]()))
        .thenReturn(Future.failed(new Exception("missing data")))

      val response = await(controller.submitVATRegistration(regId)(FakeRequest()))

      status(response) shouldBe Status.BAD_REQUEST
      contentAsString(response) shouldBe "Registration was submitted without full data: missing data"
    }

    "return an Ok response with acknowledgement reference for a valid submit" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)
      ServiceMocks.mockRetrieveVatScheme(regId, vatScheme)
      val idMatcher: RegistrationId = RegistrationId(anyString())

      System.setProperty("feature.mockSubmission", "false")

      when(mockSubmissionService.submitVatRegistration(idMatcher)(any[HeaderCarrier]()))
        .thenReturn(Future.successful("BRVT00000000001"))

      val response = controller.submitVATRegistration(regId)(FakeRequest())
      status(response) shouldBe Status.OK
      await(contentAsJson(response)) shouldBe Json.toJson("BRVT00000000001")
    }

    "return an Ok response with fake ack ref for a mock submission" in new SetupWithMockSubmission {
      AuthorisationMocks.mockAuthorised(regId.value, internalid)

      val response = controller.submitVATRegistration(regId)(FakeRequest())
      status(response) shouldBe Status.OK
      await(contentAsJson(response)) shouldBe Json.toJson(s"BRVT000000$regId")
    }
  }

  "call to clearDownDocment" should {
    "pass" when {
      "given a transactionid" in new Setup {
        when(mockRegistrationService.clearDownDocument(any())(any())).thenReturn(Future.successful(true))
        val resp = await(controller.clearDownDocument("TransID")(FakeRequest()))
        status(resp) shouldBe Status.OK
      }
    }
    "fail" when {
      "given a transactionid that isn't found in mongo" in new Setup {
        when(mockRegistrationService.clearDownDocument(any())(any())).thenReturn(Future.successful(false))
        val resp = await(controller.clearDownDocument("TransID")(FakeRequest()))
        status(resp) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "call to saveTransId" should {
    "return Ok" when {
      "the transaction id was saved to the document" in new Setup {
        val regId = "regId"

        when(mockRegistrationMongoRepository.saveTransId(any(), RegistrationId(any()))(any()))
          .thenReturn(Future.successful("transId"))

        lazy val fakeRequest: FakeRequest[JsValue] =
          FakeRequest().withBody[JsValue](Json.parse("""{"transactionID":"transId"}"""))

        val resp = await(controller.saveTransId(regId)(fakeRequest))
        status(resp) shouldBe Status.OK
      }
    }
  }
}