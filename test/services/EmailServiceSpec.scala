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

import connectors.{EmailFailedToSend, EmailSent}
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.{MockAttachmentsService, MockEmailConnector, MockVatSchemeRepository}
import models.api.{Attachments, EmailMethod}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{GatewayTimeoutException, HeaderCarrier}

import scala.concurrent.Future

class EmailServiceSpec extends VatRegSpec with VatRegistrationFixture
  with MockVatSchemeRepository
  with MockAttachmentsService
  with MockEmailConnector {

  implicit val hc = HeaderCarrier()

  val testEmailVatScheme = testVatScheme.copy(
    applicantDetails = Some(validApplicantDetails),
    transactorDetails = Some(validTransactorDetails),
    attachments = Some(Attachments(Some(EmailMethod))),
    acknowledgementReference = Some(testAckReference)
  )

  val basicTemplate = "mtdfb_vatreg_registration_received"
  val emailTemplate = s"${basicTemplate}_email"
  val postalTemplate = s"${basicTemplate}_post"
  val basicCyTemplate = s"${basicTemplate}_cy"
  val emailCyTemplate = s"${emailTemplate}_cy"
  val postalCyTemplate = s"${postalTemplate}_cy"
  val testEmailFirstName = "Forename"
  val testApplicantEmail = "skylake@vilikariet.com"

  val testParams = Map(
    "name" -> testEmailFirstName,
    "ref" -> testAckReference
  )

  object TestService extends EmailService(mockEmailConnector, mockVatSchemeRepository)

  "the email service" when {
    "the VAT scheme exists for the user" when {
      "an applicant name/email exists" when {
        "a transactor name/email exists" when {
          "the email sends successfully" must {
            "send the Submission Received email to the transactor's email and use the transactor name as the salutation" in {
              mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testEmailVatScheme)))
              mockSendSubmissionReceivedEmail(testEmail, emailTemplate, testParams, force = true)(Future.successful(EmailSent))

              val res = await(TestService.sendRegistrationReceivedEmail(testInternalId, testRegId, "en"))

              res mustBe EmailSent
            }
          }
          "the welsh email sends successfully" must {
            "send the Submission Received email to the transactor's email and use the transactor name as the salutation" in {
              mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testEmailVatScheme)))
              mockSendSubmissionReceivedEmail(testEmail, emailCyTemplate, testParams, force = true)(Future.successful(EmailSent))

              val res = await(TestService.sendRegistrationReceivedEmail(testInternalId, testRegId, "cy"))

              res mustBe EmailSent
            }
          }
          "the email fails to send" must {
            "return EmailFailedToSend" in {
              mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testEmailVatScheme)))
              mockSendSubmissionReceivedEmail(testEmail, emailTemplate, testParams, force = true)(Future.successful(EmailFailedToSend))

              val res = await(TestService.sendRegistrationReceivedEmail(testInternalId, testRegId, "en"))

              res mustBe EmailFailedToSend
            }
          }
          "the connection to the email service times out" must {
            "return EmailFailedToSend" in {
              mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(testEmailVatScheme)))
              mockSendSubmissionReceivedEmail(testEmail, emailTemplate, testParams, force = true)(Future.failed(new GatewayTimeoutException("")))

              val res = await(TestService.sendRegistrationReceivedEmail(testInternalId, testRegId, "en"))

              res mustBe EmailFailedToSend
            }
          }
        }
        "a transactor name/email doesn't exist" when {
          "the email sends successfully" must {
            "send the Submission Received email to the applicant's email and use the applicant name as the salutation" in {
              val vatScheme = testEmailVatScheme.copy(transactorDetails = None)
              mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(vatScheme)))
              mockSendSubmissionReceivedEmail(testApplicantEmail, emailTemplate, testParams, force = true)(Future.successful(EmailSent))

              val res = await(TestService.sendRegistrationReceivedEmail(testInternalId, testRegId, "en"))

              res mustBe EmailSent
            }
          }
          "the welsh email sends successfully" must {
            "send the Submission Received email to the applicant's email and use the applicant name as the salutation" in {
              val vatScheme = testEmailVatScheme.copy(transactorDetails = None)
              mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(vatScheme)))
              mockSendSubmissionReceivedEmail(testApplicantEmail, emailCyTemplate, testParams, force = true)(Future.successful(EmailSent))

              val res = await(TestService.sendRegistrationReceivedEmail(testInternalId, testRegId, "cy"))

              res mustBe EmailSent
            }
          }
          "the email fails to send" must {
            "return EmailFailedToSend" in {
              val vatScheme = testEmailVatScheme.copy(transactorDetails = None)
              mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(vatScheme)))
              mockSendSubmissionReceivedEmail(testApplicantEmail, emailTemplate, testParams, force = true)(Future.successful(EmailFailedToSend))

              val res = await(TestService.sendRegistrationReceivedEmail(testInternalId, testRegId, "en"))

              res mustBe EmailFailedToSend
            }
          }
        }
      }
      "the applicant name doesn't exist (invalid state)" must {
        "return EmailFailedToSend" in {
          val vatScheme = testEmailVatScheme.copy(applicantDetails = None, transactorDetails = None)

          mockGetRegistration(testInternalId, testRegId)(Future.successful(Some(vatScheme)))

          val res = await(TestService.sendRegistrationReceivedEmail(testInternalId, testRegId, "en"))

          res mustBe EmailFailedToSend
        }
      }
    }
    "the VAT scheme doesn't exist (invalid state)" must {
      "return EmailFailedToSend" in {
        mockGetRegistration(testInternalId, testRegId)(Future.successful(None))

        val res = await(TestService.sendRegistrationReceivedEmail(testInternalId, testRegId, "en"))

        res mustBe EmailFailedToSend
      }
    }
  }

}
