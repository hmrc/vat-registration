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

import config.BackendConfig
import enums.VatRegStatus
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.MockVatSchemeRepository
import models.api.{ApplicantDetails, Contact, TransactorDetails, VatScheme}
import org.mockito.Mockito._
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import scala.concurrent.Future

class VatRegistrationServiceSpec extends VatRegSpec with VatRegistrationFixture with MockVatSchemeRepository with LogCapturing {

  private val mockAppConfig                = mock[BackendConfig]
  private implicit val request: Request[_] = FakeRequest()

  class Setup {
    lazy val service: VatRegistrationService = new VatRegistrationService(mockVatSchemeRepository, mockAppConfig)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockVatSchemeRepository)
  }

  private def buildVatScheme(applicantDetails: Option[ApplicantDetails], transactorDetails: Option[TransactorDetails]): VatScheme =
    testVatScheme.copy(applicantDetails = applicantDetails, transactorDetails = transactorDetails)

  when(mockAppConfig.emailCheck).thenReturn(List("@badDomain.com"))

  "checkForRisks" should {
    "return a Right" when {
      "all four details exist and their checks pass" in new Setup {
        private val safeEmail =
          "example@safeDomain.com"
        private val safeApplicantDetails = validApplicantDetails.copy(
          personalDetails = Some(testPersonalDetails.copy(score = Some(service.unsafeScore - 1))),
          contact = Contact(Some(safeEmail)))
        private val safeTransactorDetails = validTransactorDetails.copy(
          personalDetails = Some(testPersonalDetails.copy(score = Some(service.unsafeScore + 1))),
          email = Some(safeEmail))

        private val safeDetails =
          buildVatScheme(applicantDetails = Some(safeApplicantDetails), transactorDetails = Some(safeTransactorDetails))

        service.checkForRisks(safeDetails) mustBe Right(())
      }

      "the relevant details are empty" in new Setup {
        private val emptyDetails =
          buildVatScheme(applicantDetails = None, transactorDetails = None)

        service.checkForRisks(emptyDetails) mustBe Right(())
      }
    }

    "return a Left with a list of descriptions for any failed checks" in new Setup {
      private val badEmail =
        "example@badDomain.com"
      private val unsafePersonalDetails =
        testPersonalDetails.copy(score = Some(service.unsafeScore))
      private val unsafeApplicantDetails =
        validApplicantDetails.copy(personalDetails = Some(unsafePersonalDetails), contact = Contact(Some(badEmail)))
      private val unsafeTransactorDetails =
        validTransactorDetails.copy(personalDetails = Some(unsafePersonalDetails), email = Some(badEmail))

      private val allFourBadDetails =
        buildVatScheme(applicantDetails = Some(unsafeApplicantDetails), transactorDetails = Some(unsafeTransactorDetails))

      service.checkForRisks(allFourBadDetails) mustBe Left(
        Seq(
          "Applicant personal details score was 100",
          "Transactor personal details score was 100",
          "Applicant email (example@badDomain.com) has an unaccepted domain",
          "Transactor email (example@badDomain.com) has an unaccepted domain"
        ))
    }
  }

  "getStatus" should {
    "return the registration's status" when {
      "data is fetched from the repository and none of the checks fail" in new Setup {
        private val status = VatRegStatus.submitted
        private val safeSubmittedVatScheme = testVatScheme.copy(
          status = status,
          applicantDetails = None,
          transactorDetails = None
        )

        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(safeSubmittedVatScheme)))

        await(service.getStatus(testInternalId, testRegId)) mustBe status
      }
    }

    "return a 'contact' status and log the failure(s)" when {
      "one check fails" in new Setup {
        private val badEmail                = "example@badDomain.com"
        private val unsafeTransactorDetails = validTransactorDetails.copy(email = Some(badEmail))
        private val unsafeSubmittedVatScheme =
          testVatScheme.copy(status = VatRegStatus.submitted, transactorDetails = Some(unsafeTransactorDetails))

        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(unsafeSubmittedVatScheme)))

        withCaptureOfLoggingFrom(service) { logs =>
          await(service.getStatus(testInternalId, testRegId)) mustBe VatRegStatus.contact

          logs.exists(_.getMessage.contains("Transactor email (example@badDomain.com) has an unaccepted domain")) mustBe true
        }
      }

      "multiple checks fail" in new Setup {
        private val badEmail              = "example@badDomain.com"
        private val unsafePersonalDetails = testPersonalDetails.copy(score = Some(service.unsafeScore))
        private val unsafeTransactorDetails =
          validTransactorDetails.copy(personalDetails = Some(unsafePersonalDetails), email = Some(badEmail))
        private val unsafeSubmittedVatScheme =
          testVatScheme.copy(status = VatRegStatus.submitted, transactorDetails = Some(unsafeTransactorDetails))

        mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(unsafeSubmittedVatScheme)))

        withCaptureOfLoggingFrom(service) { logs =>
          await(service.getStatus(testInternalId, testRegId)) mustBe VatRegStatus.contact

          logs.exists(
            _.getMessage.contains(
              "- Transactor personal details score was 100,\n" +
                "- Transactor email (example@badDomain.com) has an unaccepted domain"
            )) mustBe true
        }
      }
    }

    "throw an InternalServerException and log the failure" when {
      "the repository returns no data" in new Setup {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(None))

        withCaptureOfLoggingFrom(service) { logs =>
          intercept[InternalServerException](await(service.getStatus(testInternalId, testRegId)))

          logs.exists(_.getMessage.contains(s"- No VAT registration document found for $testRegId")) mustBe true
        }
      }
    }
  }

}
