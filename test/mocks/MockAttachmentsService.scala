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

import models.api.{AttachmentType, VatScheme}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import services.AttachmentsService

import scala.concurrent.Future

trait MockAttachmentsService extends MockitoSugar {
  self: Suite =>

  val mockAttachmentService: AttachmentsService = mock[AttachmentsService]

  def mockGetAttachmentList(internalId: String, regId: String)
                           (response: Future[List[AttachmentType]]): OngoingStubbing[Future[List[AttachmentType]]] =
    when(mockAttachmentService.getAttachmentList(ArgumentMatchers.eq(internalId), ArgumentMatchers.eq(regId)))
      .thenReturn(response)

  def mockAttachmentList(vatScheme: VatScheme)
                        (response: List[AttachmentType]): OngoingStubbing[List[AttachmentType]] =
    when(mockAttachmentService.mandatoryAttachmentList(ArgumentMatchers.eq(vatScheme)))
      .thenReturn(response)
}

