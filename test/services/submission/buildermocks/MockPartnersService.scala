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

package services.submission.buildermocks

import models.api.Partner
import models.registration.sections.PartnersSection
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import services.PartnersService

import scala.concurrent.Future

trait MockPartnersService extends MockitoSugar {
  self: Suite =>

  val mockPartnersService = mock[PartnersService]

  def mockGetPartner(regId: String, index: Int)(response: Future[Option[Partner]]): OngoingStubbing[Future[Option[Partner]]] =
    when(mockPartnersService.getPartner(
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(index)
    )).thenReturn(response)

  def mockGetPartners(regId: String)(response: Future[Option[PartnersSection]]): OngoingStubbing[Future[Option[PartnersSection]]] =
    when(mockPartnersService.getPartners(ArgumentMatchers.eq(regId))).thenReturn(response)

  def mockStorePartner(regId: String, partner: Partner, index: Int)(response: Future[Partner]):OngoingStubbing[Future[Partner]] =
    when(mockPartnersService.storePartner(
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(partner),
      ArgumentMatchers.eq(index)
    )).thenReturn(response)

  def mockDeletePartner(regId: String, index: Int)(response: Future[PartnersSection]): OngoingStubbing[Future[PartnersSection]] =
    when(mockPartnersService.deletePartner(
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(index)
    )).thenReturn(response)

}
