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

import models.GroupRegistration
import models.api._
import models.submission._
import repositories.{UpscanMongoRepository, VatSchemeRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AttachmentsService @Inject()(val registrationRepository: VatSchemeRepository,
                                   upscanMongoRepository: UpscanMongoRepository
                                  )(implicit executionContext: ExecutionContext) {

  private val attachmentDetailsKey = "attachments"

  def getAttachmentList(regId: String): Future[Set[AttachmentType]] =
    registrationRepository.retrieveVatScheme(regId).map {
      case Some(vatScheme) => attachmentList(vatScheme)
      case None => Set.empty
    }

  def getIncompleteAttachments(regId: String): Future[List[AttachmentType]] = {
    for {
      attachmentList <- getAttachmentList(regId)
      requiredAttachments = attachmentList.toList.flatMap {
        case IdentityEvidence => List(PrimaryIdentityEvidence, ExtraIdentityEvidence, ExtraIdentityEvidence)
        case TransactorIdentityEvidence => List(PrimaryTransactorIdentityEvidence, ExtraTransactorIdentityEvidence, ExtraTransactorIdentityEvidence)
        case attachmentType => List(attachmentType)
      }
      upscanDetails <- upscanMongoRepository.getAllUpscanDetails(regId)
      completeAttachments = upscanDetails.collect {
        case UpscanDetails(_, _, Some(attachmentType), _, Ready, _, _) => attachmentType
      }
      incompleteAttachments = requiredAttachments.diff(completeAttachments)
    } yield incompleteAttachments
  }

  def attachmentList(vatScheme: VatScheme): Set[AttachmentType] = {
    Set(
      getTransactorIdentityEvidenceAttachment(vatScheme),
      getIdentityEvidenceAttachment(vatScheme),
      getVat2Attachment(vatScheme),
      getVat51Attachment(vatScheme),
      getVat5LAttachment(vatScheme),
      getTaxRepresentativeAttachment(vatScheme)
    ).flatten
  }

  def getAttachmentDetails(regId: String): Future[Option[Attachments]] =
    registrationRepository.fetchBlock[Attachments](regId, attachmentDetailsKey)

  def storeAttachmentDetails(regId: String, attachmentDetails: Attachments): Future[Attachments] =
    registrationRepository.updateBlock[Attachments](regId, attachmentDetails, attachmentDetailsKey)

  private def getIdentityEvidenceAttachment(vatScheme: VatScheme): Option[IdentityEvidence.type] = {
    val unverifiedPersonalDetails = vatScheme.applicantDetails.exists(data => !data.personalDetails.identifiersMatch)
    if (unverifiedPersonalDetails) Some(IdentityEvidence) else None
  }

  private def getTransactorIdentityEvidenceAttachment(vatScheme: VatScheme): Option[TransactorIdentityEvidence.type] = {
    val needIdentityDocuments = vatScheme.transactorDetails.exists(data => !data.personalDetails.identifiersMatch)
    if (needIdentityDocuments) Some(TransactorIdentityEvidence) else None
  }

  private def getVat2Attachment(vatScheme: VatScheme): Option[VAT2.type] = {
    val allPartnershipsExceptLLP = List(Partnership, LtdPartnership, ScotPartnership, ScotLtdPartnership)
    val needVat2ForPartnership = vatScheme.eligibilitySubmissionData.exists(data => allPartnershipsExceptLLP.contains(data.partyType))
    if (needVat2ForPartnership) Some(VAT2) else None
  }

  private def getVat51Attachment(vatScheme: VatScheme): Option[VAT51.type] = {
    vatScheme.eligibilitySubmissionData.map(_.registrationReason) match {
      case Some(GroupRegistration) => Some(VAT51)
      case _ => None
    }
  }

  private def getTaxRepresentativeAttachment(vatScheme: VatScheme): Option[TaxRepresentativeAuthorisation.type] = {
    vatScheme.returns.flatMap(_.hasTaxRepresentative) match {
      case Some(hasTaxRepresentative) if hasTaxRepresentative => Some(TaxRepresentativeAuthorisation)
      case _ => None
    }
  }

  private def getVat5LAttachment(vatScheme: VatScheme): Option[VAT5L.type] = {
    if (vatScheme.sicAndCompliance.exists(_.hasLandAndProperty.contains(true))) Some(VAT5L) else None
  }
}
