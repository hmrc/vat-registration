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

import enums.VatRegStatus
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class VatRegistrationServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    lazy val service: VatRegistrationService = new VatRegistrationService(mockRegistrationMongoRepository, backendConfig, mockHttpClient)
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "call to getStatus" should {
    "return a correct status" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(Some(testVatScheme)))

      await(service.getStatus(testRegId)) mustBe VatRegStatus.draft
    }

    "return correct status when if applicant details cannot be processed" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(Some(
        testVatScheme.copy(applicantDetails = Some(validApplicantDetails.copy(personalDetails = testPersonalDetails)))
      )))

      await(service.getStatus(testRegId)) mustBe VatRegStatus.contact
      verify(mockRegistrationMongoRepository).updateSubmissionStatus(testRegId, VatRegStatus.contact)
    }

    "return correct status when if transactor details cannot be processed" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(Some(
        testVatScheme.copy(transactorDetails = Some(validTransactorDetails.copy(personalDetails = testPersonalDetails)))
      )))

      await(service.getStatus(testRegId)) mustBe VatRegStatus.contact
      verify(mockRegistrationMongoRepository).updateSubmissionStatus(testRegId, VatRegStatus.contact)
    }

    "return correct status when if either transactor/applicant details cannot be processed" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(testRegId)).thenReturn(Future.successful(Some(
        testVatScheme.copy(
          transactorDetails = Some(validTransactorDetails.copy(personalDetails = testPersonalDetails)),
          applicantDetails = Some(validApplicantDetails.copy(personalDetails = testPersonalDetails.copy(score = Some(0))))
        )
      )))

      await(service.getStatus(testRegId)) mustBe VatRegStatus.contact
      verify(mockRegistrationMongoRepository).updateSubmissionStatus(testRegId, VatRegStatus.contact)
    }
  }

}
