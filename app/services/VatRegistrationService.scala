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

import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import common.exceptions._
import config.BackendConfig
import enums.VatRegStatus
import play.api.Logging
import repositories.VatSchemeRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatRegistrationService @Inject()(registrationRepository: VatSchemeRepository,
                                       val backendConfig: BackendConfig)
                                      (implicit executionContext: ExecutionContext)
  extends ApplicativeSyntax with FutureInstances with Logging {

  def getStatus(internalId: String, regId: String): Future[VatRegStatus.Value] = {
    registrationRepository.getRegistration(internalId, regId) map {
      case Some(registration) =>
        List(
          registration.applicantDetails.flatMap(_.personalDetails.score),
          registration.transactorDetails.flatMap(_.personalDetails.score),
          registration.applicantDetails.flatMap(_.contact.email).map(email => if (backendConfig.emailCheck.exists(email.endsWith)) 100 else 0),
          registration.transactorDetails.map(_.email).map(email => if (backendConfig.emailCheck.exists(email.endsWith)) 100 else 0)
        ).flatten match {
          case scores if scores.contains(100) =>
            registrationRepository.updateSubmissionStatus(internalId, regId, VatRegStatus.contact)
            VatRegStatus.contact
          case _ => registration.status
        }
      case None =>
        logger.warn(s"[getStatus] - No VAT registration document found for $regId")
        throw MissingRegDocument(regId)
    }
  }
}
