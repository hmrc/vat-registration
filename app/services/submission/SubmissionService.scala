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

package services.submission

import cats.instances.FutureInstances
import connectors.VatSubmissionConnector
import enums.VatRegStatus
import featureswitch.core.config.FeatureSwitching
import httpparsers.VatSubmissionHttpParser.VatSubmissionResponse
import models.api.returns.Annual
import models.api.{Attached, PersonalDetails, Submitted, VatScheme}
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, CONFLICT}
import play.api.libs.json.JsObject
import play.api.mvc.Request
import repositories._
import services.monitoring.{AuditService, SubmissionAuditBlockBuilder}
import services._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{BadRequestException, ConflictException, HeaderCarrier, InternalServerException}
import utils.JsonUtils.{conditional, jsonObject, optional}
import utils.{IdGenerator, TimeMachine}

import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionService @Inject()(registrationRepository: VatSchemeRepository,
                                  vatSubmissionConnector: VatSubmissionConnector,
                                  nonRepudiationService: NonRepudiationService,
                                  trafficManagementService: TrafficManagementService,
                                  submissionPayloadBuilder: SubmissionPayloadBuilder,
                                  submissionAuditBlockBuilder: SubmissionAuditBlockBuilder,
                                  attachmentsService: AttachmentsService,
                                  sdesService: SdesService,
                                  timeMachine: TimeMachine,
                                  auditService: AuditService,
                                  idGenerator: IdGenerator,
                                  emailService: EmailService,
                                  val authConnector: AuthConnector
                                 )(implicit executionContext: ExecutionContext) extends FutureInstances with AuthorisedFunctions with Logging with FeatureSwitching {

  def submitVatRegistration(regId: String, userHeaders: Map[String, String])
                           (implicit hc: HeaderCarrier,
                            request: Request[_]): Future[String] = {
    {
      for {
        _ <- registrationRepository.updateSubmissionStatus(regId, VatRegStatus.locked)
        vatScheme <- registrationRepository.retrieveVatScheme(regId)
          .map(_.getOrElse(throw new InternalServerException("[SubmissionService][submitVatRegistration] Missing VatScheme")))
        submission = submissionPayloadBuilder.buildSubmissionPayload(vatScheme)
        correlationId = idGenerator.createId
        submissionResponse <- submit(submission, regId, correlationId)
        _ <- logSubmission(vatScheme, submissionResponse)
        formBundleId <- handleResponse(submissionResponse, regId) // chain ends here if the main submission failed
        (providerId, affinityGroup, optAgentCode) <- retrieveIdentityDetails
        _ <- auditSubmission(formBundleId, vatScheme, providerId, affinityGroup, optAgentCode)
        _ <- trafficManagementService.updateStatus(regId, Submitted)
        _ <- emailService.sendRegistrationReceivedEmail(regId)
        digitalAttachments = vatScheme.attachments.exists(_.method.equals(Attached)) && attachmentsService.attachmentList(vatScheme).nonEmpty
        optNrsId <- submitToNrs(formBundleId, vatScheme, userHeaders, digitalAttachments)
        _ <- if (digitalAttachments) Future.successful(sdesService.notifySdes(regId, formBundleId, correlationId, optNrsId, providerId)) else Future.successful()
      } yield formBundleId
    } recover {
      case exception: ConflictException =>
        registrationRepository.updateSubmissionStatus(regId, VatRegStatus.duplicateSubmission)
        throw exception
      case exception: BadRequestException =>
        registrationRepository.updateSubmissionStatus(regId, VatRegStatus.failed)
        throw exception
      case exception =>
        registrationRepository.updateSubmissionStatus(regId, VatRegStatus.failedRetryable)
        throw exception
    }
  }

  private[services] def submit(submission: JsObject,
                               regId: String,
                               correlationId: String
                              )(implicit hc: HeaderCarrier,
                                request: Request[_]): Future[VatSubmissionResponse] = {

    logger.info(s"VAT Submission API Correlation Id: $correlationId for the following regId: $regId")

    authorised().retrieve(credentials) { case Some(credentials) =>
      vatSubmissionConnector.submit(submission, correlationId, credentials.providerId)
    }
  }

  private[services] def handleResponse(vatSubmissionStatus: VatSubmissionResponse,
                                       regId: String)(implicit hc: HeaderCarrier,
                                                      request: Request[_]): Future[String] = {
    vatSubmissionStatus.fold(
      failure =>
        failure.status match {
          case CONFLICT => throw new ConflictException(failure.body)
          case BAD_REQUEST => throw new BadRequestException(failure.body)
          case _ => throw new InternalServerException(failure.body)
        },
      success =>
        registrationRepository
          .finishRegistrationSubmission(regId, VatRegStatus.submitted, success.formBundleId)
          .map(_ => success.formBundleId)
    )
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

    nonRepudiationService.submitNonRepudiation(vatScheme.id, payloadString, timeMachine.timestamp, formBundleId, userHeaders, digitalAttachments).recover {
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
        Future.failed(throw new InternalServerException("Couldn't retrieve auth details for user"))
    }

  private[services] def auditSubmission(formBundleId: String,
                                        vatScheme: VatScheme,
                                        providerId: String,
                                        affinityGroup: AffinityGroup,
                                        optAgentCode: Option[String])
                                       (implicit hc: HeaderCarrier,
                                        request: Request[_]): Future[Unit] = {
    auditService.audit(
      submissionAuditBlockBuilder.buildAuditJson(
        vatScheme = vatScheme,
        authProviderId = providerId,
        affinityGroup = affinityGroup,
        optAgentReferenceNumber = optAgentCode,
        formBundleId = formBundleId
      )
    )

    Future.successful()
  }

  private[services] def logSubmission(vatScheme: VatScheme,
                                      vatSubmissionStatus: VatSubmissionResponse): Future[Unit] = {

    val agentOrTransactor = (vatScheme.transactorDetails.map(_.personalDetails), vatScheme.eligibilitySubmissionData.map(_.isTransactor)) match {
      case (Some(PersonalDetails(_, _, _, Some(_), _, _)), Some(true)) => Some("AgentFlow")
      case (Some(_), Some(true)) => Some("TransactorFlow")
      case _ => None
    }

    val exceptionOrExemption = (vatScheme.eligibilitySubmissionData, vatScheme.returns) match {
      case (Some(eligibilityData), Some(returns)) => VatScheme.exceptionOrExemption(eligibilityData, returns) match {
        case VatScheme.exceptionKey => Some("Exception")
        case VatScheme.exemptionKey => Some("Exemption")
        case _ => None
      }
      case _ => None
    }
    val appliedForAas = if (vatScheme.returns.map(_.returnsFrequency).contains(Annual)) Some("AnnualAccountingScheme") else None
    val appliedForFrs = if (vatScheme.flatRateScheme.exists(_.joinFrs)) Some("FlatRateScheme") else None
    val hasObi = if (vatScheme.otherBusinessInvolvements.exists(_.nonEmpty)) Some("OtherBusinessInvolvements") else None
    val specialSituations = List(exceptionOrExemption, appliedForAas, appliedForFrs, hasObi).flatten

    val attachmentList = attachmentsService.attachmentList(vatScheme).map(_.toString)

    logger.info(jsonObject(
      "logInfo" -> "SubmissionLog",
      "status" -> vatSubmissionStatus.fold(_ => "Failed", _ => "Successful"),
      "regId" -> vatScheme.id,
      "partyType" -> vatScheme.partyType.map(_.toString),
      "regReason" -> vatScheme.eligibilitySubmissionData.map(_.registrationReason.toString),
      optional("agentOrTransactor" -> agentOrTransactor),
      conditional(specialSituations.nonEmpty)("specialSituations" -> specialSituations),
      optional("attachments" -> vatScheme.attachments.map(attachmentDetails => jsonObject(
        "attachmentMethod" -> attachmentDetails.method.toString,
        "attachments" -> attachmentList
      )))
    ).toString())

    Future.successful()
  }
}
