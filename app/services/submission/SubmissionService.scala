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

package services.submission

import cats.instances.FutureInstances
import common.exceptions._
import connectors.VatSubmissionConnector
import enums.VatRegStatus
import featureswitch.core.config.FeatureSwitching
import models.api.{Submitted, VatScheme}
import play.api.Logging
import play.api.libs.json.JsObject
import play.api.mvc.Request
import repositories._
import services.monitoring.{AuditService, SubmissionAuditBlockBuilder}
import services.{NonRepudiationService, TrafficManagementService}
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utils.{IdGenerator, TimeMachine}

import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionService @Inject()(registrationRepository: RegistrationMongoRepository,
                                  vatSubmissionConnector: VatSubmissionConnector,
                                  nonRepudiationService: NonRepudiationService,
                                  trafficManagementService: TrafficManagementService,
                                  submissionPayloadBuilder: SubmissionPayloadBuilder,
                                  submissionAuditBlockBuilder: SubmissionAuditBlockBuilder,
                                  timeMachine: TimeMachine,
                                  auditService: AuditService,
                                  idGenerator: IdGenerator,
                                  val authConnector: AuthConnector
                                 )(implicit executionContext: ExecutionContext) extends FutureInstances with AuthorisedFunctions with Logging with FeatureSwitching {

  def submitVatRegistration(regId: String, userHeaders: Map[String, String])
                           (implicit hc: HeaderCarrier,
                            request: Request[_]): Future[String] = {
    for {
      vatScheme <- registrationRepository.retrieveVatScheme(regId)
        .map(_.getOrElse(throw new InternalServerException("[SubmissionService][submitVatRegistration] Missing VatScheme")))
      _ <- vatScheme.status match {
        case VatRegStatus.draft | VatRegStatus.locked =>
          registrationRepository.lockSubmission(regId)
        case _ =>
          throw InvalidSubmissionStatus(s"VAT submission status was in a ${vatScheme.status} state")
      }
      submission <- submissionPayloadBuilder.buildSubmissionPayload(regId)
      formBundleId <- submit(submission, vatScheme, regId, userHeaders) // TODO refactor so this returns a value from the VatRegStatus enum or maybe an ADT
      _ <- registrationRepository.finishRegistrationSubmission(regId, VatRegStatus.submitted, formBundleId)
      _ <- trafficManagementService.updateStatus(regId, Submitted)
    } yield {
      formBundleId
    }
  }

  // scalastyle:off
  private[services] def submit(submission: JsObject,
                               vatScheme: VatScheme,
                               regId: String,
                               userHeaders: Map[String, String]
                              )(implicit hc: HeaderCarrier,
                                request: Request[_]): Future[String] = {

    val correlationId = idGenerator.createId
    logger.info(s"VAT Submission API Correlation Id: $correlationId for the following regId: $regId")

    authorised().retrieve(credentials and affinityGroup and agentCode) {
      case Some(credentials) ~ Some(affinity) ~ optAgentCode =>
        vatSubmissionConnector.submit(submission, correlationId, credentials.providerId).map { formBundleId =>
          auditService.audit(
            submissionAuditBlockBuilder.buildAuditJson(
              vatScheme = vatScheme,
              authProviderId = credentials.providerId,
              affinityGroup = affinity,
              optAgentReferenceNumber = optAgentCode
            )
          )

          val encodedHtml = vatScheme.nrsSubmissionPayload
            .getOrElse(throw new InternalServerException("[SubmissionService][submit] Missing NRS Submission payload"))
          val payloadString = new String(Base64.getDecoder.decode(encodedHtml))

          nonRepudiationService.submitNonRepudiation(regId, payloadString, timeMachine.timestamp, formBundleId, userHeaders)

          formBundleId
        }
    }
  }

}
