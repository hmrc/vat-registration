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

import cats.data.OptionT
import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import common.exceptions._
import config.BackendConfig
import enums.VatRegStatus
import models.api.VatScheme
import play.api.Logging
import repositories.VatSchemeRepository
import uk.gov.hmrc.http.HttpClient

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class VatRegistrationService @Inject()(registrationRepository: VatSchemeRepository,
                                       val backendConfig: BackendConfig,
                                       val http: HttpClient) extends ApplicativeSyntax with FutureInstances with Logging {

  def getStatus(regId: String): Future[VatRegStatus.Value] = {
    registrationRepository.retrieveVatScheme(regId) map {
      case Some(registration) =>
        registration.status
      case None =>
        logger.warn(s"[getStatus] - No VAT registration document found for $regId")
        throw new MissingRegDocument(regId)
    }
  }

  def retrieveVatScheme(regId: String): ServiceResult[VatScheme] =
    OptionT(registrationRepository.retrieveVatScheme(regId)).toRight(ResourceNotFound(regId))

  def retrieveVatSchemeByInternalId(internalId: String): ServiceResult[VatScheme] =
    OptionT(registrationRepository.retrieveVatSchemeByInternalId(internalId)).toRight(ResourceNotFound(internalId))
}
