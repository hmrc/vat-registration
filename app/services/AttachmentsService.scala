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

package services

import models.api.{AttachmentType, Attachments, IdentityEvidence, VAT2, VatScheme}
import models.submission.{LtdPartnership, NETP, NonUkNonEstablished, Partnership, ScotLtdPartnership, ScotPartnership}
import repositories.VatSchemeRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AttachmentsService @Inject()(val registrationRepository: VatSchemeRepository
                                  )(implicit executionContext: ExecutionContext) {

  private val attachmentDetailsKey = "attachments"

  def getAttachmentList(regId: String): Future[Set[AttachmentType]] =
    registrationRepository.retrieveVatScheme(regId).map {
      case Some(vatScheme) => attachmentList(vatScheme)
      case None => Set.empty
    }

  def attachmentList(vatScheme: VatScheme): Set[AttachmentType] = {
    Set(
      getIdentityEvidenceAttachment(vatScheme),
      getVat2Attachment(vatScheme)
    ).flatten
  }

  def getAttachmentDetails(regId: String): Future[Option[Attachments]] =
    registrationRepository.fetchBlock[Attachments](regId, attachmentDetailsKey)

  def storeAttachmentDetails(regId: String, attachmentDetails: Attachments): Future[Attachments] =
    registrationRepository.updateBlock[Attachments](regId, attachmentDetails, attachmentDetailsKey)

  private def getIdentityEvidenceAttachment(vatScheme: VatScheme): Option[AttachmentType] = {
    val needIdentityDoc = vatScheme.eligibilitySubmissionData.exists(data => List(NETP, NonUkNonEstablished).contains(data.partyType))
    val unverifiedPersonalDetails = vatScheme.applicantDetails.exists(data => !data.personalDetails.identifiersMatch)
    if (needIdentityDoc || unverifiedPersonalDetails) Some(IdentityEvidence) else None
  }

  private def getVat2Attachment(vatScheme: VatScheme): Option[AttachmentType] = {
    val allPartnershipsExceptLLP = List(Partnership, LtdPartnership, ScotPartnership, ScotLtdPartnership)
    val needVat2ForPartnership = vatScheme.eligibilitySubmissionData.exists(data => allPartnershipsExceptLLP.contains(data.partyType))
    if (needVat2ForPartnership) Some(VAT2) else None
  }
}
