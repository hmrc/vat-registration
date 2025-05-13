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

package services.submission

import cats.instances.FutureInstances
import connectors.{NonRepudiationConnector, VatSubmissionConnector}
import enums.VatRegStatus
import featureswitch.core.config.{FeatureSwitching, PostSubmissionDecoupling, PostSubmissionDecouplingConnector}
import httpparsers.VatSubmissionHttpParser.VatSubmissionResponse
import httpparsers.{VatSubmissionFailure, VatSubmissionSuccess}
import models.api.schemas.API1364
import models.api.vatapplication.Annual
import models.api.{Attached, PersonalDetails, VatScheme}
import models.monitoring.SubmissionFailureErrorsAuditModel
import models.nonrepudiation.NonRepudiationAuditing.{NonRepudiationAttachmentFailureAuditUpscan, NonRepudiationAttachmentSuccessAuditUpscan}
import models.nonrepudiation.{NonRepudiationAttachment, NonRepudiationAttachmentAccepted, NonRepudiationAttachmentFailed}
import models.{IntendingTrader, Voluntary}
import play.api.http.Status.{BAD_REQUEST, CONFLICT}
import play.api.libs.json.JsObject
import play.api.mvc.Request
import repositories._
import services._
import services.monitoring.{AuditService, SubmissionAuditBlockBuilder}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{BadRequestException, HeaderCarrier, InternalServerException}
import utils.JsonUtils.{conditional, jsonObject, optional}
import utils.{AlertLogging, IdGenerator, PagerDutyKeys, TimeMachine}

import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionService @Inject()(
                                   registrationRepository: VatSchemeRepository,
                                   vatSubmissionConnector: VatSubmissionConnector,
                                   nonRepudiationService: NonRepudiationService,
                                   submissionPayloadBuilder: SubmissionPayloadBuilder,
                                   submissionAuditBlockBuilder: SubmissionAuditBlockBuilder,
                                   attachmentsService: AttachmentsService,
                                   upscanService: UpscanService,
                                   nonRepudiationConnector: NonRepudiationConnector,
                                   sdesService: SdesService,
                                   timeMachine: TimeMachine,
                                   auditService: AuditService,
                                   idGenerator: IdGenerator,
                                   emailService: EmailService,
                                   schemaValidationService: SchemaValidationService,
                                   apiSchema: API1364,
                                   val authConnector: AuthConnector
                                 )(implicit executionContext: ExecutionContext)
  extends FutureInstances
    with AuthorisedFunctions
    with AlertLogging
    with FeatureSwitching {

  def submitVatRegistration(internalId: String, regId: String, userHeaders: Map[String, String], lang: String)(implicit
                                                                                                               hc: HeaderCarrier,
                                                                                                               request: Request[_]
  ): Future[VatSubmissionResponse] = {
    infoLog(s"[SubmissionService][submitVatRegistration] attempting to submit registration", regId)
    (for {
      _ <- registrationRepository.updateSubmissionStatus(internalId, regId, VatRegStatus.locked)
      vatScheme <- registrationRepository
        .getRegistration(internalId, regId)
        .map(
          _.getOrElse(
            throw new InternalServerException(
              s"[SubmissionService][submitVatRegistration] Missing VatScheme. regId: $regId"
            )
          )
        )
      submission = submissionPayloadBuilder.buildSubmissionPayload(vatScheme)
      correlationId = idGenerator.createId
      submissionResponse <- submit(submission, regId, correlationId)
      _ <- logSubmission(vatScheme, submissionResponse)
      optFormBundle <- handleResponse(submissionResponse, submission.toString(), regId, correlationId, internalId)
    } yield optFormBundle match {
      case Some(formBundleId) =>
        postSubmissionTasks(vatScheme, formBundleId, lang, userHeaders, correlationId) // Non blocking async post submission tasks
        submissionResponse
      case None =>
        submissionResponse
    }).recover {
      case exception: BadRequestException =>
        println("====>>>>>>>>>BADREQUEST====>>>")
        errorLog(s"[SubmissionService][submitVatRegistration] - $exception", regId)
        throw exception
      case exception =>
        registrationRepository.updateSubmissionStatus(internalId, regId, VatRegStatus.failedRetryable)
        errorLog(s"[SubmissionService][submitVatRegistration] - $exception", regId)
        throw exception

    }
  }

  // Non blocking tasks to be done after a successful submission.
  // To be called on a separate async thread to allow the user to proceed.
  private[services] def postSubmissionTasks(
                                             vatScheme: VatScheme,
                                             formBundleId: String,
                                             lang: String,
                                             userHeaders: Map[String, String],
                                             correlationId: String
                                           )(implicit hc: HeaderCarrier, request: Request[_]): Future[Unit] =
    for {
      (providerId, affinityGroup, optAgentCode) <- retrieveIdentityDetails
      _ <- auditSubmission(formBundleId, vatScheme, providerId, affinityGroup, optAgentCode)
      _ <- emailService.sendRegistrationReceivedEmail(vatScheme.internalId, vatScheme.registrationId, lang)
      digitalAttachments = vatScheme.attachments.exists(_.method.contains(Attached)) && attachmentsService.mandatoryAttachmentList(vatScheme).nonEmpty
      optNrsId <- submitToNrs(formBundleId, vatScheme, userHeaders, digitalAttachments)
      _ <- if (isEnabled(PostSubmissionDecoupling) && optNrsId.isDefined && digitalAttachments) {
              notifyNrs(vatScheme.registrationId, optNrsId.get, correlationId)
            } else { Future.successful() }
      _ <- if (digitalAttachments) {
              sdesService.notifySdes(vatScheme.registrationId, formBundleId, optNrsId, providerId)
            } else {
              Future.successful()
            }
    } yield {}

  private def notifyNrs(registrationId: String, nrSubmissionId: String, correlationId: String)
                       (implicit request: Request[_], hc: HeaderCarrier): Future[Seq[Unit]] = {
    upscanService.getAllUpscanDetails(registrationId).map { upscanDetailsList => {
      upscanDetailsList.map { upscanDetails => {
        val url = upscanDetails.downloadUrl
        val attachmentId = upscanDetails.reference
        val optMimeType = upscanDetails.uploadDetails.map(_.fileMimeType)
        val optChecksum = upscanDetails.uploadDetails.map(_.checksum)
        val filename = upscanDetails.uploadDetails.map(_.fileName).get

        if(url.isDefined && optChecksum.isDefined && optMimeType.isDefined) {
          val payload = NonRepudiationAttachment(
            attachmentUrl = url.get,
            attachmentId = attachmentId,
            attachmentSha256Checksum = optChecksum.get,
            attachmentContentType = optMimeType.get,
            nrSubmissionId = nrSubmissionId
          )
          infoLog(s"[SubmissionService] Sending NRS attachment for NR submission $nrSubmissionId, attachment $attachmentId")
            if (isEnabled(PostSubmissionDecouplingConnector)) {
              nonRepudiationConnector.submitAttachmentNonRepudiation(payload).map {
                case NonRepudiationAttachmentAccepted(nrAttachmentId) =>
                  auditService.audit(NonRepudiationAttachmentSuccessAuditUpscan(payload, nrAttachmentId, correlationId, filename))
                  infoLog(
                    s"[SubmissionService][notifyNrs] Successful attachment NRS submission with id $nrAttachmentId for attachment $attachmentId"
                  )
                case NonRepudiationAttachmentFailed(body, status) =>
                  auditService.audit(NonRepudiationAttachmentFailureAuditUpscan(payload, status, correlationId, filename))
                  errorLog(
                    s"[SubmissionService][notifyNrs] Attachment NRS submission failed with status: $status and body: $body. CorrelationId: $correlationId"
                  )
                  pagerduty(
                    PagerDutyKeys.NRS_ATTACHMENT_NOTIFICATION_FAILED,
                    Some(s"[SubmissionService][notifyNrs] Attachment NRS submission failed with status: $status and body: $body")
                  )
              }
            } else { infoLog(s"[SubmissionService][notifyNrs] Not sending NRS attachment as PostSubmissionDecouplingConnector is off. Attachment $attachmentId")}
        }
        else {
          pagerduty(
            PagerDutyKeys.INVALID_UPSCAN_DETAILS_RECEIVED,
            Some(s"[SubmissionService][notifyNrs] Not sending attachment NRS payload for $attachmentId as incomplete upscan details were received"))
          Future.successful()
        }
      }
      }
    }
    }
  }

  private[services] def submit(submission: JsObject, regId: String, correlationId: String)(implicit
                                                                                           hc: HeaderCarrier,
                                                                                           request: Request[_]
  ): Future[VatSubmissionResponse] = {

    infoLog(
      s"[SubmissionService][submit] attempting to submit. VAT Submission API Correlation Id: $correlationId",
      regId
    )
    authorised().retrieve(credentials) { case Some(credentials) =>
      infoLog(s"[SubmissionService][submit] credentials retrieved from auth", regId)
      vatSubmissionConnector.submit(submission, correlationId, credentials.providerId)
    }
  }

  private[services] def handleResponse(
                                        vatSubmissionResponse: VatSubmissionResponse,
                                        submissionPayload: String,
                                        regId: String,
                                        correlationId: String,
                                        internalId: String
                                      )(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[String]] = {
    val suppressedKey = "suppressedErrors"
    val unknownKey = "unknownErrors"

    vatSubmissionResponse match {
      case Left(VatSubmissionFailure(CONFLICT, _)) =>
        errorLog(s"[SubmissionService][handleResponse] submission failed due to CONFLICT", regId)
        registrationRepository
          .updateSubmissionStatus(internalId, regId, VatRegStatus.duplicateSubmission)
          .map(_ => None)

      case Left(VatSubmissionFailure(BAD_REQUEST, _)) =>
        errorLog(s"[SubmissionService][handleResponse] submission failed due to BAD_REQUEST", regId)
        for {
          _ <- registrationRepository.updateSubmissionStatus(internalId, regId, VatRegStatus.failed)
          errors = schemaValidationService.validate(apiSchema, submissionPayload)
          _ = auditService.audit(SubmissionFailureErrorsAuditModel(regId, correlationId, errors))
        } yield {
          errors.get(suppressedKey).foreach { errors =>
            errorLog(
              s"[SubmissionService][handleResponse][Suppressed submission errors] Submission for reg id '$regId' failed with suppressed " +
                s"errors for the following fields:\n${errors.mkString(", ")}\n"
            )
          }
          errors.get(unknownKey).map { errors =>
            errorLog(
              s"[SubmissionService][handleResponse][Unknown submission errors] Submission for reg id '$regId' failed with new or unfiltered errors for the following fields:\n${
                errors
                  .mkString(", ")
              }\n"
            )
            throw new BadRequestException(
              s"[Unknown submission errors] Submission for reg id '$regId' failed with new or unfiltered errors for the following fields:\n${
                errors
                  .mkString(", ")
              }\n"
            )
          }
          None
        }
      case Left(VatSubmissionFailure(status, reason)) =>
        errorLog(s"[SubmissionService][handleResponse] submission failed status $status, reason: $reason", regId)
        registrationRepository
          .updateSubmissionStatus(internalId, regId, VatRegStatus.failedRetryable)
          .map(_ => None)
      case Right(VatSubmissionSuccess(formBundleId)) =>
        infoLog(s"[SubmissionService][handleResponse] submission successful. Attempting to update mongo", regId)
        registrationRepository
          .finishRegistrationSubmission(regId, VatRegStatus.submitted, formBundleId)
          .map(_ => Some(formBundleId))
    }
  }

  private[services] def submitToNrs(
                                     formBundleId: String,
                                     vatScheme: VatScheme,
                                     userHeaders: Map[String, String],
                                     digitalAttachments: Boolean
                                   )(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[String]] = {
    val encodedHtml = vatScheme.nrsSubmissionPayload
      .getOrElse(throw new InternalServerException("[SubmissionService][submit] Missing NRS Submission payload"))
    val payloadString = new String(Base64.getDecoder.decode(encodedHtml))

    nonRepudiationService
      .submitNonRepudiation(
        vatScheme.registrationId,
        payloadString,
        timeMachine.timestamp,
        formBundleId,
        userHeaders,
        digitalAttachments
      )
      .recover { case _ =>
        errorLog("[SubmissionService] NRS Returned an unexpected exception")
        None
      }
  }

  private[services] def retrieveIdentityDetails(implicit
                                                hc: HeaderCarrier,
                                                request: Request[_]
                                               ): Future[(String, AffinityGroup, Option[String])] =
    authorised().retrieve(credentials and affinityGroup and agentCode) {
      case Some(credentials) ~ Some(affinity) ~ optAgentCode =>
        Future.successful((credentials.providerId, affinity, optAgentCode))
      case _ =>
        errorLog("[SubmissionService][retrieveIdentityDetails] - Couldn't retrieve auth details for user")
        Future.failed(throw new InternalServerException("[SubmissionService] Couldn't retrieve auth details for user"))
    }

  private[services] def auditSubmission(
                                         formBundleId: String,
                                         vatScheme: VatScheme,
                                         providerId: String,
                                         affinityGroup: AffinityGroup,
                                         optAgentCode: Option[String]
                                       )(implicit hc: HeaderCarrier, request: Request[_]): Future[Unit] =
    Future.successful(
      auditService.audit(
        submissionAuditBlockBuilder.buildAuditJson(
          vatScheme = vatScheme,
          authProviderId = providerId,
          affinityGroup = affinityGroup,
          optAgentReferenceNumber = optAgentCode,
          formBundleId = formBundleId
        )
      )
    )

  // scalastyle:off
  private[services] def logSubmission(vatScheme: VatScheme, vatSubmissionStatus: VatSubmissionResponse)(implicit
                                                                                                        request: Request[_]
  ): Future[Unit] = {

    val agentOrTransactor = (
      vatScheme.transactorDetails.flatMap(_.personalDetails),
      vatScheme.eligibilitySubmissionData.map(_.isTransactor)
    ) match {
      case (Some(PersonalDetails(_, _, _, Some(_), _, _, _)), Some(true)) => Some("AgentFlow")
      case (Some(_), Some(true)) => Some("TransactorFlow")
      case _ => None
    }

    val exceptionOrExemption = (vatScheme.eligibilitySubmissionData, vatScheme.vatApplication) match {
      case (Some(eligibilityData), Some(vatApplication)) =>
        VatScheme.exceptionOrExemption(eligibilityData, vatApplication) match {
          case VatScheme.exceptionKey => Some("Exception")
          case VatScheme.exemptionKey => Some("Exemption")
          case _ => None
        }
      case _ => None
    }
    val appliedForAas =
      if (vatScheme.vatApplication.flatMap(_.returnsFrequency).contains(Annual)) Some("AnnualAccountingScheme")
      else None
    val appliedForFrs = if (vatScheme.flatRateScheme.exists(_.joinFrs.contains(true))) Some("FlatRateScheme") else None
    val hasObi = if (vatScheme.otherBusinessInvolvements.exists(_.nonEmpty)) Some("OtherBusinessInvolvements") else None
    val specialSituations = List(exceptionOrExemption, appliedForAas, appliedForFrs, hasObi).flatten

    val attachmentList = (
      attachmentsService.mandatoryAttachmentList(vatScheme) ++
        attachmentsService.optionalAttachmentList(vatScheme)
      ).map(_.toString)

    val regReason =
      if (
        vatScheme.vatApplication.exists(_.currentlyTrading.contains(false)) &&
          vatScheme.eligibilitySubmissionData.exists(_.registrationReason.equals(Voluntary))
      ) {
        Some(IntendingTrader.toString)
      } else {
        vatScheme.eligibilitySubmissionData.map(_.registrationReason.toString)
      }

    Future.successful(
      infoLog(
        jsonObject(
          "logInfo" -> "SubmissionLog",
          "status" -> vatSubmissionStatus.fold(_ => "Failed", _ => "Successful"),
          optional(
            "failureReason" ->
              vatSubmissionStatus
                .fold(failure => Some(s"status: ${failure.status}, reason: ${failure.body}"), _ => None)
          ),
          "regId" -> vatScheme.registrationId,
          "partyType" -> vatScheme.partyType.map(_.toString),
          "regReason" -> regReason,
          optional("agentOrTransactor" -> agentOrTransactor),
          conditional(specialSituations.nonEmpty)("specialSituations" -> specialSituations),
          conditional(attachmentList.nonEmpty)(
            "attachments" -> vatScheme.attachments.map(attachmentDetails =>
              jsonObject(
                "attachmentMethod" -> attachmentDetails.method.map(_.toString),
                "attachmentList" -> attachmentList
              )
            )
          )
        ).toString()
      )
    )
  }
}
