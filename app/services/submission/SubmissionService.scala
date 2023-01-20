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
import connectors.VatSubmissionConnector
import enums.VatRegStatus
import featureswitch.core.config.FeatureSwitching
import httpparsers.VatSubmissionHttpParser.VatSubmissionResponse
import httpparsers.{VatSubmissionFailure, VatSubmissionSuccess}
import models.api.schemas.API1364
import models.api.vatapplication.Annual
import models.api.{Attached, PersonalDetails, VatScheme}
import models.monitoring.SubmissionFailureErrorsAuditModel
import models.{IntendingTrader, Voluntary}
import play.api.Logging
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
import utils.{IdGenerator, TimeMachine}

import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionService @Inject()(registrationRepository: VatSchemeRepository,
                                  vatSubmissionConnector: VatSubmissionConnector,
                                  nonRepudiationService: NonRepudiationService,
                                  submissionPayloadBuilder: SubmissionPayloadBuilder,
                                  submissionAuditBlockBuilder: SubmissionAuditBlockBuilder,
                                  attachmentsService: AttachmentsService,
                                  sdesService: SdesService,
                                  timeMachine: TimeMachine,
                                  auditService: AuditService,
                                  idGenerator: IdGenerator,
                                  emailService: EmailService,
                                  schemaValidationService: SchemaValidationService,
                                  apiSchema: API1364,
                                  val authConnector: AuthConnector
                                 )(implicit executionContext: ExecutionContext) extends FutureInstances with AuthorisedFunctions with Logging with FeatureSwitching {

  def submitVatRegistration(internalId: String, regId: String, userHeaders: Map[String, String], lang: String)
                           (implicit hc: HeaderCarrier,
                            request: Request[_]): Future[VatSubmissionResponse] =
    (for {
      _ <- registrationRepository.updateSubmissionStatus(internalId, regId, VatRegStatus.locked)
      vatScheme <- registrationRepository.getRegistration(internalId, regId)
        .map(_.getOrElse(throw new InternalServerException("[SubmissionService][submitVatRegistration] Missing VatScheme")))
      submission = submissionPayloadBuilder.buildSubmissionPayload(vatScheme)
      correlationId = idGenerator.createId
      submissionResponse <- submit(submission, regId, correlationId)
      _ <- logSubmission(vatScheme, submissionResponse)
      optFormBundle <- handleResponse(submissionResponse, submission.toString(), regId, correlationId, internalId)
    } yield {
      optFormBundle match {
        case Some(formBundleId) =>
          postSubmissionTasks(vatScheme, formBundleId, lang, userHeaders) // Non blocking async post submission tasks
          submissionResponse
        case None =>
          submissionResponse
      }
    }).recover {
      case exception: BadRequestException =>
        throw exception
      case exception =>
        registrationRepository.updateSubmissionStatus(internalId, regId, VatRegStatus.failedRetryable)
        throw exception
    }

  // Non blocking tasks to be done after a successful submission.
  // To be called on a separate async thread to allow the user to proceed.
  private[services] def postSubmissionTasks(vatScheme: VatScheme,
                                            formBundleId: String,
                                            lang: String,
                                            userHeaders: Map[String, String])
                                           (implicit hc: HeaderCarrier, request: Request[_]): Future[Unit] = {
    for {
      (providerId, affinityGroup, optAgentCode) <- retrieveIdentityDetails
      _ <- auditSubmission(formBundleId, vatScheme, providerId, affinityGroup, optAgentCode)
      _ <- emailService.sendRegistrationReceivedEmail(vatScheme.internalId, vatScheme.registrationId, lang)
      digitalAttachments = vatScheme.attachments.exists(_.method.contains(Attached)) && attachmentsService.mandatoryAttachmentList(vatScheme).nonEmpty
      optNrsId <- submitToNrs(formBundleId, vatScheme, userHeaders, digitalAttachments)
      _ <- if (digitalAttachments) sdesService.notifySdes(vatScheme.registrationId, formBundleId, optNrsId, providerId) else Future.successful()
    } yield {}
  }

  private[services] def submit(submission: JsObject,
                               regId: String,
                               correlationId: String)
                              (implicit hc: HeaderCarrier,
                               request: Request[_]): Future[VatSubmissionResponse] = {

    logger.info(s"VAT Submission API Correlation Id: $correlationId for the following regId: $regId")

    authorised().retrieve(credentials) { case Some(credentials) =>
      vatSubmissionConnector.submit(submission, correlationId, credentials.providerId)
    }
  }

  private[services] def handleResponse(vatSubmissionResponse: VatSubmissionResponse,
                                       submissionPayload: String,
                                       regId: String,
                                       correlationId: String,
                                       internalId: String)
                                      (implicit hc: HeaderCarrier, request: Request[_]): Future[Option[String]] = {
    val suppressedKey = "suppressedErrors"
    val unknownKey = "unknownErrors"

    vatSubmissionResponse match {
      case Left(VatSubmissionFailure(CONFLICT, _)) =>
        registrationRepository.updateSubmissionStatus(internalId, regId, VatRegStatus.duplicateSubmission)
          .map(_ => None)
      case Left(VatSubmissionFailure(BAD_REQUEST, _)) =>
        for {
          _ <- registrationRepository.updateSubmissionStatus(internalId, regId, VatRegStatus.failed)
          errors = schemaValidationService.validate(apiSchema, submissionPayload)
         _ = auditService.audit(SubmissionFailureErrorsAuditModel(regId, correlationId, errors))
        } yield {
          errors.get(suppressedKey).foreach { errors =>
            logger.error(s"[Suppressed submission errors] Submission for reg id '$regId' failed with suppressed " +
              s"errors for the following fields:\n${errors.mkString(", ")}\n")
          }
          errors.get(unknownKey).map { errors =>
            logger.error(s"[Unknown submission errors] Submission for reg id '$regId' failed with new or unfiltered errors for the following fields:\n${errors.mkString(", ")}\n")
            throw new BadRequestException(s"[Unknown submission errors] Submission for reg id '$regId' failed with new or unfiltered errors for the following fields:\n${errors.mkString(", ")}\n")
          }

          None
        }
      case Left(VatSubmissionFailure(_, _)) =>
        registrationRepository.updateSubmissionStatus(internalId, regId, VatRegStatus.failedRetryable)
          .map(_ => None)
      case Right(VatSubmissionSuccess(formBundleId)) =>
        registrationRepository.finishRegistrationSubmission(regId, VatRegStatus.submitted, formBundleId)
          .map(_ => Some(formBundleId))
    }
  }

  private[services] def submitToNrs(formBundleId: String,
                                    vatScheme: VatScheme,
                                    userHeaders: Map[String, String],
                                    digitalAttachments: Boolean)
                                   (implicit hc: HeaderCarrier,
                                    request: Request[_]): Future[Option[String]] = {
    val encodedHtml = vatScheme.nrsSubmissionPayload
      .getOrElse(throw new InternalServerException("[SubmissionService][submit] Missing NRS Submission payload"))
    val payloadString = new String(Base64.getDecoder.decode(encodedHtml))

    nonRepudiationService.submitNonRepudiation(vatScheme.registrationId, payloadString, timeMachine.timestamp, formBundleId, userHeaders, digitalAttachments)
      .recover {
        case _ =>
          logger.error("[SubmissionService] NRS Returned an unexpected exception")
          None
      }
  }

  private[services] def retrieveIdentityDetails(implicit hc: HeaderCarrier,
                                                request: Request[_]): Future[(String, AffinityGroup, Option[String])] =
    authorised().retrieve(credentials and affinityGroup and agentCode) {
      case Some(credentials) ~ Some(affinity) ~ optAgentCode =>
        Future.successful((credentials.providerId, affinity, optAgentCode))
      case _ =>
        Future.failed(throw new InternalServerException("[SubmissionService] Couldn't retrieve auth details for user"))
    }

  private[services] def auditSubmission(formBundleId: String,
                                        vatScheme: VatScheme,
                                        providerId: String,
                                        affinityGroup: AffinityGroup,
                                        optAgentCode: Option[String])
                                       (implicit hc: HeaderCarrier,
                                        request: Request[_]): Future[Unit] = {
    Future.successful(auditService.audit(
      submissionAuditBlockBuilder.buildAuditJson(
        vatScheme = vatScheme,
        authProviderId = providerId,
        affinityGroup = affinityGroup,
        optAgentReferenceNumber = optAgentCode,
        formBundleId = formBundleId
      )
    ))
  }

  // scalastyle:off
  private[services] def logSubmission(vatScheme: VatScheme,
                                      vatSubmissionStatus: VatSubmissionResponse): Future[Unit] = {

    val agentOrTransactor = (vatScheme.transactorDetails.flatMap(_.personalDetails), vatScheme.eligibilitySubmissionData.map(_.isTransactor)) match {
      case (Some(PersonalDetails(_, _, _, Some(_), _, _, _)), Some(true)) => Some("AgentFlow")
      case (Some(_), Some(true)) => Some("TransactorFlow")
      case _ => None
    }

    val exceptionOrExemption = (vatScheme.eligibilitySubmissionData, vatScheme.vatApplication) match {
      case (Some(eligibilityData), Some(vatApplication)) => VatScheme.exceptionOrExemption(eligibilityData, vatApplication) match {
        case VatScheme.exceptionKey => Some("Exception")
        case VatScheme.exemptionKey => Some("Exemption")
        case _ => None
      }
      case _ => None
    }
    val appliedForAas = if (vatScheme.vatApplication.flatMap(_.returnsFrequency).contains(Annual)) Some("AnnualAccountingScheme") else None
    val appliedForFrs = if (vatScheme.flatRateScheme.exists(_.joinFrs.contains(true))) Some("FlatRateScheme") else None
    val hasObi = if (vatScheme.otherBusinessInvolvements.exists(_.nonEmpty)) Some("OtherBusinessInvolvements") else None
    val specialSituations = List(exceptionOrExemption, appliedForAas, appliedForFrs, hasObi).flatten

    val attachmentList = (
      attachmentsService.mandatoryAttachmentList(vatScheme) ++
        attachmentsService.optionalAttachmentList(vatScheme)
      ).map(_.toString)

    val regReason = if (vatScheme.vatApplication.exists(_.currentlyTrading.contains(false)) &&
      vatScheme.eligibilitySubmissionData.exists(_.registrationReason.equals(Voluntary))) {
      Some(IntendingTrader.toString)
    } else {
      vatScheme.eligibilitySubmissionData.map(_.registrationReason.toString)
    }

    Future.successful(logger.info(jsonObject(
      "logInfo" -> "SubmissionLog",
      "status" -> vatSubmissionStatus.fold(_ => "Failed", _ => "Successful"),
      "regId" -> vatScheme.registrationId,
      "partyType" -> vatScheme.partyType.map(_.toString),
      "regReason" -> regReason,
      optional("agentOrTransactor" -> agentOrTransactor),
      conditional(specialSituations.nonEmpty)("specialSituations" -> specialSituations),
      conditional(attachmentList.nonEmpty)("attachments" -> vatScheme.attachments.map(attachmentDetails => jsonObject(
        "attachmentMethod" -> attachmentDetails.method.map(_.toString),
        "attachmentList" -> attachmentList
      )))
    ).toString()))
  }
}
