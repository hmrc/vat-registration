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

package mocks

import models.api.{RegistrationChannel, RegistrationInformation, RegistrationStatus}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import services.TrafficManagementService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockTrafficManagementService extends MockitoSugar {
  self: Suite =>

  val mockTrafficManagementService = mock[TrafficManagementService]

  def mockGetRegInfo(internalId: String)
                    (response: Future[Option[RegistrationInformation]]): OngoingStubbing[Future[Option[RegistrationInformation]]] =
    when(mockTrafficManagementService.getRegistrationInformation(
      ArgumentMatchers.eq(internalId)
    )(
      ArgumentMatchers.any[HeaderCarrier]
    )).thenReturn(response)

  def mockUpsertRegInfo(internalId: String, regId: String, status: RegistrationStatus, channel: RegistrationChannel)
                                (response: Future[RegistrationInformation]): OngoingStubbing[Future[RegistrationInformation]] =
    when(mockTrafficManagementService.upsertRegistrationInformation(
      ArgumentMatchers.eq(internalId),
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(status),
      ArgumentMatchers.eq(channel)
    )(
      ArgumentMatchers.any[HeaderCarrier]
    )).thenReturn(response)

}