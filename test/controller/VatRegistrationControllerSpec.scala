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

package controller

import common.exceptions.AcknowledgementReferenceExists
import controllers.VatRegistrationController
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models._
import models.api._
import play.api.libs.json.Json
import play.api.mvc.Results.Accepted
import play.api.test.FakeRequest
import play.api.test.Helpers._


class VatRegistrationControllerSpec extends VatRegSpec with VatRegistrationFixture {

  val vatLodgingOfficer = VatLodgingOfficer(scrsAddress, DateOfBirth(1, 1, 1980), "NB666666C", "director", name, changeOfName, currentOrPreviousAddress, contact)

  class Setup {
    val controller = new VatRegistrationController(mockAuthConnector, mockRegistrationService, mockSubmissionService)
    AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
  }

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
        zeroRatedTurnoverEstimate = Some(10000000000L),
        reclaimVatOnMostReturns = true,
        accountingPeriods = VatAccountingPeriod("monthly")
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

    "updateTradingDetails" should {

      val fakeRequest = FakeRequest().withBody(Json.toJson(tradingDetails))

      "call updateTradingDetails return ACCEPTED" in new Setup {
        ServiceMocks.mockSuccessfulUpdateLogicalGroup(tradingDetails)
        controller.updateTradingDetails(regId)(fakeRequest) returns Accepted(Json.toJson(tradingDetails))
      }

      "call updateTradingDetails return ServiceUnavailable" in new Setup {
        ServiceMocks.mockServiceUnavailableUpdateLogicalGroup(tradingDetails, exception)
        controller.updateTradingDetails(regId)(fakeRequest) returnsStatus SERVICE_UNAVAILABLE
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

      "call getAcknowledgementReference return AcknowledgementReferenceExists Erorr" in new Setup {
        ServiceMocks.mockGetAcknowledgementReferenceExistsError()
        controller.getAcknowledgementReference(regId)(FakeRequest()) returnsStatus CONFLICT
      }
    }

    "deleteVatScheme" should {
      "call to deleteVatScheme return Ok with VatScheme" in new Setup {
        ServiceMocks.mockDeleteVatScheme(regId)
        controller.deleteVatScheme(regId)(FakeRequest()) returnsStatus OK
      }

      "call to deleteVatScheme return ServiceUnavailable" in new Setup {
        ServiceMocks.mockDeleteVatSchemeThrowsException(regId)
        controller.deleteVatScheme(regId)(FakeRequest()) returnsStatus SERVICE_UNAVAILABLE
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

}
