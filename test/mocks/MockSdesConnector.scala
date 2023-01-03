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

import connectors.SdesConnector
import models.sdes.{SdesNotification, SdesNotificationResult}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockSdesConnector {

  this: MockitoSugar =>

  val mockSdesConnector: SdesConnector = mock[SdesConnector]

  def mockNotifySdes(payload: SdesNotification, result: Future[SdesNotificationResult]): OngoingStubbing[Future[SdesNotificationResult]] =
    when(
      mockSdesConnector.notifySdes(ArgumentMatchers.eq(payload))(ArgumentMatchers.any[HeaderCarrier])
    ).thenReturn(result)
}
