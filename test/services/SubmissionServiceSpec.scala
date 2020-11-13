/*
 * Copyright 2020 HM Revenue & Customs
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

import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import common.exceptions._
import enums.VatRegStatus
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.monitoring.MockAuditService
import models.api._
import models.monitoring.RegistrationSubmissionAuditing.RegistrationSubmissionAuditModel
import models.nonrepudiation.NonRepudiationSubmissionAccepted
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.IdGenerator

import scala.concurrent.Future

class SubmissionServiceSpec extends VatRegSpec with VatRegistrationFixture with ApplicativeSyntax with FutureInstances with MockAuditService with Eventually {

  class Setup {

    object TestIdGenerator extends IdGenerator {
      override def createId: String = "TestCorrelationId"
    }

    val service: SubmissionService = new SubmissionService(
      sequenceMongoRepository = mockSequenceRepository,
      registrationRepository = mockRegistrationMongoRepository,
      vatSubmissionConnector = mockVatSubmissionConnector,
      nonRepudiationService = mockNonRepudiationService,
      idGenerator = TestIdGenerator,
      auditService = mockAuditService,
      timeMachine = mockTimeMachine,
      authConnector = mockAuthConnector
    )
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", "testUrl")

  "submitVatRegistration" should {
    "successfully submit and return an acknowledgment reference" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(testFullVatScheme)))
      when(mockSequenceRepository.getNext(any())(any())).thenReturn(Future.successful(100))
      when(mockRegistrationMongoRepository.prepareRegistrationSubmission(anyString(), any(), any())(any())).thenReturn(Future.successful(true))
      when(mockRegistrationMongoRepository.saveTransId(any(), anyString())(any())).thenReturn(Future.successful("transID"))
      when(mockVatSubmissionConnector.submit(any[VatSubmission], anyString(), anyString())(any())).thenReturn(Future.successful(HttpResponse(200)))
      when(mockRegistrationMongoRepository.finishRegistrationSubmission(anyString(), any())(any())).thenReturn(Future.successful(VatRegStatus.submitted))

      when(mockTimeMachine.timestamp).thenReturn(testDateTime)
      val nonRepudiationPayloadString: String = Json.toJson(VatSubmission.fromVatScheme(testFullVatScheme)).toString()
      val testNonRepudiationSubmissionId = "testNonRepudiationSubmissionId"


      when(mockNonRepudiationService.submitNonRepudiation(
        ArgumentMatchers.eq(testRegId),
        ArgumentMatchers.eq(nonRepudiationPayloadString),
        ArgumentMatchers.eq(testDateTime),
        ArgumentMatchers.eq(testPostcode),
        ArgumentMatchers.eq(testUserHeaders)
      )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(request))).thenReturn(Future.successful(NonRepudiationSubmissionAccepted(testNonRepudiationSubmissionId)))

      mockAuthorise(Retrievals.credentials and Retrievals.affinityGroup and Retrievals.agentCode)(
        Future.successful(
          Some(testCredentials) ~ Some(testAffinityGroup) ~ None
        )
      )

      await(service.submitVatRegistration(testRegId, testUserHeaders)) mustBe "BRVT00000000100"
      eventually {
        verifyAudit(RegistrationSubmissionAuditModel(
          testFullSubmission,
          testRegId,
          testProviderId,
          testAffinityGroup,
          None
        ))
        verify(mockNonRepudiationService).submitNonRepudiation(
          ArgumentMatchers.eq(testRegId),
          ArgumentMatchers.eq(nonRepudiationPayloadString),
          ArgumentMatchers.eq(testDateTime),
          ArgumentMatchers.eq(testPostcode),
          ArgumentMatchers.eq(testUserHeaders)
        )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(request))
      }
    }
  }

  "submit" should {
    "return a 200 response and successfully audit when all calls succeed" in new Setup {
      when(mockVatSubmissionConnector.submit(any[VatSubmission], anyString(), anyString())(any())).thenReturn(Future.successful(HttpResponse(200)))
      mockAuthorise(Retrievals.credentials and Retrievals.affinityGroup and Retrievals.agentCode)(
        Future.successful(
          Some(testCredentials) ~ Some(testAffinityGroup) ~ None
        )
      )

      await(service.submit(testFullSubmission, testRegId, testUserHeaders)).status mustBe OK
      verifyAudit(RegistrationSubmissionAuditModel(
        testFullSubmission,
        testRegId,
        testProviderId,
        AffinityGroup.Organisation,
        None
      ))
    }

    "return a 502 response and successfully audit when submission fails with a 502" in new Setup {
      when(mockVatSubmissionConnector.submit(any[VatSubmission], anyString(), anyString())(any())).thenReturn(Future.successful(HttpResponse(502)))
      mockAuthorise(Retrievals.credentials and Retrievals.affinityGroup and Retrievals.agentCode)(
        Future.successful(
          Some(testCredentials) ~ Some(testAffinityGroup) ~ None
        )
      )

      await(service.submit(testFullSubmission, testRegId, testUserHeaders)).status mustBe BAD_GATEWAY
      verifyAudit(RegistrationSubmissionAuditModel(
        testFullSubmission,
        testRegId,
        testProviderId,
        AffinityGroup.Organisation,
        None
      ))
    }
  }

  "ensureAcknowledgementReference" should {
    val vatScheme = VatScheme(testRegId, testInternalid, None, None, None, status = VatRegStatus.draft, acknowledgementReference = Some("testref"))
    val sequenceNo = 1
    val formattedRefNumber = f"BRVT$sequenceNo%011d"

    "throw an exception if the document is not available" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      intercept[MissingRegDocument](await(service.ensureAcknowledgementReference(testRegId, VatRegStatus.draft)))
    }

    "get the acknowledgement references if they are available" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme)))

      await(service.ensureAcknowledgementReference(testRegId, VatRegStatus.draft)) mustBe "testref"
    }

    "generate acknowledgment reference if it does not exist" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme.copy(status = VatRegStatus.draft, acknowledgementReference = None))))
      when(mockSequenceRepository.getNext(ArgumentMatchers.eq("AcknowledgementID"))(ArgumentMatchers.any())).thenReturn(sequenceNo.pure)
      when(mockRegistrationMongoRepository.prepareRegistrationSubmission(anyString(), ArgumentMatchers.any(),
        ArgumentMatchers.any())(ArgumentMatchers.any())).thenReturn(Future.successful(true))

      await(service.ensureAcknowledgementReference(testRegId, VatRegStatus.draft)) mustBe formattedRefNumber
    }
  }

  "getValidDocumentStatus" should {
    val vatScheme = VatScheme(
      testRegId,
      testInternalid,
      None,
      None,
      None,
      status = VatRegStatus.draft,
      eligibilitySubmissionData = Some(testEligibilitySubmissionData)
    )

    "throw an exception if the document is not available" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(ArgumentMatchers.eq(testRegId))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      intercept[MissingRegDocument](await(service.getValidDocumentStatus(testRegId)))
    }

    "throw an exception if the document is not locked or draft" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(ArgumentMatchers.eq(testRegId))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme.copy(status = VatRegStatus.cancelled))))

      intercept[InvalidSubmissionStatus](await(service.getValidDocumentStatus(testRegId)))
    }

    "return the status as being draft" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(ArgumentMatchers.eq(testRegId))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme)))

      await(service.getValidDocumentStatus(testRegId)) mustBe VatRegStatus.draft
    }
  }

  "Calling buildDesSubmission" should {
    "successfully create a full DES submission" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(ArgumentMatchers.eq(testRegId))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(testFullVatScheme)))

      await(service.buildSubmission(testRegId)) mustBe testFullSubmission
    }

    "successfully create a partial DES submission" in new Setup {
      val partialScheme: VatScheme = testFullVatScheme.copy(bankAccount = None, flatRateScheme = None)
      val partialDESSubmission: VatSubmission = testFullSubmission.copy(bankDetails = None, flatRateScheme = None)

      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(partialScheme)))

      await(service.buildSubmission(testRegId)) mustBe partialDESSubmission
    }

    "throw a MissingRegDocument exception when there is no registration in mongo" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))
      intercept[MissingRegDocument](await(service.buildSubmission(testRegId)))
    }

    "throw a IllegalStateException when the vat scheme doesn't contain returns" in new Setup {
      val vatSchemeNoTradingDetails: VatScheme = VatScheme(testRegId, testInternalid, None, None, None, status = VatRegStatus.draft)

      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatSchemeNoTradingDetails)))

      intercept[IllegalStateException](await(service.buildSubmission(testRegId)))
    }
  }

}
