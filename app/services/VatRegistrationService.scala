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
import repositories.VatSchemeRepository
import uk.gov.hmrc.http.InternalServerException
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import utils.LoggingUtils
import play.api.mvc.Request

@Singleton
class VatRegistrationService @Inject() (registrationRepository: VatSchemeRepository, val backendConfig: BackendConfig)(
  implicit executionContext: ExecutionContext
) extends ApplicativeSyntax
    with FutureInstances
    with LoggingUtils {

  def getStatus(internalId: String, regId: String)(implicit request: Request[_]): Future[VatRegStatus.Value] =
    registrationRepository.getRegistration(internalId, regId) map {
      case Some(registration) =>
        List(
          registration.applicantDetails.flatMap(_.personalDetails.flatMap(_.score)),
          registration.transactorDetails.flatMap(_.personalDetails.flatMap(_.score)),
          registration.applicantDetails
            .flatMap(_.contact.email)
            .map(email => if (backendConfig.emailCheck.exists(email.endsWith)) 100 else 0),
          registration.transactorDetails
            .flatMap(_.email)
            .map(email => if (backendConfig.emailCheck.exists(email.endsWith)) 100 else 0)
        ).flatten match {
          case scores if scores.contains(100) =>
            registrationRepository.updateSubmissionStatus(internalId, regId, VatRegStatus.contact)
            VatRegStatus.contact
          case _                              => registration.status
        }
      case None               =>
        warnLog(s"[VatRegistrationService][getStatus] - No VAT registration document found for $regId")
        throw new InternalServerException(s"[VatRegistrationService] No VAT registration document found for $regId")
    }
}
