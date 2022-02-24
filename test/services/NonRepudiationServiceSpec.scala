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

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.monitoring.MockAuditService
import models.nonrepudiation.NonRepudiationAuditing.{NonRepudiationSubmissionFailureAudit, NonRepudiationSubmissionSuccessAudit}
import models.nonrepudiation.{NonRepudiationMetadata, NonRepudiationSubmissionAccepted, NonRepudiationSubmissionFailed}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.http.{Authorization, HeaderCarrier}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import scala.concurrent.Future

class NonRepudiationServiceSpec extends VatRegSpec with MockAuditService with VatRegistrationFixture with Eventually {

  import AuthTestData._

  object TestService extends NonRepudiationService(
    mockNonRepudiationConnector,
    mockAuditService,
    mockAuthConnector
  )

  implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization(testAuthToken)))
  implicit val request: Request[AnyContent] = FakeRequest()

  val testFormBundleId = "testFormBundleId"

  "submitNonRepudiation" should {
    "call the nonRepudiationConnector with the correctly formatted metadata" in {
      val testSubmissionId = "testSubmissionId"
      val testPayloadString = "testPayloadString"
      val testRegistrationId = "testRegistrationId"

      val testPayloadChecksum = MessageDigest.getInstance("SHA-256")
        .digest(testPayloadString.getBytes(StandardCharsets.UTF_8))
        .map("%02x".format(_)).mkString

      val testEncodedPayload = Base64.getEncoder.encodeToString(testPayloadString.getBytes(StandardCharsets.UTF_8))


      val expectedMetadata = NonRepudiationMetadata(
        businessId = "vrs",
        notableEvent = "vat-registration",
        payloadContentType = "application/json",
        payloadSha256Checksum = testPayloadChecksum,
        userSubmissionTimestamp = testDateTime,
        identityData = testNonRepudiationIdentityData,
        userAuthToken = testAuthToken,
        headerData = testUserHeaders,
        searchKeys = Map("formBundleId" -> testFormBundleId)
      )

      when(mockNonRepudiationConnector.submitNonRepudiation(
        ArgumentMatchers.eq(testEncodedPayload),
        ArgumentMatchers.eq(expectedMetadata)
      )(ArgumentMatchers.eq(hc)))
        .thenReturn(Future.successful(NonRepudiationSubmissionAccepted(testSubmissionId)))

      when(mockAuthConnector.authorise(
        ArgumentMatchers.eq(EmptyPredicate),
        ArgumentMatchers.eq(NonRepudiationService.nonRepudiationIdentityRetrievals
        ))(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(executionContext))
      ).thenReturn(Future.successful(testAuthRetrievals))

      val res = TestService.submitNonRepudiation(testRegistrationId, testPayloadString, testDateTime, testFormBundleId, testUserHeaders)

      await(res) mustBe Some(testSubmissionId)

      eventually {
        verifyAudit(NonRepudiationSubmissionSuccessAudit(testRegistrationId, testSubmissionId))
      }
    }
    "audit when the non repudiation call fails" in {
      val testPayloadString = "testPayloadString"
      val testRegistrationId = "testRegistrationId"

      val testPayloadChecksum = MessageDigest.getInstance("SHA-256")
        .digest(testPayloadString.getBytes(StandardCharsets.UTF_8))
        .map("%02x".format(_)).mkString

      val testEncodedPayload = Base64.getEncoder.encodeToString(testPayloadString.getBytes(StandardCharsets.UTF_8))


      val expectedMetadata = NonRepudiationMetadata(
        businessId = "vrs",
        notableEvent = "vat-registration",
        payloadContentType = "application/json",
        payloadSha256Checksum = testPayloadChecksum,
        userSubmissionTimestamp = testDateTime,
        identityData = testNonRepudiationIdentityData,
        userAuthToken = testAuthToken,
        headerData = testUserHeaders,
        searchKeys = Map("formBundleId" -> testFormBundleId)
      )

      val testExceptionMessage = "testExceptionMessage"

      when(mockAuthConnector.authorise(
        ArgumentMatchers.eq(EmptyPredicate),
        ArgumentMatchers.eq(NonRepudiationService.nonRepudiationIdentityRetrievals
        ))(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(executionContext))
      ).thenReturn(Future.successful(testAuthRetrievals))

      when(mockNonRepudiationConnector.submitNonRepudiation(
        ArgumentMatchers.eq(testEncodedPayload),
        ArgumentMatchers.eq(expectedMetadata)
      )(ArgumentMatchers.eq(hc)))
        .thenReturn(Future.successful(NonRepudiationSubmissionFailed(testExceptionMessage, NOT_FOUND)))

      val res = TestService.submitNonRepudiation(testRegistrationId, testPayloadString, testDateTime, testFormBundleId, testUserHeaders)

      await(res) mustBe None

      eventually {
        verifyAudit(NonRepudiationSubmissionFailureAudit(testRegistrationId, NOT_FOUND, testExceptionMessage))
      }
    }
  }
}
