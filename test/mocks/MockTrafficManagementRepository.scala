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

import java.time.LocalDate

import models.api.{RegistrationChannel, RegistrationInformation, RegistrationStatus}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import repositories.trafficmanagement.TrafficManagementRepository

import scala.concurrent.Future

trait MockTrafficManagementRepository extends MockitoSugar {

  val mockTrafficManagementRepository = mock[TrafficManagementRepository]

  def mockGetRegInfo(internalId: String)
                    (response: Future[Option[RegistrationInformation]]): OngoingStubbing[Future[Option[RegistrationInformation]]] =
    when(mockTrafficManagementRepository.getRegistrationInformation(
      ArgumentMatchers.eq(internalId)
    )).thenReturn(response)

  def mockUpsertRegInfo(internalId: String,
                        regId: String,
                        status: RegistrationStatus,
                        regStartDate: Option[LocalDate],
                        channel: RegistrationChannel)
                       (response: Future[RegistrationInformation]): OngoingStubbing[Future[RegistrationInformation]] =
    when(mockTrafficManagementRepository.upsertRegistrationInformation(
      ArgumentMatchers.eq(internalId),
      ArgumentMatchers.eq(regId),
      ArgumentMatchers.eq(status),
      ArgumentMatchers.eq(regStartDate),
      ArgumentMatchers.eq(channel)
    )) thenReturn response

}
