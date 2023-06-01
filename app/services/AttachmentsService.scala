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

package services

import models.GroupRegistration
import models.api._
import models.registration.AttachmentsSectionId
import models.submission._
import play.api.mvc.Request
import repositories.{UpscanMongoRepository, VatSchemeRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AttachmentsService @Inject()(val registrationRepository: VatSchemeRepository,
                                   upscanMongoRepository: UpscanMongoRepository
                                  )(implicit executionContext: ExecutionContext) {

  def getAttachmentList(internalId: String, regId: String)(implicit request: Request[_]): Future[List[AttachmentType]] =
    registrationRepository.getRegistration(internalId, regId).map {
      case Some(vatScheme) =>
        mandatoryAttachmentList(vatScheme) match {
          case Nil if vatScheme.attachments.exists(_.method.isDefined) => // Only triggered when users change answers, this drops the old invalid ones.
            upscanMongoRepository.deleteAllUpscanDetails(regId)
            if (vatScheme.attachments.exists(_.additionalPartnersDocuments.isEmpty)) {
              registrationRepository.deleteSection(internalId, regId, AttachmentsSectionId.repoKey)
            } else {
              val updatedAttachments = vatScheme.attachments
                .map(_.copy(method = None, supplyVat1614a = None, supplyVat1614h = None, supplySupportingDocuments = None))
                .getOrElse(Attachments())
              registrationRepository.upsertSection(internalId, regId, AttachmentsSectionId.repoKey, updatedAttachments)
            }
            Nil
          case list => list
        }
      case None =>
        List.empty[AttachmentType]
    }

  def getIncompleteAttachments(internalId: String, regId: String)(implicit request: Request[_]): Future[List[AttachmentType]] = {
    for {
      attachmentList <- getAttachmentList(internalId, regId)
      requiredAttachments = attachmentList.flatMap {
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

  def mandatoryAttachmentList(vatScheme: VatScheme): List[AttachmentType] = {
    List(
      getTransactorIdentityEvidenceAttachment(vatScheme),
      getIdentityEvidenceAttachment(vatScheme),
      getVat2Attachment(vatScheme),
      getVat51Attachment(vatScheme),
      getTaxRepresentativeAttachment(vatScheme),
      getVat5LAttachment(vatScheme)
    ).flatten
  }

  def optionalAttachmentList(vatScheme: VatScheme): List[AttachmentType] = {
    List(
      if (vatScheme.attachments.flatMap(_.supplyVat1614a).contains(true)) Some(Attachment1614a) else None,
      if (vatScheme.attachments.flatMap(_.supplyVat1614h).contains(true)) Some(Attachment1614h) else None,
      if (vatScheme.attachments.flatMap(_.supplySupportingDocuments).contains(true)) Some(LandPropertyOtherDocs) else None
    ).flatten
  }

  private def getIdentityEvidenceAttachment(vatScheme: VatScheme): Option[IdentityEvidence.type] = {
    val unverifiedPersonalDetails = vatScheme.applicantDetails.exists(data => !data.personalDetails.exists(_.identifiersMatch))
    if (unverifiedPersonalDetails) Some(IdentityEvidence) else None
  }

  private def getTransactorIdentityEvidenceAttachment(vatScheme: VatScheme): Option[TransactorIdentityEvidence.type] = {
    val needIdentityDocuments = vatScheme.transactorDetails.exists(data => !data.personalDetails.exists(_.identifiersMatch))
    if (needIdentityDocuments) Some(TransactorIdentityEvidence) else None
  }

  private def getVat2Attachment(vatScheme: VatScheme): Option[VAT2.type] = {
    vatScheme.attachments.flatMap(_.additionalPartnersDocuments) match {
      case Some(true) => Some(VAT2)
      case Some(false) => None
      case _ =>
        val allPartnershipsExceptLLP = List(Partnership, LtdPartnership, ScotPartnership, ScotLtdPartnership)
        val needVat2ForPartnership = vatScheme.eligibilitySubmissionData.exists(data => allPartnershipsExceptLLP.contains(data.partyType))
        if (needVat2ForPartnership) Some(VAT2) else None
    }
  }

  private def getVat51Attachment(vatScheme: VatScheme): Option[VAT51.type] = {
    vatScheme.eligibilitySubmissionData.map(_.registrationReason) match {
      case Some(GroupRegistration) => Some(VAT51)
      case _ => None
    }
  }

  private def getTaxRepresentativeAttachment(vatScheme: VatScheme): Option[TaxRepresentativeAuthorisation.type] = {
    vatScheme.vatApplication.flatMap(_.hasTaxRepresentative) match {
      case Some(hasTaxRepresentative) if hasTaxRepresentative => Some(TaxRepresentativeAuthorisation)
      case _ => None
    }
  }

  private def getVat5LAttachment(vatScheme: VatScheme): Option[VAT5L.type] = {
    if (vatScheme.business.exists(_.hasLandAndProperty.contains(true))) Some(VAT5L) else None
  }
}
