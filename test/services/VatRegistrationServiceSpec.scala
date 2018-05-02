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

package services

import common.RegistrationId
import common.exceptions._
import enums.VatRegStatus
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models._
import models.api._
import models.external.CurrentProfile
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatRegistrationServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service = new RegistrationService {
      override val vatCancelUrl = "/test/uri"
      override val brConnector = mockBusRegConnector
      override val vatRestartUrl = "/test-uri"
      override val registrationRepository = mockRegistrationRepository
    }
  }

  implicit val hc = HeaderCarrier()

  "createNewRegistration" should {

    val vatScheme = VatScheme(RegistrationId("1"),internalid, None, None, None, status = VatRegStatus.draft)

    "return a existing VatScheme response " in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Some(vatScheme))

      service.createNewRegistration(internalid) returnsRight vatScheme
    }

    "call to retrieveVatScheme return VatScheme from DB" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatScheme)))
      service.retrieveVatScheme(RegistrationId("1")) returnsRight vatScheme
    }

    "call to retrieveVatScheme return None from DB " in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(None))
      service.retrieveVatScheme(RegistrationId("1")) returnsLeft ResourceNotFound("1")
    }

    "return a new VatScheme response " in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(None)
      when(mockRegistrationRepository.createNewVatScheme(RegistrationId("1"),internalid)).thenReturn(vatScheme)

      await(service.createNewRegistration(internalid).value) shouldBe Right(vatScheme)
    }

    "error when creating VatScheme" in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))
      val t = new Exception("Exception")

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(None)
      when(mockRegistrationRepository.createNewVatScheme(RegistrationId("1"),internalid)).thenReturn(Future.failed(t))

      service.createNewRegistration(internalid) returnsLeft GenericError(t)
    }

    "error with the DB when creating VatScheme" in new Setup {
      val businessRegistrationSuccessResponse = Right(CurrentProfile("1", None, ""))
      val t = InsertFailed(RegistrationId("regId"), "VatScheme")

      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(businessRegistrationSuccessResponse)
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(None)
      when(mockRegistrationRepository.createNewVatScheme(RegistrationId("1"),internalid)).thenReturn(Future.failed(t))

      service.createNewRegistration(internalid) returnsLeft GenericDatabaseError(t, Some("regId"))
    }

    "call to business service return ForbiddenException response " in new Setup {
      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(Left(ForbiddenAccess("forbidden")))

      service.createNewRegistration(internalid) returnsLeft ForbiddenAccess("forbidden")
    }

    "call to business service return NotFoundException response " in new Setup {
      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(Left(ResourceNotFound("notfound")))

      service.createNewRegistration(internalid) returnsLeft ResourceNotFound("notfound")
    }

    "call to business service return ErrorResponse response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockBusRegConnector.retrieveCurrentProfile(any(), any())).thenReturn(Left(GenericError(t)))

      service.createNewRegistration(internalid) returnsLeft GenericError(t)
    }

  }

  "call to updateLogicalGroup" should {

    val tradingDetails = TradingDetails(Some("test-name"), Some(false))

    "return Success response " in new Setup {
      when(mockRegistrationRepository.updateLogicalGroup(RegistrationId("1"), tradingDetails)).thenReturn(tradingDetails)
      service.updateLogicalGroup(RegistrationId("1"), tradingDetails) returnsRight tradingDetails
    }

    "return Error response " in new Setup {
      val t = new RuntimeException("Exception")
      when(mockRegistrationRepository.updateLogicalGroup(RegistrationId("1"), tradingDetails)).thenReturn(Future.failed(t))

      service.updateLogicalGroup(RegistrationId("1"), tradingDetails) returnsLeft GenericError(t)
    }

    "return Error response for MissingRegDocument" in new Setup {
      val regId = RegistrationId("regId")
      val t = new MissingRegDocument(regId)
      when(mockRegistrationRepository.updateLogicalGroup(regId, tradingDetails)).thenReturn(Future.failed(t))

      service.updateLogicalGroup(regId, tradingDetails) returnsLeft ResourceNotFound(s"No registration found for registration ID: $regId")
    }
  }

  "call to deleteVatScheme" should {
    "return true" when {
      "the document has been deleted" in new Setup {
        val idMatcher = RegistrationId(ArgumentMatchers.any())
        when(mockRegistrationRepository.retrieveVatScheme(idMatcher)(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(vatScheme)))

        when(mockRegistrationRepository.deleteVatScheme(ArgumentMatchers.any())(ArgumentMatchers.any()))
          .thenReturn(Future.successful(true))

        await(service.deleteVatScheme("1", VatRegStatus.draft, VatRegStatus.rejected)) shouldBe true
      }
    }

    "throw a MissingRegDoc exception" when {
      "no reg doc is found" in new Setup {
        val idMatcher = RegistrationId(ArgumentMatchers.any())
        when(mockRegistrationRepository.retrieveVatScheme(idMatcher)(ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))

        intercept[MissingRegDocument](await(service.deleteVatScheme("1", VatRegStatus.draft, VatRegStatus.rejected)))
      }
    }

    "throw an InvalidSubmissionStatus exception" when {
      "the reg doc status is not valid for cancellation" in new Setup {
        val idMatcher = RegistrationId(ArgumentMatchers.any())
        when(mockRegistrationRepository.retrieveVatScheme(idMatcher)(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(vatScheme.copy(status = VatRegStatus.submitted))))

        intercept[InvalidSubmissionStatus](await(service.deleteVatScheme("1", VatRegStatus.draft, VatRegStatus.rejected)))
      }
    }
  }


  "call to saveAcknowledgementReference" should {

    val vatScheme = VatScheme(RegistrationId("1"),internalid, None, None, None, status = VatRegStatus.draft)

    "return Success response " in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Some(vatScheme))
      when(mockRegistrationRepository.updateByElement(RegistrationId("1"), AcknowledgementReferencePath, ackRefNumber))
        .thenReturn(ackRefNumber)
      service.saveAcknowledgementReference(RegistrationId("1"), ackRefNumber) returnsRight ackRefNumber
    }

    val vatSchemeWithAckRefNum = vatScheme.copy(acknowledgementReference = Some(ackRefNumber))
    "return Error response " in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Some(vatSchemeWithAckRefNum))
      service.saveAcknowledgementReference(RegistrationId("1"), ackRefNumber) returnsLeft
        AcknowledgementReferenceExists(s"""Registration ID 1 already has an acknowledgement reference of: $ackRefNumber""")
    }

    "return Error response for MissingVatSchemeDocument" in new Setup {
      val regId = RegistrationId("regId")
      when(mockRegistrationRepository.retrieveVatScheme(regId)).thenReturn(Future.successful(None))
      service.saveAcknowledgementReference(regId, ackRefNumber) returnsLeft ResourceNotFound(s"VatScheme ID: $regId missing")
    }
  }

  "call to retrieveAcknowledgementReference" should {

    "call to retrieveAcknowledgementReference return AcknowledgementReference from DB" in new Setup {
      val vatSchemeWithAckRefNum = vatScheme.copy(acknowledgementReference = Some(ackRefNumber))
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatSchemeWithAckRefNum)))
      service.retrieveAcknowledgementReference(RegistrationId("1")) returnsRight ackRefNumber
    }

    "call to retrieveAcknowledgementReference return None from DB" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatScheme)))
      service.retrieveAcknowledgementReference(RegistrationId("1")) returnsLeft ResourceNotFound("AcknowledgementId")
    }
  }

  "call to getStatus" should {
    "return a correct JsValue" in new Setup {
      val expectedJson = Json.parse(
        """
          |{
          | "status":"draft",
          | "cancelURL":"/test/uri"
          |}
        """.stripMargin)

      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatScheme)))

      await(service.getStatus(RegistrationId("1"))) shouldBe expectedJson
    }

    "return a correct JsValue with ackRef" in new Setup {
      val vatSchemeWithAckRefNum = vatScheme.copy(acknowledgementReference = Some(ackRefNumber))
      val expectedJson = Json.parse(
        s"""
           |{
           |  "status":"draft",
           |  "ackRef":"BRPY000000000001",
           |  "cancelURL":"/test/uri"
           |}
          """.stripMargin)

      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId("1"))).thenReturn(Future.successful(Some(vatSchemeWithAckRefNum)))
      await(service.getStatus(RegistrationId("1"))) shouldBe expectedJson
    }
  }

  "call to clearDownDocment" should {
    "pass" when {
      "given a transactionid" in new Setup {
        when(mockRegistrationRepository.clearDownDocument(any())(any())).thenReturn(Future.successful(true))
        await(service.clearDownDocument("testTransID")) shouldBe true
      }
    }
  }
}
