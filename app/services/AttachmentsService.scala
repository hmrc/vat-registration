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

package services

import models.api.{Attachments, AttachmentMethod, AttachmentType, IdentityEvidence, VatScheme}
import models.submission.NETP
import repositories.RegistrationMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AttachmentsService @Inject()(val registrationRepository: RegistrationMongoRepository
                                  )(implicit executionContext: ExecutionContext) {

  private val attachmentDetailsKey = "attachments"

  def getAttachmentList(regId: String): Future[List[AttachmentType]] =
    registrationRepository.retrieveVatScheme(regId).map {
      case Some(vatScheme) => attachmentList(vatScheme)
      case None => Nil
    }

  def attachmentList(vatScheme: VatScheme): List[AttachmentType] = {
    val needIdentityDoc = vatScheme.eligibilitySubmissionData.exists(_.partyType.equals(NETP))

    if (needIdentityDoc) List(IdentityEvidence) else Nil
  }

  def getAttachmentDetails(regId: String): Future[Option[Attachments]] =
    registrationRepository.fetchBlock[Attachments](regId, attachmentDetailsKey)

  def storeAttachmentDetails(regId: String, attachmentDetails: Attachments): Future[Attachments] =
    registrationRepository.updateBlock[Attachments](regId, attachmentDetails, attachmentDetailsKey)

}
