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

package services.submission

import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import common.exceptions._
import enums.VatRegStatus
import featureswitch.core.config.FeatureSwitching
import fixtures.{SubmissionAuditFixture, VatSubmissionFixture}
import helpers.VatRegSpec
import mocks.MockTrafficManagementService
import mocks.monitoring.MockAuditService
import models.api._
import models.monitoring.SubmissionAuditModel
import models.nonrepudiation.NonRepudiationSubmissionAccepted
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.scalatest.concurrent.Eventually
import play.api.libs.json.JsObject
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.monitoring.buildermocks.MockSubmissionAuditBlockBuilder
import services.submission.buildermocks.MockSubmissionPayloadBuilder
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import utils.IdGenerator

import scala.concurrent.Future

class SubmissionServiceSpec extends VatRegSpec
  with SubmissionAuditFixture
  with VatSubmissionFixture
  with ApplicativeSyntax
  with FutureInstances
  with MockAuditService
  with Eventually
  with MockTrafficManagementService
  with MockSubmissionPayloadBuilder
  with MockSubmissionAuditBlockBuilder
  with FeatureSwitching {

  class Setup {

    object TestIdGenerator extends IdGenerator {
      override def createId: String = "TestCorrelationId"
    }

    val service: SubmissionService = new SubmissionService(
      registrationRepository = mockRegistrationMongoRepository,
      vatSubmissionConnector = mockVatSubmissionConnector,
      nonRepudiationService = mockNonRepudiationService,
      trafficManagementService = mockTrafficManagementService,
      submissionPayloadBuilder = mockSubmissionPayloadBuilder,
      submissionAuditBlockBuilder = mockSubmissionAuditBlockBuilder,
      idGenerator = TestIdGenerator,
      auditService = mockAuditService,
      timeMachine = mockTimeMachine,
      authConnector = mockAuthConnector
    )
  }

  val testRegInfo: RegistrationInformation = RegistrationInformation(
    internalId = testInternalId,
    registrationId = testRegId,
    status = Submitted,
    regStartDate = testDate,
    channel = VatReg,
    lastModified = testDate
  )

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", "testUrl")

  val testFormBundleId = "testFormBundleId"

  "submitVatRegistration" when {
    "successfully submit and return an acknowledgment reference" in new Setup {
      when(mockRegistrationMongoRepository.retrieveVatScheme(anyString()))
        .thenReturn(Future.successful(Some(testFullVatScheme)))
      when(mockRegistrationMongoRepository.lockSubmission(anyString())).thenReturn(Future.successful(true))
      when(mockVatSubmissionConnector.submit(any[JsObject], anyString(), anyString())(any())).thenReturn(Future.successful(testFormBundleId))
      when(mockRegistrationMongoRepository.finishRegistrationSubmission(anyString(), any(), any())).thenReturn(Future.successful(VatRegStatus.submitted))
      mockUpdateStatus(testRegId, Submitted)(Future.successful(Some(testRegInfo)))
      mockBuildAuditJson(testFullVatScheme, testProviderId, Organisation, None)(SubmissionAuditModel(detailBlockAnswers, testFullVatScheme, testProviderId, Organisation, None))
      when(mockTimeMachine.timestamp).thenReturn(testDateTime)
      when(mockSubmissionPayloadBuilder.buildSubmissionPayload(testRegId)).thenReturn(Future.successful(vatSubmissionVoluntaryJson.as[JsObject]))
      val testNonRepudiationSubmissionId = "testNonRepudiationSubmissionId"

      when(mockNonRepudiationService.submitNonRepudiation(
        ArgumentMatchers.eq(testRegId),
        ArgumentMatchers.eq(testSubmissionPayload),
        ArgumentMatchers.eq(testDateTime),
        ArgumentMatchers.eq(testFormBundleId),
        ArgumentMatchers.eq(testUserHeaders)
      )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(request))).thenReturn(Future.successful(NonRepudiationSubmissionAccepted(testNonRepudiationSubmissionId)))

      mockAuthorise(Retrievals.credentials and Retrievals.affinityGroup and Retrievals.agentCode)(
        Future.successful(
          Some(testCredentials) ~ Some(testAffinityGroup) ~ None
        )
      )

      await(service.submitVatRegistration(testRegId, testUserHeaders)) mustBe testFormBundleId
      eventually {
        verifyAudit(SubmissionAuditModel(
          detailBlockAnswers,
          testFullVatScheme,
          testProviderId,
          testAffinityGroup,
          None
        ))
        verify(mockNonRepudiationService).submitNonRepudiation(
          ArgumentMatchers.eq(testRegId),
          ArgumentMatchers.eq(testSubmissionPayload),
          ArgumentMatchers.eq(testDateTime),
          ArgumentMatchers.eq(testFormBundleId),
          ArgumentMatchers.eq(testUserHeaders)
        )(ArgumentMatchers.eq(hc), ArgumentMatchers.eq(request))
      }
    }
  }

  "submit" should {
    "return a 200 response and successfully audit when all calls succeed" in new Setup {
      when(mockVatSubmissionConnector.submit(any[JsObject], anyString(), anyString())(any())).thenReturn(Future.successful(testFormBundleId))
      mockAuthorise(Retrievals.credentials and Retrievals.affinityGroup and Retrievals.agentCode)(
        Future.successful(
          Some(testCredentials) ~ Some(testAffinityGroup) ~ None
        )
      )
      mockBuildAuditJson(testFullVatScheme, testProviderId, Organisation, None)(SubmissionAuditModel(detailBlockAnswers, testFullVatScheme, testProviderId, Organisation, None))

      await(service.submit(vatSubmissionJson.as[JsObject], testFullVatScheme, testRegId, testUserHeaders)) mustBe testFormBundleId

      verifyAudit(SubmissionAuditModel(
        detailBlockAnswers,
        testFullVatScheme,
        testProviderId,
        testAffinityGroup,
        None
      ))
    }

    "return a 502 response and successfully audit when submission fails with a 502" in new Setup {
      when(mockVatSubmissionConnector.submit(any[JsObject], anyString(), anyString())(any())).thenReturn(Future.successful(testFormBundleId))
      mockAuthorise(Retrievals.credentials and Retrievals.affinityGroup and Retrievals.agentCode)(
        Future.successful(
          Some(testCredentials) ~ Some(testAffinityGroup) ~ None
        )
      )
      mockBuildAuditJson(testFullVatScheme, testProviderId, Organisation, None)(SubmissionAuditModel(detailBlockAnswers, testFullVatScheme, testProviderId, Organisation, None))

      await(service.submit(vatSubmissionJson.as[JsObject], testFullVatScheme, testRegId, testUserHeaders)) mustBe testFormBundleId

      verifyAudit(SubmissionAuditModel(
        detailBlockAnswers,
        testFullVatScheme,
        testProviderId,
        testAffinityGroup,
        None
      ))
    }
  }

}
