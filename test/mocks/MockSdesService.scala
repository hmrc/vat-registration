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

import models.sdes.SdesNotificationResult
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Request
import services.SdesService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockSdesService {
  this: MockitoSugar =>

  val mockSdesService: SdesService = mock[SdesService]

  def mockNotifySdes(regId: String,
                     formBundleId: String,
                     nrsSubmissionId: Option[String],
                     providerId: String,
                     result: Future[Seq[SdesNotificationResult]]): OngoingStubbing[Future[Seq[SdesNotificationResult]]] =
    when(
      mockSdesService.notifySdes(
        ArgumentMatchers.eq(regId),
        ArgumentMatchers.eq(formBundleId),
        ArgumentMatchers.eq(nrsSubmissionId),
        ArgumentMatchers.eq(providerId)
      )(ArgumentMatchers.any[HeaderCarrier],
        ArgumentMatchers.any[Request[_]],
        ArgumentMatchers.any[ExecutionContext])
    ).thenReturn(result)
}
