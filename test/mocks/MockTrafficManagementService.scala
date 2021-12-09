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

import models.api.{RegistrationInformation, RegistrationStatus}
import models.submission.PartyType
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import services.{AllocationResponse, TrafficManagementService}

import scala.concurrent.Future

trait MockTrafficManagementService extends MockitoSugar {
  self: Suite =>

  val mockTrafficManagementService = mock[TrafficManagementService]

  def mockAllocate(internalId: String, regId: String, partyType: PartyType, isEnrolled: Boolean)
                  (response: Future[AllocationResponse]): OngoingStubbing[Future[AllocationResponse]] =
    when(mockTrafficManagementService.allocate(
      ArgumentMatchers.eq(internalId),
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(partyType),
      ArgumentMatchers.eq(isEnrolled)
    )).thenReturn(response)

  def mockUpdateStatus(regId: String, channel: RegistrationStatus)
                      (response: Future[Option[RegistrationInformation]]): OngoingStubbing[Future[Option[RegistrationInformation]]] =
    when(mockTrafficManagementService.updateStatus(
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(channel)
    )) thenReturn response

}
