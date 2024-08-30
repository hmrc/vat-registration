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

package services.submission

import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import connectors.EmailSent
import enums.VatRegStatus
import featureswitch.core.config.{FeatureSwitching, PostSubmissionDecoupling, PostSubmissionDecouplingConnector, PostSubmissionNonDecoupling}
import fixtures.{SubmissionAuditFixture, VatSubmissionFixture}
import helpers.VatRegSpec
import httpparsers.{VatSubmissionFailure, VatSubmissionSuccess}
import mocks.monitoring.MockAuditService
import mocks._
import models.api._
import models.api.schemas.API1364
import models.monitoring.SubmissionAuditModel
import models.nonrepudiation.NonRepudiationAttachmentAccepted
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Seconds, Span}
import play.api.libs.json.JsObject
import play.api.mvc.{AnyContentAsEmpty, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.monitoring.buildermocks.MockSubmissionAuditBlockBuilder
import services.submission.buildermocks.MockSubmissionPayloadBuilder
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier}
import utils.IdGenerator

import scala.concurrent.Future

class SubmissionServiceSpec
  extends VatRegSpec
    with SubmissionAuditFixture
    with VatSubmissionFixture
    with ApplicativeSyntax
    with FutureInstances
    with MockAuditService
    with Eventually
    with MockAttachmentsService
    with MockUpscanService
    with MockSubmissionPayloadBuilder
    with MockSubmissionAuditBlockBuilder
    with MockSchemaValidationService
    with MockEmailService
    with FeatureSwitching
    with MockSdesService {

  val apiSchema = app.injector.instanceOf[API1364]

  class Setup {

    object TestIdGenerator extends IdGenerator {
      override def createId: String = "TestCorrelationId"
    }

    val service: SubmissionService = new SubmissionService(
      registrationRepository = mockRegistrationMongoRepository,
      vatSubmissionConnector = mockVatSubmissionConnector,
      nonRepudiationService = mockNonRepudiationService,
      submissionPayloadBuilder = mockSubmissionPayloadBuilder,
      submissionAuditBlockBuilder = mockSubmissionAuditBlockBuilder,
      attachmentsService = mockAttachmentService,
      upscanService = mockUpscanService,
      nonRepudiationConnector = mockNonRepudiationConnector,
      sdesService = mockSdesService,
      idGenerator = TestIdGenerator,
      auditService = mockAuditService,
      timeMachine = mockTimeMachine,
      emailService = mockEmailService,
      schemaValidationService = mockSchemaValidationService,
      apiSchema = apiSchema,
      authConnector = mockAuthConnector
    )
  }

  val testCorrelationId = "testCorrelationId"
  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", "testUrl")

  "submitVatRegistration" when {
    "successfully submit and return an acknowledgment reference and send attachments to NRS" in new Setup {
      enable(PostSubmissionDecoupling)
      enable(PostSubmissionDecouplingConnector)
      disable(PostSubmissionNonDecoupling)
      when(mockRegistrationMongoRepository.getRegistration(anyString(), anyString()))
        .thenReturn(Future.successful(Some(testFullVatScheme)))
      when(
        mockRegistrationMongoRepository.updateSubmissionStatus(anyString(), anyString(), any[VatRegStatus.Value]())(
          ArgumentMatchers.any[Request[_]]
        )
      )
        .thenReturn(Future.successful(Some(VatRegStatus.submitted)))
      when(mockVatSubmissionConnector.submit(any[JsObject], anyString(), anyString())(any()))
        .thenReturn(Future.successful(Right(VatSubmissionSuccess(testFormBundleId))))
      when(mockRegistrationMongoRepository.finishRegistrationSubmission(anyString(), any(), any()))
        .thenReturn(Future.successful(VatRegStatus.submitted))
      mockBuildAuditJson(testFullVatScheme, testProviderId, Organisation, None, testFormBundleId)(
        SubmissionAuditModel(
          detailBlockAnswers,
          testFullVatScheme,
          testProviderId,
          Organisation,
          None,
          testFormBundleId
        )
      )
      when(mockTimeMachine.timestamp).thenReturn(testDateTime)
      when(mockSubmissionPayloadBuilder.buildSubmissionPayload(testFullVatScheme))
        .thenReturn(vatSubmissionVoluntaryJson.as[JsObject])
      mockSendRegistrationReceivedEmail(testInternalId, testRegId, "en")(Future.successful(EmailSent))

      val testNonRepudiationSubmissionId = "testNonRepudiationSubmissionId"

      when(
        mockNonRepudiationService.submitNonRepudiation(
          ArgumentMatchers.eq(testRegId),
          ArgumentMatchers.eq(testSubmissionPayload),
          ArgumentMatchers.eq(testDateTime),
          ArgumentMatchers.eq(testFormBundleId),
          ArgumentMatchers.eq(testUserHeaders),
          ArgumentMatchers.any[Boolean]
        )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(request))
      ).thenReturn(Future.successful(Some(testNonRepudiationSubmissionId)))

      when(
        mockUpscanService.getAllUpscanDetails(ArgumentMatchers.eq(testRegId))(ArgumentMatchers.eq(request))
      ).thenReturn(Future.successful(Seq(testUpscanDetails)))

      when(
        mockNonRepudiationConnector.submitAttachmentNonRepudiation(
          ArgumentMatchers.eq(testNonRepudiationAttachment)
        )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(request))
      ).thenReturn(Future.successful(NonRepudiationAttachmentAccepted(testNrAttachmentId)))

      mockAuthorise(Retrievals.credentials and Retrievals.affinityGroup and Retrievals.agentCode)(
        Future.successful(
          Some(testCredentials) ~ Some(testAffinityGroup) ~ None
        )
      )
      mockAuthorise(Retrievals.credentials)(
        Future.successful(
          Some(testCredentials)
        )
      )
      mockAttachmentList(testFullVatScheme)(List[AttachmentType](VAT51))
      mockOptionalAttachmentList(testFullVatScheme)(List[AttachmentType]())

      await(service.submitVatRegistration(testInternalId, testRegId, testUserHeaders, "en")) mustBe Right(
        VatSubmissionSuccess(testFormBundleId)
      )

      eventually(timeout(Span(5, Seconds))) {
        verify(mockNonRepudiationService, atLeastOnce()).submitNonRepudiation(
          ArgumentMatchers.eq(testRegId),
          ArgumentMatchers.eq(testSubmissionPayload),
          ArgumentMatchers.eq(testDateTime),
          ArgumentMatchers.eq(testFormBundleId),
          ArgumentMatchers.eq(testUserHeaders),
          ArgumentMatchers.any[Boolean]
        )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(request))
        verify(mockUpscanService).getAllUpscanDetails(ArgumentMatchers.eq(testRegId))(ArgumentMatchers.eq(request))
        verify(mockNonRepudiationConnector).submitAttachmentNonRepudiation(ArgumentMatchers.eq(testNonRepudiationAttachment))(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(request))
        verifyAudit(
            SubmissionAuditModel(
              detailBlockAnswers,
              testFullVatScheme,
              testProviderId,
              testAffinityGroup,
              None,
              testFormBundleId
          )
        )
      }
    }

    "successfully submit and return an acknowledgment reference" in new Setup {
      disable(PostSubmissionDecoupling)
      enable(PostSubmissionNonDecoupling)
      when(mockRegistrationMongoRepository.getRegistration(anyString(), anyString()))
        .thenReturn(Future.successful(Some(testFullVatScheme)))
      when(
        mockRegistrationMongoRepository.updateSubmissionStatus(anyString(), anyString(), any[VatRegStatus.Value]())(
          ArgumentMatchers.any[Request[_]]
        )
      )
        .thenReturn(Future.successful(Some(VatRegStatus.submitted)))
      when(mockVatSubmissionConnector.submit(any[JsObject], anyString(), anyString())(any()))
        .thenReturn(Future.successful(Right(VatSubmissionSuccess(testFormBundleId))))
      when(mockRegistrationMongoRepository.finishRegistrationSubmission(anyString(), any(), any()))
        .thenReturn(Future.successful(VatRegStatus.submitted))
      mockBuildAuditJson(testFullVatScheme, testProviderId, Organisation, None, testFormBundleId)(
        SubmissionAuditModel(
          detailBlockAnswers,
          testFullVatScheme,
          testProviderId,
          Organisation,
          None,
          testFormBundleId
        )
      )
      when(mockTimeMachine.timestamp).thenReturn(testDateTime)
      when(mockSubmissionPayloadBuilder.buildSubmissionPayload(testFullVatScheme))
        .thenReturn(vatSubmissionVoluntaryJson.as[JsObject])
      mockSendRegistrationReceivedEmail(testInternalId, testRegId, "en")(Future.successful(EmailSent))

      val testNonRepudiationSubmissionId = "testNonRepudiationSubmissionId"

      when(
        mockNonRepudiationService.submitNonRepudiation(
          ArgumentMatchers.eq(testRegId),
          ArgumentMatchers.eq(testSubmissionPayload),
          ArgumentMatchers.eq(testDateTime),
          ArgumentMatchers.eq(testFormBundleId),
          ArgumentMatchers.eq(testUserHeaders),
          ArgumentMatchers.any[Boolean]
        )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(request))
      ).thenReturn(Future.successful(Some(testNonRepudiationSubmissionId)))

      mockAuthorise(Retrievals.credentials and Retrievals.affinityGroup and Retrievals.agentCode)(
        Future.successful(
          Some(testCredentials) ~ Some(testAffinityGroup) ~ None
        )
      )
      mockAuthorise(Retrievals.credentials)(
        Future.successful(
          Some(testCredentials)
        )
      )
      mockAttachmentList(testFullVatScheme)(List[AttachmentType]())
      mockOptionalAttachmentList(testFullVatScheme)(List[AttachmentType]())

      await(service.submitVatRegistration(testInternalId, testRegId, testUserHeaders, "en")) mustBe Right(
        VatSubmissionSuccess(testFormBundleId)
      )

      eventually(timeout(Span(5, Seconds))) {
        verify(mockNonRepudiationService, atLeastOnce()).submitNonRepudiation(
          ArgumentMatchers.eq(testRegId),
          ArgumentMatchers.eq(testSubmissionPayload),
          ArgumentMatchers.eq(testDateTime),
          ArgumentMatchers.eq(testFormBundleId),
          ArgumentMatchers.eq(testUserHeaders),
          ArgumentMatchers.any[Boolean]
        )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(request))
        verifyNoInteractions(mockNonRepudiationConnector)
        verifyAudit(
          SubmissionAuditModel(
            detailBlockAnswers,
            testFullVatScheme,
            testProviderId,
            testAffinityGroup,
            None,
            testFormBundleId
          )
        )
      }
    }

    "fail the submission on BAD_REQUEST and validate the failures" in new Setup {
      when(mockRegistrationMongoRepository.getRegistration(anyString(), anyString()))
        .thenReturn(Future.successful(Some(testFullVatScheme)))
      when(
        mockRegistrationMongoRepository.updateSubmissionStatus(anyString(), anyString(), any[VatRegStatus.Value]())(
          ArgumentMatchers.any[Request[_]]
        )
      )
        .thenReturn(Future.successful(Some(VatRegStatus.submitted)))
      when(mockVatSubmissionConnector.submit(any[JsObject], anyString(), anyString())(any()))
        .thenReturn(Future.successful(Left(VatSubmissionFailure(BAD_REQUEST, ""))))
      when(mockRegistrationMongoRepository.finishRegistrationSubmission(anyString(), any(), any()))
        .thenReturn(Future.successful(VatRegStatus.failed))
      when(mockSubmissionPayloadBuilder.buildSubmissionPayload(testFullVatScheme))
        .thenReturn(vatSubmissionVoluntaryJson.as[JsObject])
      mockValidate(vatSubmissionVoluntaryJson.toString())(Map("suppressedErrors" -> List("/suppressed/error")))
      mockAuthorise(Retrievals.credentials)(Future.successful(Some(testCredentials)))
      mockAttachmentList(testFullVatScheme)(List[AttachmentType]())
      mockOptionalAttachmentList(testFullVatScheme)(List[AttachmentType]())

      await(service.submitVatRegistration(testInternalId, testRegId, testUserHeaders, "en")) mustBe Left(
        VatSubmissionFailure(BAD_REQUEST, "")
      )
    }

    "throw a bad request exception BAD_REQUEST if the schema validator returns an unknown error" in new Setup {
      when(mockRegistrationMongoRepository.getRegistration(anyString(), anyString()))
        .thenReturn(Future.successful(Some(testFullVatScheme)))
      when(
        mockRegistrationMongoRepository.updateSubmissionStatus(anyString(), anyString(), any[VatRegStatus.Value]())(
          ArgumentMatchers.any[Request[_]]
        )
      )
        .thenReturn(Future.successful(Some(VatRegStatus.submitted)))
      when(mockVatSubmissionConnector.submit(any[JsObject], anyString(), anyString())(any()))
        .thenReturn(Future.successful(Left(VatSubmissionFailure(BAD_REQUEST, ""))))
      when(mockRegistrationMongoRepository.finishRegistrationSubmission(anyString(), any(), any()))
        .thenReturn(Future.successful(VatRegStatus.failed))
      when(mockSubmissionPayloadBuilder.buildSubmissionPayload(testFullVatScheme))
        .thenReturn(vatSubmissionVoluntaryJson.as[JsObject])
      mockValidate(vatSubmissionVoluntaryJson.toString())(Map("unknownErrors" -> List("/new/error")))
      mockAuthorise(Retrievals.credentials)(Future.successful(Some(testCredentials)))
      mockAttachmentList(testFullVatScheme)(List[AttachmentType]())
      mockOptionalAttachmentList(testFullVatScheme)(List[AttachmentType]())

      intercept[BadRequestException] {
        await(service.submitVatRegistration(testInternalId, testRegId, testUserHeaders, "en"))
      }
    }
  }

  "submit" should {
    "return an OK response and successfully audit when all calls succeed" in new Setup {
      when(mockVatSubmissionConnector.submit(any[JsObject], anyString(), anyString())(any()))
        .thenReturn(Future.successful(Right(VatSubmissionSuccess(testFormBundleId))))
      mockAuthorise(Retrievals.credentials)(
        Future.successful(
          Some(testCredentials)
        )
      )
      mockBuildAuditJson(testFullVatScheme, testProviderId, Organisation, None, testFormBundleId)(
        SubmissionAuditModel(
          detailBlockAnswers,
          testFullVatScheme,
          testProviderId,
          Organisation,
          None,
          testFormBundleId
        )
      )

      await(service.submit(vatSubmissionJson.as[JsObject], testRegId, testCorrelationId)) mustBe Right(
        VatSubmissionSuccess(testFormBundleId)
      )
    }

    "return a 502 response and successfully audit when submission fails with a 502" in new Setup {
      when(mockVatSubmissionConnector.submit(any[JsObject], anyString(), anyString())(any()))
        .thenReturn(Future.successful(Right(VatSubmissionSuccess(testFormBundleId))))
      mockAuthorise(Retrievals.credentials)(
        Future.successful(
          Some(testCredentials)
        )
      )
      mockBuildAuditJson(testFullVatScheme, testProviderId, Organisation, None, testFormBundleId)(
        SubmissionAuditModel(
          detailBlockAnswers,
          testFullVatScheme,
          testProviderId,
          Organisation,
          None,
          testFormBundleId
        )
      )

      await(service.submit(vatSubmissionJson.as[JsObject], testRegId, testCorrelationId)) mustBe Right(
        VatSubmissionSuccess(testFormBundleId)
      )
    }
  }

}
