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

package mocks

import models.api.VatScheme
import models.registration.RegistrationSectionId
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import services.RegistrationService

import scala.concurrent.Future

trait MockRegistrationService extends MockitoSugar {
  self: Suite =>

  val mockRegistrationService = mock[RegistrationService]

  def mockNewRegistration(internalId: String)
                         (response: Future[VatScheme]): OngoingStubbing[Future[VatScheme]] =
    when(mockRegistrationService.newRegistration(ArgumentMatchers.eq(internalId)))
    .thenReturn(response)

  def mockGetAnswer[T](internalId: String, regId: String, section: RegistrationSectionId, answer: String)
                      (response: Future[Option[T]]): OngoingStubbing[Future[Option[T]]] =
    when(mockRegistrationService.getAnswer[T](
      ArgumentMatchers.eq(internalId),
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(section),
      ArgumentMatchers.eq(answer)
    )(ArgumentMatchers.any())) thenReturn response

}
