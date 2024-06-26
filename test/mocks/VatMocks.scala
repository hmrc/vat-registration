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

package mocks

import connectors._
import enums.VatRegStatus
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.mockito.{ArgumentMatchers => Matchers}
import org.scalatestplus.mockito.MockitoSugar
import repositories._
import services._
import services.submission.SubmissionService
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthConnector, InvalidBearerToken}
import uk.gov.hmrc.http.HeaderCarrier
import utils.TimeMachine

import scala.concurrent.{ExecutionContext, Future}

trait VatMocks {

  this: MockitoSugar =>

  lazy val mockAuthConnector: AuthConnector                     = mock[AuthConnector]
  lazy val mockRegistrationMongoRepository: VatSchemeRepository = mock[VatSchemeRepository]
  lazy val mockSubmissionService: SubmissionService             = mock[SubmissionService]
  lazy val mockVatRegistrationService: VatRegistrationService   = mock[VatRegistrationService]
  lazy val mockVatSubmissionConnector: VatSubmissionConnector   = mock[VatSubmissionConnector]
  lazy val mockEligibilityService: EligibilityService           = mock[EligibilityService]
  lazy val mockNonRepudiationService: NonRepudiationService     = mock[NonRepudiationService]
  lazy val mockNonRepudiationConnector: NonRepudiationConnector = mock[NonRepudiationConnector]
  lazy val mockTimeMachine: TimeMachine                         = mock[TimeMachine]

  object AuthorisationMocks {

    def mockAuthenticated(intId: String): OngoingStubbing[Future[Option[String]]] =
      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(intId)))

    def mockAuthenticatedLoggedInNoCorrespondingData(): OngoingStubbing[Future[Option[String]]] =
      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future.successful(None))

    def mockNotLoggedInOrAuthenticated(): OngoingStubbing[Future[Option[String]]] =
      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any()))
        .thenReturn(Future.failed(new InvalidBearerToken("Invalid Bearer Token")))

    def mockAuthorised(regId: String, internalId: String): OngoingStubbing[Future[Option[String]]] = {
      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(internalId)))

      when(mockRegistrationMongoRepository.getInternalId(Matchers.eq[String](regId)))
        .thenReturn(Future.successful(Some(internalId)))
    }

    def mockNotAuthorised(regId: String, internalId: String): OngoingStubbing[Future[Option[String]]] = {
      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(internalId)))

      when(mockRegistrationMongoRepository.getInternalId(Matchers.eq(regId)))
        .thenReturn(Future.successful(Some(internalId + "xxx")))
    }

    def mockAuthMongoResourceNotFound(regId: String, internalId: String): OngoingStubbing[Future[Option[String]]] = {
      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(internalId)))

      when(mockRegistrationMongoRepository.getInternalId(Matchers.eq(regId)))
        .thenReturn(Future.successful(None))
    }

    def mockNotLoggedInOrAuthorised(regId: String): OngoingStubbing[Future[Option[String]]] = {
      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any()))
        .thenReturn(Future.successful(None))

      when(mockRegistrationMongoRepository.getInternalId(Matchers.eq(regId)))
        .thenReturn(Future.successful(Some("SomeInternalId")))
    }

  }

  object ServiceMocks {
    def mockGetDocumentStatus(status: VatRegStatus.Value): Unit =
      when(mockVatRegistrationService.getStatus(anyString(), anyString())(any()))
        .thenReturn(Future.successful(status))
  }

  implicit class RetrievalCombiner[A](a: A) {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }

  def mockAuthorise[T](retrievals: Retrieval[T])(response: Future[T]): Unit =
    when(
      mockAuthConnector.authorise(
        Matchers.any,
        Matchers.eq(retrievals)
      )(Matchers.any[HeaderCarrier], Matchers.any[ExecutionContext])
    ) thenReturn response

}
