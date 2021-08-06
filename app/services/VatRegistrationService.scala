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

package services

import cats.data.{EitherT, OptionT}
import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import common.exceptions._
import config.BackendConfig
import enums.VatRegStatus
import models.api.{Threshold, TurnoverEstimates, VatScheme}
import models.submission.PartyType
import org.slf4j.LoggerFactory
import play.api.libs.json._
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.http.{HttpClient, InternalServerException}

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class VatRegistrationService @Inject()(registrationRepository: RegistrationMongoRepository,
                                       val backendConfig: BackendConfig,
                                       val http: HttpClient) extends ApplicativeSyntax with FutureInstances {

  lazy val vatRestartUrl: String = backendConfig.servicesConfig.getString("api.vatRestartURL")
  lazy val vatCancelUrl: String = backendConfig.servicesConfig.getString("api.vatCancelURL")

  private val cancelStatuses: Seq[VatRegStatus.Value] = Seq(VatRegStatus.draft, VatRegStatus.invalid)
  private val logger = LoggerFactory.getLogger(getClass)

  private def repositoryErrorHandler[T]: PartialFunction[Throwable, Either[LeftState, T]] = {
    case e: MissingRegDocument => Left(ResourceNotFound(s"No registration found for registration ID: ${e.id}"))
    case dbe: DBExceptions => Left(GenericDatabaseError(dbe, Some(dbe.id)))
    case t: Throwable => Left(GenericError(t))
  }

  private def getOrCreateVatScheme(registrationId: String, internalId: String): Future[Either[LeftState, VatScheme]] =
    registrationRepository.retrieveVatScheme(registrationId).flatMap {
      case Some(vatScheme) => Future.successful(Right(vatScheme))
      case None => registrationRepository.createNewVatScheme(registrationId, internalId)
        .map(Right(_)).recover(repositoryErrorHandler)
    }

  def getStatus(regId: String): Future[JsValue] = {
    registrationRepository.retrieveVatScheme(regId) map {
      case Some(registration) =>
        val base = Json.obj(
          "status" -> registration.status
        )

        val ackRef = registration.acknowledgementReference.fold(Json.obj())(ref => Json.obj("ackRef" -> ref))
        val restartUrl = if (registration.status.equals(VatRegStatus.rejected)) Json.obj("restartURL" -> vatRestartUrl) else Json.obj()
        val cancelUrl = if (cancelStatuses.contains(registration.status)) Json.obj("cancelURL" -> vatCancelUrl.replace(":regID", regId)) else Json.obj()

        base ++ ackRef ++ restartUrl ++ cancelUrl
      case None =>
        logger.warn(s"[getStatus] - No VAT registration document found for ${regId}")
        throw new MissingRegDocument(regId)
    }
  }

  def generateRegistrationId(): String = UUID.randomUUID().toString

  def createNewRegistration(intId: String): ServiceResult[VatScheme] =
    EitherT(getOrCreateVatScheme(generateRegistrationId(), intId))

  def retrieveVatScheme(regId: String): ServiceResult[VatScheme] =
    OptionT(registrationRepository.retrieveVatScheme(regId)).toRight(ResourceNotFound(regId))

  def retrieveVatSchemeByInternalId(internalId: String): ServiceResult[VatScheme] =
    OptionT(registrationRepository.retrieveVatSchemeByInternalId(internalId)).toRight(ResourceNotFound(internalId))

  def deleteVatScheme(regId: String, validStatuses: VatRegStatus.Value*): Future[Boolean] = {
    for {
      someDocument <- registrationRepository.retrieveVatScheme(regId)
      document <- someDocument.fold(throw new MissingRegDocument(regId))(doc => Future.successful(doc))
      deleted <- if (validStatuses.contains(document.status)) {
        registrationRepository.deleteVatScheme(regId)
      } else {
        throw new InvalidSubmissionStatus(s"[deleteVatScheme] - VAT reg doc for regId $regId was not deleted as the status was ${document.status}; not ${validStatuses.toString}")
      }
    } yield deleted
  }

  def retrieveAcknowledgementReference(regId: String): ServiceResult[String] = {
    retrieveVatScheme(regId).subflatMap(_.acknowledgementReference.toRight(ResourceNotFound("AcknowledgementId")))
  }

  def getPartyType(regId: String): Future[Option[PartyType]] = {
    registrationRepository.retrieveVatScheme(regId).map(_.flatMap(_.partyType))
  }

  def getThreshold(regId: String): Future[Option[Threshold]] = {
    registrationRepository.fetchEligibilitySubmissionData(regId).map(_.map(_.threshold))
  }

  def getTurnoverEstimates(regId: String): Future[Option[TurnoverEstimates]] = {
    registrationRepository.fetchEligibilitySubmissionData(regId).map(_.map(_.estimates))
  }

  def storeHonestyDeclaration(regId: String, honestyDeclarationStatus: Boolean): Future[Boolean] = {
    registrationRepository.storeHonestyDeclaration(regId, honestyDeclarationStatus)
  }
}
