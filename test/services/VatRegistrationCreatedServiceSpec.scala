/*
 * Copyright 2022 HM Revenue & Customs
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
import models.api._
import models.submission.UkCompany
import models.{ForwardLook, Voluntary}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
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

    val vatScheme = VatScheme(testRegId, testInternalId, None, None, None, status = VatRegStatus.draft, eligibilitySubmissionData = Some(testEligibilitySubmissionData))

    "return a existing VatScheme response " in new Setup {

      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(Some(vatScheme)))

      service.createNewRegistration(testInternalId) returnsRight vatScheme
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
      when(mockRegistrationMongoRepository.createNewVatScheme(testRegId, testInternalId)).thenReturn(Future.successful(vatScheme))

      await(service.createNewRegistration(testInternalId).value) mustBe Right(vatScheme)
    }

    "error when creating VatScheme" in new Setup {
      val t = new Exception("Exception")

      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(None))
      when(mockRegistrationMongoRepository.createNewVatScheme(testRegId, testInternalId)).thenReturn(Future.failed(t))

      service.createNewRegistration(testInternalId) returnsLeft GenericError(t)
    }

    "error with the DB when creating VatScheme" in new Setup {
      val error: InsertFailed = InsertFailed("regId", "VatScheme")

      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(None))
      when(mockRegistrationMongoRepository.createNewVatScheme(testRegId, testInternalId)).thenReturn(Future.failed(error))

      service.createNewRegistration(testInternalId) returnsLeft GenericDatabaseError(error, Some("regId"))
    }

    "call to business service return ForbiddenException response " in new Setup {
      service.createNewRegistration(testInternalId) returnsLeft ForbiddenAccess("forbidden")
    }

    "call to business service return NotFoundException response " in new Setup {
      service.createNewRegistration(testInternalId) returnsLeft ResourceNotFound("notfound")
    }

    "call to business service return ErrorResponse response " in new Setup {
      val t = new RuntimeException("Exception")

      service.createNewRegistration(testInternalId) returnsLeft GenericError(t)
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
    "return a correct status" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(Some(testVatScheme)))

      await(service.getStatus(testRegId)) mustBe VatRegStatus.draft
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
        partyType = UkCompany,
        registrationReason = Voluntary,
        isTransactor = false
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
