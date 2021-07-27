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

package services

import common.exceptions._
import enums.VatRegStatus
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.{Threshold, TurnoverEstimates, _}
import models.submission.UkCompany
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import scala.concurrent.Future

class VatRegistrationCreatedServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    lazy val service: VatRegistrationService = new VatRegistrationService(mockRegistrationMongoRepository, backendConfig, mockHttpClient) {
      override lazy val vatCancelUrl = "/test/uri"
      override lazy val vatRestartUrl = "/test-uri"
    }
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  //TODO - fix these tests when we define how to create new journeys
  "createNewRegistration" ignore {

    val vatScheme = VatScheme(testRegId, testInternalid, None, None, None, status = VatRegStatus.draft)

    "return a existing VatScheme response " in new Setup {

      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(Some(vatScheme)))

      service.createNewRegistration(testInternalid) returnsRight vatScheme
    }

    "call to retrieveVatScheme return VatScheme from DB" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(Some(vatScheme)))
      service.retrieveVatScheme(testRegId) returnsRight vatScheme
    }

    "call to retrieveVatScheme return None from DB " in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(None))
      service.retrieveVatScheme(testRegId) returnsLeft ResourceNotFound("1")
    }

    "return a new VatScheme response " in new Setup {

      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(None))
      when(mockRegistrationMongoRepository.createNewVatScheme(testRegId, testInternalid)).thenReturn(Future.successful(vatScheme))

      await(service.createNewRegistration(testInternalid).value) mustBe Right(vatScheme)
    }

    "error when creating VatScheme" in new Setup {
      val t = new Exception("Exception")

      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(None))
      when(mockRegistrationMongoRepository.createNewVatScheme(testRegId, testInternalid)).thenReturn(Future.failed(t))

      service.createNewRegistration(testInternalid) returnsLeft GenericError(t)
    }

    "error with the DB when creating VatScheme" in new Setup {
      val error: InsertFailed = InsertFailed("regId", "VatScheme")

      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(None))
      when(mockRegistrationMongoRepository.createNewVatScheme(testRegId, testInternalid)).thenReturn(Future.failed(error))

      service.createNewRegistration(testInternalid) returnsLeft GenericDatabaseError(error, Some("regId"))
    }

    "call to business service return ForbiddenException response " in new Setup {
      service.createNewRegistration(testInternalid) returnsLeft ForbiddenAccess("forbidden")
    }

    "call to business service return NotFoundException response " in new Setup {
      service.createNewRegistration(testInternalid) returnsLeft ResourceNotFound("notfound")
    }

    "call to business service return ErrorResponse response " in new Setup {
      val t = new RuntimeException("Exception")

      service.createNewRegistration(testInternalid) returnsLeft GenericError(t)
    }

  }

  "call to deleteVatScheme" should {
    "return true" when {
      "the document has been deleted" in new Setup {
        AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
        when(mockRegistrationMongoRepository.retrieveVatScheme(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(testVatScheme)))

        when(mockRegistrationMongoRepository.deleteVatScheme(ArgumentMatchers.any()))
          .thenReturn(Future.successful(true))

        await(service.deleteVatScheme(testRegId, VatRegStatus.draft, VatRegStatus.rejected)) mustBe true
      }
    }

    "throw a MissingRegDoc exception" when {
      "no reg doc is found" in new Setup {
        AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
        when(mockRegistrationMongoRepository.retrieveVatScheme(ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))

        intercept[MissingRegDocument](await(service.deleteVatScheme(testRegId, VatRegStatus.draft, VatRegStatus.rejected)))
      }
    }

    "throw an InvalidSubmissionStatus exception" when {
      "the reg doc status is not valid for cancellation" in new Setup {
        AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
        when(mockRegistrationMongoRepository.retrieveVatScheme(ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(testVatScheme.copy(status = VatRegStatus.submitted))))

        intercept[InvalidSubmissionStatus](await(service.deleteVatScheme(testRegId, VatRegStatus.draft, VatRegStatus.rejected)))
      }
    }
  }

  "call to retrieveAcknowledgementReference" should {

    "call to retrieveAcknowledgementReference return AcknowledgementReference from DB" in new Setup {
      val vatSchemeWithAckRefNum: VatScheme = testVatScheme.copy(acknowledgementReference = Some(testAckReference))
      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(Some(vatSchemeWithAckRefNum)))
      service.retrieveAcknowledgementReference(testRegId) returnsRight testAckReference
    }

    "call to retrieveAcknowledgementReference return None from DB" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(Some(testVatScheme)))
      service.retrieveAcknowledgementReference(testRegId) returnsLeft ResourceNotFound("AcknowledgementId")
    }
  }

  "call to getStatus" should {
    "return a correct JsValue" in new Setup {
      val expectedJson: JsValue = Json.parse(
        """
          |{
          | "status":"draft",
          | "cancelURL":"/test/uri"
          |}
        """.stripMargin)

      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(Some(testVatScheme)))

      await(service.getStatus(testRegId)) mustBe expectedJson
    }

    "return a correct JsValue with ackRef" in new Setup {
      val vatSchemeWithAckRefNum: VatScheme = testVatScheme.copy(acknowledgementReference = Some(testAckReference))
      val expectedJson: JsValue = Json.parse(
        s"""
           |{
           |  "status":"draft",
           |  "ackRef":"BRPY000000000001",
           |  "cancelURL":"/test/uri"
           |}
          """.stripMargin)

      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(Some(vatSchemeWithAckRefNum)))
      await(service.getStatus(testRegId)) mustBe expectedJson
    }
  }

  "call to clearDownDocument" should {
    "pass" when {
      "given a transactionid" in new Setup {
        when(mockRegistrationMongoRepository.clearDownDocument(ArgumentMatchers.eq("testTransID"))).thenReturn(Future.successful(true))
        await(service.clearDownDocument("testTransID")) mustBe true
      }
    }
  }

  "call to getThresholds" should {
    val thresholdPreviousThirtyDays = LocalDate.of(2017, 5, 23)
    val thresholdInTwelveMonths = LocalDate.of(2017, 7, 16)

    "return nothing if nothing in EligibilityData" in new Setup {
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any())).thenReturn(Future.successful(None))

      await(service.getThreshold("regId")) mustBe None
    }

    "return correct Threshold model" in new Setup {
      val eligibilitySubmissionData: EligibilitySubmissionData = EligibilitySubmissionData(
        threshold = Threshold(
          mandatoryRegistration = true,
          thresholdInTwelveMonths = Some(thresholdInTwelveMonths),
          thresholdNextThirtyDays = None,
          thresholdPreviousThirtyDays = Some(thresholdPreviousThirtyDays)
        ),
        exceptionOrExemption = "0",
        estimates = TurnoverEstimates(123456),
        customerStatus = MTDfB,
        partyType = UkCompany
      )

      val expected: Threshold = Threshold(
        mandatoryRegistration = true,
        thresholdPreviousThirtyDays = Some(thresholdPreviousThirtyDays),
        thresholdInTwelveMonths = Some(thresholdInTwelveMonths),
        thresholdNextThirtyDays = None
      )

      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any())).thenReturn(Future.successful(Some(eligibilitySubmissionData)))

      await(service.getThreshold("regId")) mustBe Some(expected)
    }
  }

  "call to getTurnoverEstimates" should {
    "return nothing if nothing in EligibilityData" in new Setup {
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any())).thenReturn(Future.successful(None))

      await(service.getTurnoverEstimates("regId")) mustBe None
    }

    "return correct TurnoverEstimates model when turnover estimate is provided with a number" in new Setup {
      val eligibilitySubmissionData: EligibilitySubmissionData = EligibilitySubmissionData(
        threshold = Threshold(
          mandatoryRegistration = false
        ),
        exceptionOrExemption = "0",
        estimates = TurnoverEstimates(10001),
        customerStatus = MTDfB,
        partyType = UkCompany
      )

      val expected: TurnoverEstimates = TurnoverEstimates(turnoverEstimate = 10001)

      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any())).thenReturn(Future.successful(Some(eligibilitySubmissionData)))

      await(service.getTurnoverEstimates("regId")) mustBe Some(expected)
    }
  }

  "call to store Honesty Declaration status" should {
    "return value being stored" in new Setup {
      when(mockRegistrationMongoRepository.storeHonestyDeclaration("regId", honestyDeclarationData = true)).thenReturn(Future(true))

      await(service.storeHonestyDeclaration("regId", honestyDeclarationStatus = true)) mustBe true
    }
  }
}
