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
import common.exceptions.UpdateFailed
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models._
import models.api._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Results.Accepted
import play.api.test.FakeRequest
import play.api.test.Helpers._
import org.mockito.Mockito.when
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.Status
import play.api.mvc.Result
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future


class VatRegistrationControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import fakeApplication.materializer

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


  class Setup {
    val controller = new VatRegistrationController(
      mockAuthConnector,
      mockRegistrationService,
      mockSubmissionService,
      mockRegistrationMongo
    )

    AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
    when(mockRegistrationMongo.store).thenReturn(mockRegistrationMongoRepository)
  }

  val registrationId = "reg-12345"

  "GET /" should {

    "return 403 if user not authenticated" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised()
      controller.newVatRegistration(FakeRequest()) returnsStatus FORBIDDEN
    }

    "return 201 if a new VAT scheme is successfully created" in new Setup {
      ServiceMocks.mockSuccessfulCreateNewRegistration(regId)
      controller.newVatRegistration()(FakeRequest()) returnsStatus CREATED
    }

    "call to retrieveVatScheme return Ok with VatScheme" in new Setup {
      ServiceMocks.mockRetrieveVatScheme(regId, vatScheme)
      controller.retrieveVatScheme(regId)(FakeRequest()) returnsStatus OK
    }

    "call to retrieveVatScheme return ServiceUnavailable" in new Setup {
      ServiceMocks.mockRetrieveVatSchemeThrowsException(regId)
      controller.retrieveVatScheme(regId)(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
    }

    "call to fetchTradingDetails return Ok with TradingDetails if it found trading details" in new Setup {
      ServiceMocks.mockRetrieveTradingDetails(Some(TradingDetails(Some("tradingDetails"),Some(true))))
      controller.fetchTradingDetails("regId")(FakeRequest()) returnsStatus OK
    }

    "call to fetchTradingDetails return Not found without trading details if it did not find trading details" in new Setup {
      ServiceMocks.mockRetrieveTradingDetails(None)
      controller.fetchTradingDetails("regId")(FakeRequest()) returnsStatus NOT_FOUND
    }

    "return 503 if RegistrationService encounters any problems" in new Setup {
      ServiceMocks.mockFailedCreateNewRegistration(regId)
      controller.newVatRegistration()(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
    }

    "return 503 if RegistrationService encounters any problems with the DB" in new Setup {
      ServiceMocks.mockFailedCreateNewRegistrationWithDbError(regId)
      controller.newVatRegistration()(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
    }

    "updateVatFinancials" should {

      val vatFinancials = VatFinancials(Some(VatBankAccount("Reddy", "10-01-01", "12345678")),
        turnoverEstimate = 10000000000L,
        zeroRatedTurnoverEstimate = Some(10000000000L)
      )

      val fakeRequest = FakeRequest().withBody(Json.toJson(vatFinancials))

      "call updateVatFinancials return ACCEPTED" in new Setup {
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(vatFinancials)
        controller.updateVatFinancials(regId)(fakeRequest) returnsStatus ACCEPTED
      }

      "call updateVatFinancials return ServiceUnavailable" in new Setup {
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(vatFinancials, exception)
        controller.updateVatFinancials(regId)(fakeRequest) returnsStatus SERVICE_UNAVAILABLE
      }

    }

    "updateVatTradingDetails" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(tradingDetails))

      "call updateTradingDetails return ACCEPTED" in new Setup {
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(tradingDetails)
        controller.updateVatTradingDetails(regId)(fakeRequest) returns Accepted(Json.toJson(tradingDetails))
      }

      "call updateTradingDetails return ServiceUnavailable" in new Setup {
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(tradingDetails, exception)
        controller.updateVatTradingDetails(regId)(fakeRequest) returnsStatus SERVICE_UNAVAILABLE
      }
    }

    "updateTradingDetails" should {

      val registrationId = "reg-12345"
      val tradingName = "testTradingName"
      val tradingDetails = TradingDetails(Some("testTradingName"), Some(true))

      when(mockRegistrationMongo.store).thenReturn(mockRegistrationMongoRepository)

      "return a 200 if the update to mongo is successful" in new Setup {
        when(mockRegistrationMongoRepository.updateTradingDetails(any(), any())(any()))
          .thenReturn(Future.successful(tradingDetails))

        val request: FakeRequest[JsObject] = FakeRequest().
          withBody(Json.obj("tradingDetails" -> Json.obj("tradingName" -> tradingName, "eoriRequested" -> true)))

        val result: Result = controller.updateTradingDetails(registrationId)(request)

        status(result) shouldBe OK
      }
    }

    "updateBankAccountDetails" should {

      val registrationId = "reg-12345"

      val accountNumber = "12345678"
      val sortCode = "12-34-56"
      val bankAccountDetails = BankAccountDetails("testAccountName", sortCode, accountNumber)
      val bankAccount = BankAccount(true,Some(bankAccountDetails))

      when(mockRegistrationMongo.store).thenReturn(mockRegistrationMongoRepository)

      "return a 200 if the update to mongo was successful" in new Setup {
        when(mockRegistrationMongoRepository.updateBankAccount(any(), any())(any()))
          .thenReturn(Future.successful(bankAccount))

        val request: FakeRequest[JsObject] = FakeRequest().withBody(
          Json.obj(
            "isProvided" -> true,
            "details" -> Json.obj(
              "name" -> "testAccountName",
              "sortCode" -> sortCode,
              "number" -> accountNumber)
          )
        )

        val result: Result = controller.updateBankAccountDetails(registrationId)(request)

        status(result) shouldBe OK
      }
    }

    "fetchBankAccountDetails with a bank account" should {

      val registrationId = "reg-12345"

      val accountNumber = "12345678"
      val sortCode = "12-34-56"
      val bankAccountDetails = BankAccountDetails("testAccountName", sortCode, accountNumber)
      val bankAccount = BankAccount(true,Some(bankAccountDetails))

      "return a 200 if the fetch from mongo was successful" in new Setup {
        when(mockRegistrationMongoRepository.fetchBankAccount(any())(any()))
          .thenReturn(Future.successful(Some(bankAccount)))

        val expected = Json.obj(
          "isProvided" -> true,
          "details" -> Json.obj(
            "name" -> "testAccountName",
            "sortCode" -> sortCode,
            "number" -> accountNumber)
        )


        val result = controller.fetchBankAccountDetails(registrationId)(FakeRequest())

        status(result) shouldBe OK
        await(jsonBodyOf(result)) shouldBe expected
      }
    }

    "fetchBankAccountDetails with no bank account" should {

      val registrationId = "reg-12345"

      val accountNumber = "12345678"
      val sortCode = "12-34-56"
      val bankAccountDetails = BankAccountDetails("testAccountName", sortCode, accountNumber)
      val bankAccount = BankAccount(true,Some(bankAccountDetails))

      "return a 404 if the fetch from mongo returned nothing" in new Setup {
        when(mockRegistrationMongoRepository.fetchBankAccount(any())(any()))
          .thenReturn(Future.successful(None))

       val result = controller.fetchBankAccountDetails(registrationId)(FakeRequest())

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
        when(mockRegistrationMongoRepository.fetchReturns(any())(any()))
          .thenReturn(Future.successful(Some(returns)))

        val result = controller.fetchReturns(registrationId)(FakeRequest())

        status(result) shouldBe OK
        await(jsonBodyOf(result)) shouldBe expected
      }

      "return a NOT_FOUND if the returns is not present" in new Setup {
        when(mockRegistrationMongoRepository.fetchReturns(any())(any()))
          .thenReturn(Future.successful(None))

        val result = controller.fetchReturns(registrationId)(FakeRequest())

        status(result) shouldBe NOT_FOUND
      }
    }

    "fetchTurnoverEstimates" should {

      val vatTaxable = 1000L
      val turnoverEstimates = TurnoverEstimates(vatTaxable)

      "return a 200 and TurnoverEstimates json when it is returned from the repository" in new Setup {
        when(mockRegistrationMongoRepository.fetchTurnoverEstimates(any())(any()))
          .thenReturn(Future.successful(Some(turnoverEstimates)))

        val result: Result = await(controller.fetchTurnoverEstimates(registrationId)(FakeRequest()))
        val expectedJson: JsValue = Json.obj("vatTaxable" -> 1000)

        status(result) shouldBe 200
        contentAsJson(result) shouldBe expectedJson
      }

      "return a 204 and no json when a None is returned from the repository" in new Setup {
        when(mockRegistrationMongoRepository.fetchTurnoverEstimates(any())(any()))
          .thenReturn(Future.successful(None))

        val result: Result = await(controller.fetchTurnoverEstimates(registrationId)(FakeRequest()))
        status(result) shouldBe 204
      }

      "return a 404 when a MissingRegDocument exception is thrown" in new Setup {
        when(mockRegistrationMongoRepository.fetchTurnoverEstimates(any())(any()))
          .thenReturn(Future.failed(MissingRegDocument(regId)))

        val result: Result = await(controller.fetchTurnoverEstimates(registrationId)(FakeRequest()))
        status(result) shouldBe 404
      }
    }

    "updateTurnoverEstimates" should {

      val registrationId = "reg-12345"

      val vatTaxable = 1000L
      val turnoverEstimates = TurnoverEstimates(vatTaxable)

      when(mockRegistrationMongo.store).thenReturn(mockRegistrationMongoRepository)

      "return a 200 if the update to mongo was successful" in new Setup {
        when(mockRegistrationMongoRepository.updateTurnoverEstimates(any(), any())(any()))
          .thenReturn(Future.successful(turnoverEstimates))

        val request: FakeRequest[JsObject] = FakeRequest().withBody(
          Json.obj(
            "vatTaxable" -> vatTaxable
          )
        )

        val result: Result = controller.updateTurnoverEstimates(registrationId)(request)

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
        when(mockRegistrationMongoRepository.updateReturns(any(), any())(any()))
          .thenReturn(Future.successful(returns))

        val request: FakeRequest[JsObject] = FakeRequest().withBody(Json.obj(
          "reclaimVatOnMostReturns" -> true,
          "frequency" -> MONTHLY,
          "staggerStart" -> JAN,
          "start" -> Some(startDate))
        )

        val result: Result = controller.updateReturns(registrationId)(request)

        status(result) shouldBe OK
      }
    }

    "updateSicAndCompliance" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(sicAndCompliance))

      "call updateSicAndCompliance return ACCEPTED" in new Setup {
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(sicAndCompliance)
        controller.updateSicAndCompliance(regId)(fakeRequest) returns Accepted(Json.toJson(sicAndCompliance))
      }

      "call updateSicAndCompliance return ServiceUnavailable" in new Setup {
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(sicAndCompliance, exception)
        controller.updateSicAndCompliance(regId)(fakeRequest) returnsStatus SERVICE_UNAVAILABLE
      }

    }

    "updateVatContact" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(vatContact))

      "call updateVatContact return ACCEPTED" in new Setup {
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(vatContact)
        controller.updateVatContact(regId)(fakeRequest) returns Accepted(Json.toJson(vatContact))
      }

      "call updateVatContact return ServiceUnavailable" in new Setup {
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(vatContact, exception)
        controller.updateVatContact(regId)(fakeRequest) returnsStatus SERVICE_UNAVAILABLE
      }

    }

    "updateVatEligibility" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(vatEligibility))

      "call updateVatEligibility return ACCEPTED" in new Setup {
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(vatEligibility)
        controller.updateVatEligibility(regId)(fakeRequest) returns Accepted(Json.toJson(vatEligibility))
      }

      "call updateVatEligibility return ServiceUnavailable" in new Setup {
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(vatEligibility, exception)
        controller.updateVatEligibility(regId)(fakeRequest) returnsStatus SERVICE_UNAVAILABLE
      }
    }

    "updateVatLodgingOfficer" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(vatLodgingOfficer))

      "call updateVatLodgingOfficer return ACCEPTED" in new Setup {
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(vatLodgingOfficer)
        controller.updateLodgingOfficer(regId)(fakeRequest) returns Accepted(Json.toJson(vatLodgingOfficer))
      }

      "call updateVatLodgingOfficer return ServiceUnavailable" in new Setup {
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(vatLodgingOfficer, exception)
        controller.updateLodgingOfficer(regId)(fakeRequest) returnsStatus SERVICE_UNAVAILABLE
      }
    }

    "updateFlatRateScheme" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(vatFlatRateScheme))

      "call updateFlatRateScheme return ACCEPTED" in new Setup {
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(vatFlatRateScheme)
        controller.updateFlatRateScheme(regId)(fakeRequest) returns Accepted(Json.toJson(vatFlatRateScheme))
      }

      "call updateFlatRateScheme return ServiceUnavailable" in new Setup {
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(vatFlatRateScheme, exception)
        controller.updateFlatRateScheme(regId)(fakeRequest) returnsStatus SERVICE_UNAVAILABLE
      }

    }

    "getAcknowledgementReference" should {

      "call getAcknowledgementReference return Ok with Acknowledgement Reference" in new Setup {
        ServiceMocks.mockGetAcknowledgementReference(ackRefNumber)
        controller.getAcknowledgementReference(regId)(FakeRequest()) returnsStatus OK
      }

      "call getAcknowledgementReference return ServiceUnavailable" in new Setup {
        ServiceMocks.mockGetAcknowledgementReferenceServiceUnavailable(exception)
        controller.getAcknowledgementReference(regId)(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
      }

      "call getAcknowledgementReference return AcknowledgementReferenceExists Error" in new Setup {
        ServiceMocks.mockGetAcknowledgementReferenceExistsError()
        controller.getAcknowledgementReference(regId)(FakeRequest()) returnsStatus CONFLICT
      }

      "return the fake acknowledgement reference for a regID if mock submission is enabled" in new Setup {
        System.setProperty("feature.mockSubmission", "true")

        val result = controller.getAcknowledgementReference(regId)(FakeRequest())
        result returnsStatus OK
        result returnsJson Json.toJson("BRVT000000" + regId)
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

        ServiceMocks.mockGetDocumentStatus(json)
        controller.getDocumentStatus(regId)(FakeRequest()) returnsStatus OK
        controller.getDocumentStatus(regId)(FakeRequest()) returnsJson json
      }

      "return a Not Found response if there is no VAT Registration for the user's ID" in new Setup {
        when(mockRegistrationService.getStatus(RegistrationId(ArgumentMatchers.any()))(ArgumentMatchers.any()))
          .thenReturn(Future.failed(new MissingRegDocument(RegistrationId(""))))

        status(controller.getDocumentStatus(regId)(FakeRequest())) shouldBe NOT_FOUND
      }
    }

    "deleteVatScheme" should {
      "call to deleteVatScheme return Ok with VatScheme" in new Setup {
        ServiceMocks.mockDeleteVatScheme("testId")
        status(controller.deleteVatScheme("testId")(FakeRequest())) shouldBe OK
      }

      "call to deleteVatScheme return Internal server error" in new Setup {
        ServiceMocks.mockDeleteVatSchemeFail("testId")
        status(controller.deleteVatScheme("testId")(FakeRequest())) shouldBe INTERNAL_SERVER_ERROR
      }

      "call to deleteVatScheme return Precondition failed" in new Setup {
        ServiceMocks.mockDeleteVatSchemeInvalidStatus("testId")
        status(controller.deleteVatScheme("testId")(FakeRequest())) shouldBe PRECONDITION_FAILED
      }
    }

    "deleteByElement" should {
      "call to deleteByElement return Ok with VatScheme" in new Setup {
        ServiceMocks.mockDeleteByElement(regId, VatBankAccountPath)
        controller.deleteByElement(regId, VatBankAccountPath)(FakeRequest()) returnsStatus OK
      }

      "call to deleteBankAccountDetails return ServiceUnavailable" in new Setup {
        ServiceMocks.mockDeleteByElementThrowsException(regId, VatBankAccountPath)
        controller.deleteByElement(regId, VatBankAccountPath)(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
      }
    }
  }

  "Calling submitPAYERegistration" should {
    "return a Forbidden response if the user is not logged in" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised()

      val response = controller.submitVATRegistration(regId)(FakeRequest())

      status(response) shouldBe Status.FORBIDDEN
    }

    "return a BadRequest response when the Submission Service can't make a DES submission" in new Setup {
      ServiceMocks.mockRetrieveVatScheme(regId, vatScheme)
      val idMatcher: RegistrationId = RegistrationId(ArgumentMatchers.anyString())

      System.setProperty("feature.mockSubmission", "false")

      when(mockSubmissionService.submitVatRegistration(idMatcher)(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.failed(new Exception("missing data")))

      val response = await(controller.submitVATRegistration(regId)(FakeRequest()))

      status(response) shouldBe Status.BAD_REQUEST
      bodyOf(response) shouldBe "Registration was submitted without full data: missing data"
    }

    "return an Ok response with acknowledgement reference for a valid submit" in new Setup {
      ServiceMocks.mockRetrieveVatScheme(regId, vatScheme)
      val idMatcher: RegistrationId = RegistrationId(ArgumentMatchers.anyString())

      System.setProperty("feature.mockSubmission", "false")

      when(mockSubmissionService.submitVatRegistration(idMatcher)(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful("BRVT00000000001"))

      val response = controller.submitVATRegistration(regId)(FakeRequest())

      status(response) shouldBe Status.OK
      jsonBodyOf(await(response)) shouldBe Json.toJson("BRVT00000000001")
    }

    "return an Ok response with fake ack ref for a mock submission" in new Setup {
      System.setProperty("feature.mockSubmission", "true")

      val response = controller.submitVATRegistration(regId)(FakeRequest())

      status(response) shouldBe Status.OK
      jsonBodyOf(await(response)) shouldBe Json.toJson(s"BRVT000000$regId")
    }
  }

  "updateIVStatus" should {
    "return an Ok" when {
      "the users IV status has been successfully updated" in new Setup {
        val request = FakeRequest().withBody(Json.parse("""{"ivPassed" : true}"""))

        when(mockRegistrationService.updateIVStatus(ArgumentMatchers.any[String](), ArgumentMatchers.any[Boolean]())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(true))

        val result = controller.updateIVStatus("testRegId")(request)
        status(result) shouldBe OK
      }
    }

    "return an InternalServerError" when {
      "there was a problem updating the users IV status" in new Setup {
        val request = FakeRequest().withBody(Json.parse("""{"ivPassed" : true}"""))

        when(mockRegistrationService.updateIVStatus(ArgumentMatchers.any[String](), ArgumentMatchers.any[Boolean]())(ArgumentMatchers.any()))
          .thenReturn(Future.failed(UpdateFailed(RegistrationId("testRegId"), "testModel")))

        val result = controller.updateIVStatus("testRegId")(request)
        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
