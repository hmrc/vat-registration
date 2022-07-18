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
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class VatRegistrationServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    lazy val service: VatRegistrationService = new VatRegistrationService(mockRegistrationMongoRepository, backendConfig, mockHttpClient)
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

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

  "call to store Honesty Declaration status" should {
    "return value being stored" in new Setup {
      when(mockRegistrationMongoRepository.storeHonestyDeclaration("regId", honestyDeclarationData = true)).thenReturn(Future(true))

      await(service.storeHonestyDeclaration("regId", honestyDeclarationStatus = true)) mustBe true
    }
  }
}
