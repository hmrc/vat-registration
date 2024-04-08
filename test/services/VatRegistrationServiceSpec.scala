/*
 * Copyright 2023 HM Revenue & Customs
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
import mocks.MockVatSchemeRepository
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import play.api.mvc.Request
import play.api.test.FakeRequest
import scala.concurrent.Future

class VatRegistrationServiceSpec extends VatRegSpec with VatRegistrationFixture with MockVatSchemeRepository {

  class Setup {
    lazy val service: VatRegistrationService = new VatRegistrationService(mockVatSchemeRepository, backendConfig)
  }

  implicit val hc: HeaderCarrier   = HeaderCarrier()
  implicit val request: Request[_] = FakeRequest()

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockVatSchemeRepository)
  }

  "call to getStatus" should {
    "return a correct status" in new Setup {
      mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testVatScheme)))

      await(service.getStatus(testInternalId, testRegId)) mustBe VatRegStatus.draft
    }

    "return correct status when if applicant details cannot be processed" in new Setup {
      mockGetRegistration(testInternalId, testRegId)(
        Future.successful(
          Some(
            testVatScheme.copy(applicantDetails =
              Some(validApplicantDetails.copy(personalDetails = Some(testPersonalDetails)))
            )
          )
        )
      )

      await(service.getStatus(testInternalId, testRegId)) mustBe VatRegStatus.contact
      verify(mockVatSchemeRepository).updateSubmissionStatus(testInternalId, testRegId, VatRegStatus.contact)
    }

    "return correct status when if transactor details cannot be processed" in new Setup {
      mockGetRegistration(testInternalId, testRegId)(
        Future.successful(
          Some(
            testVatScheme.copy(transactorDetails =
              Some(validTransactorDetails.copy(personalDetails = Some(testPersonalDetails)))
            )
          )
        )
      )

      await(service.getStatus(testInternalId, testRegId)) mustBe VatRegStatus.contact
      verify(mockVatSchemeRepository).updateSubmissionStatus(testInternalId, testRegId, VatRegStatus.contact)
    }

    "return correct status when if either transactor/applicant details cannot be processed" in new Setup {
      mockGetRegistration(testInternalId, testRegId)(
        Future.successful(
          Some(
            testVatScheme.copy(
              transactorDetails = Some(validTransactorDetails.copy(personalDetails = Some(testPersonalDetails))),
              applicantDetails =
                Some(validApplicantDetails.copy(personalDetails = Some(testPersonalDetails.copy(score = Some(0)))))
            )
          )
        )
      )

      await(service.getStatus(testInternalId, testRegId)) mustBe VatRegStatus.contact
      verify(mockVatSchemeRepository).updateSubmissionStatus(testInternalId, testRegId, VatRegStatus.contact)
    }

    "return correct status when transactor email cannot be processed" in new Setup {
      mockGetRegistration(testInternalId, testRegId)(
        Future.successful(
          Some(
            testVatScheme.copy(
              transactorDetails = Some(validTransactorDetails.copy(email = Some("email@fake.contact.me")))
            )
          )
        )
      )

      await(service.getStatus(testInternalId, testRegId)) mustBe VatRegStatus.contact
      verify(mockVatSchemeRepository).updateSubmissionStatus(testInternalId, testRegId, VatRegStatus.contact)
    }

    "return correct status when applicant email cannot be processed" in new Setup {
      mockGetRegistration(testInternalId, testRegId)(
        Future.successful(
          Some(
            testVatScheme.copy(
              applicantDetails =
                Some(validApplicantDetails.copy(contact = testContact.copy(email = Some("email@fake2.contact.me"))))
            )
          )
        )
      )

      await(service.getStatus(testInternalId, testRegId)) mustBe VatRegStatus.contact
      verify(mockVatSchemeRepository).updateSubmissionStatus(testInternalId, testRegId, VatRegStatus.contact)
    }
  }

}
