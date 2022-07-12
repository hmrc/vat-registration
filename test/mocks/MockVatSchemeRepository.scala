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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsValue, Reads}
import repositories.VatSchemeRepository

import scala.concurrent.Future

trait MockVatSchemeRepository extends MockitoSugar {
  self: Suite =>

  lazy val mockVatSchemeRepository: VatSchemeRepository = mock[VatSchemeRepository]

  def mockNewVatScheme(regId: String, internalId: String)(response: VatScheme): OngoingStubbing[Future[VatScheme]] =
    when(mockVatSchemeRepository.createNewVatScheme(
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(internalId)
    )).thenReturn(Future.successful(response))

  def mockInsertVatScheme(vatScheme: VatScheme): OngoingStubbing[Future[VatScheme]] =
    when(mockVatSchemeRepository.insertVatScheme(ArgumentMatchers.eq(vatScheme)))
      .thenReturn(Future.successful(vatScheme))

  def mockGetVatScheme(regId: String)(response: Option[VatScheme]): OngoingStubbing[Future[Option[VatScheme]]] =
    when(mockVatSchemeRepository.retrieveVatScheme(regId)).thenReturn(Future.successful(response))

  def mockFetchBlock[T](regId: String, key: String)(response: Future[Option[T]]): OngoingStubbing[Future[Option[T]]] =
    when(mockVatSchemeRepository.fetchBlock[T](
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(key)
    )(ArgumentMatchers.any())).thenReturn(response)

  def mockUpdateBlock[T](regId: String, data: T, key: String): OngoingStubbing[Future[T]] =
    when(mockVatSchemeRepository.updateBlock[T](
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(data),
      ArgumentMatchers.eq(key)
    )(ArgumentMatchers.any())).thenReturn(Future.successful(data))

  def mockGetAllRegistrations(internalId: String)(response: Future[List[JsValue]]): OngoingStubbing[Future[List[JsValue]]] =
    when(mockVatSchemeRepository.getAllRegistrations(
      ArgumentMatchers.eq(internalId)
    )) thenReturn response

  def mockGetRegistration(internalId: String, regId: String)(response: Future[Option[VatScheme]]): OngoingStubbing[Future[Option[VatScheme]]] =
    when(mockVatSchemeRepository.getRegistration(
      ArgumentMatchers.eq(internalId),
      ArgumentMatchers.eq(regId)
    )) thenReturn response

  def mockUpsertRegistration(internalId: String, regId: String, data: JsValue)
                            (response: Future[Option[JsValue]]): OngoingStubbing[Future[Option[JsValue]]] =
    when(mockVatSchemeRepository.upsertRegistration(
      ArgumentMatchers.eq(internalId),
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(data)
    )) thenReturn response

  def mockDeleteRegistration(internalId: String, regId: String)
                            (response: Future[Boolean]): OngoingStubbing[Future[Boolean]] =
    when(mockVatSchemeRepository.deleteRegistration(
      ArgumentMatchers.eq(internalId),
      ArgumentMatchers.eq(regId)
    )) thenReturn response

  def mockGetSection[T](internalId: String, regId: String, key: String)(response: Future[Option[T]]): OngoingStubbing[Future[Option[T]]] =
    when(mockVatSchemeRepository.getSection[T](
      ArgumentMatchers.eq(internalId),
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(key)
    )(ArgumentMatchers.any[Reads[T]])).thenReturn(response)

  def mockUpsertSection[T](internalId: String, regId: String, key: String, data: T)(response: Option[T]): OngoingStubbing[Future[Option[T]]] =
    when(mockVatSchemeRepository.upsertSection[T](
      ArgumentMatchers.eq(internalId),
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(key),
      ArgumentMatchers.eq(data)
    )(ArgumentMatchers.any())).thenReturn(Future.successful(response))

  def mockDeleteSection(internalId: String, regId: String, key: String)(response: Boolean): OngoingStubbing[Future[Boolean]] =
    when(mockVatSchemeRepository.deleteSection(
      ArgumentMatchers.eq(internalId),
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(key)
    )).thenReturn(Future.successful(response))
}
