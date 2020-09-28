/*
 * Copyright 2020 HM Revenue & Customs
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

import java.util.UUID

import cats.data.{EitherT, OptionT}
import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import common.exceptions._
import config.BackendConfig
import connectors._
import enums.VatRegStatus
import javax.inject.{Inject, Singleton}
import models.AcknowledgementReferencePath
import models.api.{Threshold, VatScheme}
import models.external.CurrentProfile
import org.slf4j.LoggerFactory
import play.api.libs.json._
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import utils.EligibilityDataJsonUtils

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatRegistrationService @Inject()(registrationRepository: RegistrationMongoRepository,
                                       val backendConfig: BackendConfig,
                                       val http: HttpClient) extends ApplicativeSyntax with FutureInstances {

  lazy val vatRestartUrl: String = backendConfig.servicesConfig.getString("api.vatRestartURL")
  lazy val vatCancelUrl: String = backendConfig.servicesConfig.getString("api.vatCancelURL")

  private val cancelStatuses: Seq[VatRegStatus.Value] = Seq(VatRegStatus.draft, VatRegStatus.invalid)
  private val logger = LoggerFactory.getLogger(getClass)

  private def repositoryErrorHandler[T]: PartialFunction[Throwable, Either[LeftState, T]] = {
    case e: MissingRegDocument  => Left(ResourceNotFound(s"No registration found for registration ID: ${e.id}"))
    case dbe: DBExceptions      => Left(GenericDatabaseError(dbe, Some(dbe.id)))
    case t: Throwable           => Left(GenericError(t))
  }

  private def getOrCreateVatScheme(registrationId: String, internalId:String)(implicit hc: HeaderCarrier): Future[Either[LeftState, VatScheme]] =
    registrationRepository.retrieveVatScheme(registrationId).flatMap {
      case Some(vatScheme) => Future.successful(Right(vatScheme))
      case None => registrationRepository.createNewVatScheme(registrationId,internalId)
        .map(Right(_)).recover(repositoryErrorHandler)
    }

  import cats.syntax.either._

  def saveAcknowledgementReference(regID: String, ackRef: String)(implicit hc: HeaderCarrier): ServiceResult[String] =
    OptionT(registrationRepository.retrieveVatScheme(regID)).toRight(ResourceNotFound(s"VatScheme ID: $regID missing"))
      .flatMap(vs => vs.acknowledgementReference match {
        case Some(ar) =>
          Left[LeftState, String](AcknowledgementReferenceExists(s"""Registration ID $regID already has an acknowledgement reference of: $ar""")).toEitherT
        case None =>
          EitherT.liftT[Future, LeftState, String](registrationRepository.updateByElement(regID, AcknowledgementReferencePath, ackRef))
      })

  def getStatus(regId: String)(implicit hc: HeaderCarrier): Future[JsValue] = {
    registrationRepository.retrieveVatScheme(regId) map {
      case Some(registration) =>
        val base = Json.obj(
          "status"     -> registration.status
        )

        val ackRef = registration.acknowledgementReference.fold(Json.obj())(ref => Json.obj("ackRef" -> ref))
        val restartUrl = if(registration.status.equals(VatRegStatus.rejected)) Json.obj("restartURL" -> vatRestartUrl) else Json.obj()
        val cancelUrl  = if(cancelStatuses.contains(registration.status)) Json.obj("cancelURL" -> vatCancelUrl.replace(":regID", regId)) else Json.obj()

        base ++ ackRef ++ restartUrl ++ cancelUrl
      case None =>
        logger.warn(s"[getStatus] - No VAT registration document found for ${regId}")
        throw new MissingRegDocument(regId)
    }
  }

  //TODO - confirm if this is correct
  def generateRegistrationId(): String = UUID.randomUUID().toString

  def createNewRegistration(intId: String)(implicit headerCarrier: HeaderCarrier): ServiceResult[VatScheme] =
    EitherT(getOrCreateVatScheme(generateRegistrationId(), intId))

  def retrieveVatScheme(regId: String)(implicit hc: HeaderCarrier): ServiceResult[VatScheme] =
    OptionT(registrationRepository.retrieveVatScheme(regId)).toRight(ResourceNotFound(regId))

  def retrieveVatSchemeByInternalId(internalId: String)(implicit hc: HeaderCarrier): ServiceResult[VatScheme] =
    OptionT(registrationRepository.retrieveVatSchemeByInternalId(internalId)).toRight(ResourceNotFound(internalId))

  def deleteVatScheme(regId: String, validStatuses: VatRegStatus.Value*)(implicit hc: HeaderCarrier): Future[Boolean] = {
    for {
      someDocument <- registrationRepository.retrieveVatScheme(regId)
      document     <- someDocument.fold(throw new MissingRegDocument(regId))(doc => Future.successful(doc))
      deleted      <- if (validStatuses.contains(document.status)) {
        registrationRepository.deleteVatScheme(regId)
      } else {
        throw new InvalidSubmissionStatus(s"[deleteVatScheme] - VAT reg doc for regId $regId was not deleted as the status was ${document.status}; not ${validStatuses.toString}")
      }
    } yield deleted
  }

  def clearDownDocument(transId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    registrationRepository.clearDownDocument(transId)
  }

  def retrieveAcknowledgementReference(regId: String)(implicit hc: HeaderCarrier): ServiceResult[String] = {
    retrieveVatScheme(regId).subflatMap(_.acknowledgementReference.toRight(ResourceNotFound("AcknowledgementId")))
  }

  def getBlockFromEligibilityData[T](regId: String)(implicit ex: ExecutionContext, r: Reads[T]): Future[Option[T]] = {
    registrationRepository.getEligibilityData(regId) map {
      _.map { js =>
        Json.fromJson[T](js)(EligibilityDataJsonUtils.mongoReads[T]).fold(invalid => {
          val errorMsg = s"Error converting EligibilityData to model ${r.asInstanceOf[T].getClass.getSimpleName} for regId: $regId, msg: $invalid"
          logger.warn(s"[VatRegistrationService] [getBlockFromEligibilityData] $errorMsg")
          throw new InvalidEligibilityDataToConvertModel(errorMsg)
        }, identity)
      }
    }
  }
}
