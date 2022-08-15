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

import connectors.{NonRepudiationConnector, SdesConnector}
import models.api.{Ready, UpscanDetails}
import models.nonrepudiation.NonRepudiationAuditing.{NonRepudiationAttachmentFailureAudit, NonRepudiationAttachmentSuccessAudit}
import models.nonrepudiation.{NonRepudiationAttachment, NonRepudiationAttachmentAccepted, NonRepudiationAttachmentFailed}
import models.sdes.PropertyExtractor._
import models.sdes.SdesAuditing.{SdesCallbackFailureAudit, SdesFileSubmissionAudit}
import models.sdes._
import play.api.Logging
import play.api.mvc.Request
import repositories.UpscanMongoRepository
import services.SdesService.{informationType, recipientOrSender}
import services.monitoring.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import utils.IdGenerator

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

//scalastyle:off
@Singleton
class SdesService @Inject()(sdesConnector: SdesConnector,
                            nonRepudiationConnector: NonRepudiationConnector,
                            upscanMongoRepository: UpscanMongoRepository,
                            auditService: AuditService,
                            idGenerator: IdGenerator)
                           (implicit executionContext: ExecutionContext) extends Logging {

  def notifySdes(regId: String,
                 formBundleId: String,
                 nrsSubmissionId: Option[String],
                 providerId: String)
                (implicit hc: HeaderCarrier,
                 request: Request[_],
                 executionContext: ExecutionContext): Future[Seq[SdesNotificationResult]] = {
    upscanMongoRepository.getAllUpscanDetails(regId).flatMap { upscanDetailsList =>
      Future.sequence(upscanDetailsList.collect {
        case UpscanDetails(_, reference, _, Some(downloadUrl), Ready, Some(uploadDetails), _) =>
          val payload: SdesNotification = SdesNotification(
            informationType = informationType,
            file = FileDetails(
              recipientOrSender = recipientOrSender,
              name = s"$formBundleId-${uploadDetails.fileName}",
              location = downloadUrl,
              checksum = Checksum(
                algorithm = checksumAlgorithm,
                value = uploadDetails.checksum
              ),
              size = uploadDetails.size,
              properties = List(
                Property(
                  name = mimeTypeKey,
                  value = uploadDetails.fileMimeType
                ),
                Property(
                  name = prefixedFormBundleKey,
                  value = s"VRS$formBundleId"
                ),
                Property(
                  name = formBundleKey,
                  value = formBundleId
                ),
                Property(
                  name = attachmentReferenceKey,
                  value = reference
                ),
                Property(
                  name = submissionDateKey,
                  value = uploadDetails.uploadTimestamp.format(dateTimeFormatter)
                )
              ) ++ nrsSubmissionId.map(id => Property(
                name = nrsSubmissionKey,
                value = id
              ))
            ),
            audit = AuditDetals(
              correlationID = idGenerator.createId
            )
          )

          sdesConnector.notifySdes(payload).map { result =>
            auditService.audit(SdesFileSubmissionAudit(payload, result, providerId))

            result match {
              case res: SdesNotificationSuccess =>
                logger.info(s"[SdesService] SDES notification sent for $reference")
                res
              case res@SdesNotificationFailure(body, status) =>
                logger.error(s"[SdesService] SDES notification failed with status: $status and body: $body")
                res
              case res@SdesNotificationUnexpectedFailure(status, body) =>
                logger.error(s"[SdesService] SDES notification failed with an unexpected status: $status and body: $body")
                res
            }
          }
      })
    }
  }

  def processCallback(sdesCallback: SdesCallback)(implicit hc: HeaderCarrier, request: Request[_]): Future[Unit] = {
    val optUrl = sdesCallback.getPropertyValue(locationKey)
    val optAttachmentId = sdesCallback.getPropertyValue(attachmentReferenceKey)
    val optMimeType = sdesCallback.getPropertyValue(mimeTypeKey)
    val optNrSubmissionId = sdesCallback.getPropertyValue(nrsSubmissionKey)

    (optUrl, optAttachmentId, optMimeType, optNrSubmissionId, sdesCallback.checksum, sdesCallback.failureReason) match {
      case (Some(url), Some(attachmentId), Some(mimeType), Some(nrSubmissionId), Some(checksum), None) =>
        val payload = NonRepudiationAttachment(
          attachmentUrl = url,
          attachmentId = attachmentId,
          attachmentSha256Checksum = checksum,
          attachmentContentType = mimeType,
          nrSubmissionId = nrSubmissionId
        )

        nonRepudiationConnector.submitAttachmentNonRepudiation(payload).map {
          case NonRepudiationAttachmentAccepted(nrAttachmentId) =>
            auditService.audit(NonRepudiationAttachmentSuccessAudit(sdesCallback, nrAttachmentId))
            logger.info(s"[SdesService] Successful attachment NRS submission with id $nrAttachmentId for attachment $attachmentId")
          case NonRepudiationAttachmentFailed(body, status) =>
            auditService.audit(NonRepudiationAttachmentFailureAudit(sdesCallback, status))
            logger.error(s"[SdesService] Attachment NRS submission failed with status: $status and body: $body")
        }
      case (_, Some(attachmentId), _, _, _, Some(failureReason)) =>
        logger.warn(s"[SdesService] Not sending attachment NRS payload as Callback for $attachmentId failed with reason: $failureReason")
        Future.successful(auditService.audit(SdesCallbackFailureAudit(sdesCallback)))
      case (Some(_), Some(_), Some(_), None, Some(_), _) =>
        Future.successful(logger.warn("[SdesService] Not sending attachment NRS payload as NRS failed for the Registration Submission"))
      case _ =>
        Future.successful(logger.error("[SdesService] Could not send attachment NRS payload due to missing data in the callback"))
    }
  }
}

object SdesService {
  val informationType = "1655996667080"
  val recipientOrSender = "400063095160"
}
