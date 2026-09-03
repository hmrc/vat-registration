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

import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import config.BackendConfig
import enums.VatRegStatus
import models.api.VatScheme
import play.api.mvc.Request
import repositories.VatSchemeRepository
import uk.gov.hmrc.http.InternalServerException
import utils.LoggingUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatRegistrationService @Inject() (registrationRepository: VatSchemeRepository, val backendConfig: BackendConfig)(implicit
    executionContext: ExecutionContext
) extends ApplicativeSyntax
    with FutureInstances
    with LoggingUtils {

  private[services] val unsafeScore: Int = 100

  def getStatus(internalId: String, regId: String)(implicit request: Request[_]): Future[VatRegStatus.Value] =
    registrationRepository.getRegistration(internalId, regId) map {
      case Some(registration) =>
        checkForRisks(registration) match {
          case Right(_) => registration.status
          case Left(failedChecks) =>
            warnLog(
              s"[VatRegistrationService][checkForRisks] - Registration details for $regId failed risk checks:" +
                s"\n- ${failedChecks.mkString(",\n- ")}")
            registrationRepository.updateSubmissionStatus(internalId, regId, VatRegStatus.contact)
            VatRegStatus.contact
        }
      case None =>
        warnLog(s"[VatRegistrationService][getStatus] - No VAT registration document found for $regId")
        throw new InternalServerException(s"[VatRegistrationService] No VAT registration document found for $regId")
    }

  private[services] def checkForRisks(registration: VatScheme): Either[Seq[String], Unit] = {
    val failedChecks: Seq[String] = Seq(
      registration.applicantDetails
        .flatMap(_.personalDetails.flatMap(_.score))
        .collect { case `unsafeScore` =>
          s"Applicant personal details score was $unsafeScore"
        },
      registration.transactorDetails
        .flatMap(_.personalDetails.flatMap(_.score))
        .collect { case `unsafeScore` =>
          s"Transactor personal details score was $unsafeScore"
        },
      registration.applicantDetails
        .flatMap(_.contact.email)
        .collect {
          case email if backendConfig.emailCheck.exists(email.endsWith) =>
            s"Applicant email ($email) has an unaccepted domain"
        },
      registration.transactorDetails
        .flatMap(_.email)
        .collect {
          case email if backendConfig.emailCheck.exists(email.endsWith) =>
            s"Transactor email ($email) has an unaccepted domain"
        }
    ).flatten

    if (failedChecks.nonEmpty) Left(failedChecks) else Right(())
  }

}
