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

package mocks

import models.api.{SicAndCompliance, VatScheme}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import repositories.RegistrationMongoRepository

import scala.concurrent.Future

trait MockRegistrationRepository extends MockitoSugar {
  self: Suite =>

  lazy val mockRegistrationRepository: RegistrationMongoRepository = mock[RegistrationMongoRepository]

  def mockCreateRegistration(regId: String, internalId: String)(response: VatScheme): OngoingStubbing[Future[VatScheme]] =
    when(mockRegistrationRepository.createNewVatScheme(
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(internalId)
    )).thenReturn(Future.successful(response))

  def mockInsertVatScheme(vatScheme: VatScheme): OngoingStubbing[Future[VatScheme]] =
    when(mockRegistrationRepository.insertVatScheme(ArgumentMatchers.eq(vatScheme)))
      .thenReturn(Future.successful(vatScheme))

  def mockGetVatScheme(regId: String)(response: Option[VatScheme]): OngoingStubbing[Future[Option[VatScheme]]] =
    when(mockRegistrationRepository.retrieveVatScheme(regId)).thenReturn(Future.successful(response))

  def mockFetchSicAndCompliance(regid: String)(response: Option[SicAndCompliance]): OngoingStubbing[Future[Option[SicAndCompliance]]] =
    when(mockRegistrationRepository.fetchSicAndCompliance(regid)).thenReturn(Future.successful(response))

  def mockFetchBlock[T](regId: String, key: String)(response: Future[Option[T]]): OngoingStubbing[Future[Option[T]]] =
    when(mockRegistrationRepository.fetchBlock[T](
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(key)
    )(ArgumentMatchers.any())).thenReturn(response)

  def mockUpdateBlock[T](regId: String, data: T, key: String): OngoingStubbing[Future[T]] =
    when(mockRegistrationRepository.updateBlock[T](
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(data),
      ArgumentMatchers.eq(key)
    )(ArgumentMatchers.any())).thenReturn(Future.successful(data))

}
