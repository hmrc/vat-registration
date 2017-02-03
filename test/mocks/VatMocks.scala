/*
 * Copyright 2017 HM Revenue & Customs
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

import common.Now
import common.exceptions.GenericServiceException
import connectors.{AuthConnector, Authority}
import models.{VatChoice, VatScheme}
import org.joda.time.DateTime
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import services.RegistrationService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait VatMocks extends WSHTTPMock {

  this: MockitoSugar =>

  lazy val mockAuthConnector = mock[AuthConnector]
  lazy val mockRegistrationService = mock[RegistrationService]

  object AuthorisationMocks {

    def mockSuccessfulAuthorisation(authority: Authority): OngoingStubbing[Future[Option[Authority]]] = {
      when(mockAuthConnector.getCurrentAuthority()(Matchers.any()))
        .thenReturn(Future.successful(Some(authority)))
    }

    def mockNotLoggedInOrAuthorised: OngoingStubbing[Future[Option[Authority]]] = {
      when(mockAuthConnector.getCurrentAuthority()(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(None))
    }

    def mockNotAuthorised(authority: Authority): OngoingStubbing[Future[Option[Authority]]] = {
      when(mockAuthConnector.getCurrentAuthority()(Matchers.any()))
        .thenReturn(Future.successful(Some(authority)))
    }

  }

  object ServiceMocks {

    def mockSuccessfulCreateNewRegistration(registrationId: String): Unit = {
      when(mockRegistrationService.createNewRegistration(Matchers.any()))
        .thenReturn(Future.successful(Right(VatScheme.blank(registrationId)(Now(new DateTime(2017, 1, 31, 13, 6))))))
    }

    def mockSuccessfulUpdateVatChoice(registrationId: String, vatChoice: VatChoice): Unit = {
      when(mockRegistrationService.updateVatChoice(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Right(vatChoice)))
    }

    def mockServiceUnavailableUpdateVatChoice(registrationId: String, vatChoice: VatChoice, exception :Exception): Unit = {
      when(mockRegistrationService.updateVatChoice(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Left(GenericServiceException(exception))))
    }

    def mockFailedCreateNewRegistration(registrationId: String): Unit = {
      when(mockRegistrationService.createNewRegistration(Matchers.any()))
        .thenReturn(Future.successful(Left(GenericServiceException(new RuntimeException("something went wrong")))))
    }

  }

}
