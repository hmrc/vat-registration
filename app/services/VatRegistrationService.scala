/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import cats.data.{EitherT, OptionT}
import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import common.exceptions._
import common.{LogicalGroup, RegistrationId}
import connectors._
import enums.VatRegStatus
import models.AcknowledgementReferencePath
import models.api.VatScheme
import models.external.CurrentProfile
import org.slf4j.LoggerFactory
import play.api.libs.json.{JsValue, Json, Writes}
import repositories.{RegistrationMongo, RegistrationRepository}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}

class VatRegistrationService @Inject()(val brConnector: BusinessRegistrationConnector,
                                       regMongo: RegistrationMongo) extends RegistrationService with ServicesConfig {

  val registrationRepository: RegistrationRepository = regMongo.store
  override lazy val vatRestartUrl = getString("api.vatRestartURL")
  override lazy val vatCancelUrl  = getString("api.vatCancelURL")
}

trait RegistrationService extends ApplicativeSyntax with FutureInstances {
  val registrationRepository: RegistrationRepository
  val brConnector: BusinessRegistrationConnector

  val vatRestartUrl: String
  val vatCancelUrl: String

  private val cancelStatuses: Seq[VatRegStatus.Value] = Seq(VatRegStatus.draft, VatRegStatus.invalid)

  private val logger = LoggerFactory.getLogger(getClass)

  private def repositoryErrorHandler[T]: PartialFunction[Throwable, Either[LeftState, T]] = {
    case e: MissingRegDocument  => Left(ResourceNotFound(s"No registration found for registration ID: ${e.id}"))
    case dbe: DBExceptions      => Left(GenericDatabaseError(dbe, Some(dbe.id.value)))
    case t: Throwable           => Left(GenericError(t))
  }

  private def toEitherT[T](eventualT: Future[T])(implicit ex: ExecutionContext) =
    EitherT[Future, LeftState, T](eventualT.map(Right(_)).recover(repositoryErrorHandler))

  private def getOrCreateVatScheme(profile: CurrentProfile, internalId:String)(implicit hc: HeaderCarrier): Future[Either[LeftState, VatScheme]] =
    registrationRepository.retrieveVatScheme(RegistrationId(profile.registrationID)).flatMap {
      case Some(vatScheme) => Future.successful(Right(vatScheme))
      case None => registrationRepository.createNewVatScheme(RegistrationId(profile.registrationID),internalId)
        .map(Right(_)).recover(repositoryErrorHandler)
    }

  import cats.syntax.either._

  def saveAcknowledgementReference(id: RegistrationId, ackRef: String)(implicit hc: HeaderCarrier): ServiceResult[String] =
    OptionT(registrationRepository.retrieveVatScheme(id)).toRight(ResourceNotFound(s"VatScheme ID: $id missing"))
      .flatMap(vs => vs.acknowledgementReference match {
        case Some(ar) =>
          Left[LeftState, String](AcknowledgementReferenceExists(s"""Registration ID $id already has an acknowledgement reference of: $ar""")).toEitherT
        case None =>
          EitherT.liftT[Future, LeftState, String](registrationRepository.updateByElement(id, AcknowledgementReferencePath, ackRef))
      })

  def getStatus(regId: RegistrationId)(implicit hc: HeaderCarrier): Future[JsValue] = {
    registrationRepository.retrieveVatScheme(regId) map {
      case Some(registration) =>
        //TODO: Refactor with API changes
        val lastUpdate = registration.status match {
          case VatRegStatus.held                                 => "TIMESTAMP" //Partial timestamp
          case VatRegStatus.submitted                            => "TIMESTAMP" //Full timestamp
          case VatRegStatus.cancelled                            => "TIMESTAMP" //Last modified
          case VatRegStatus.acknowledged | VatRegStatus.rejected => "TIMESTAMP" //Acknowledged timestamp
          case _                                                 => "TIMESTAMP" //Form creation timestamp
        }

        val base = Json.obj(
          "status"     -> registration.status
        )

        val ackRef = registration.acknowledgementReference.fold(Json.obj())(ref => Json.obj("ackRef" -> ref))

        val vrn = Json.obj() //Placeholder for VRN

        val restartUrl = if(registration.status.equals(VatRegStatus.rejected)) Json.obj("restartURL" -> vatRestartUrl) else Json.obj()
        val cancelUrl  = if(cancelStatuses.contains(registration.status)) Json.obj("cancelURL" -> vatCancelUrl.replace(":regID", regId.value)) else Json.obj()

        base ++ ackRef ++ restartUrl ++ cancelUrl
      case None =>
        logger.warn(s"[getStatus] - No VAT registration document found for ${regId.value}")
        throw new MissingRegDocument(regId)
    }
  }

  def createNewRegistration(intId:String)(implicit headerCarrier: HeaderCarrier): ServiceResult[VatScheme] =
    for {
      profile <- EitherT(brConnector.retrieveCurrentProfile)
      vatScheme <- EitherT(getOrCreateVatScheme(profile,intId))
    } yield vatScheme

  def retrieveVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): ServiceResult[VatScheme] =
    OptionT(registrationRepository.retrieveVatScheme(id)).toRight(ResourceNotFound(id.value))

  def updateLogicalGroup[G: LogicalGroup : Writes](id: RegistrationId, group: G)(implicit ec: ExecutionContext): ServiceResult[G] =
    toEitherT(registrationRepository.updateLogicalGroup(id, group))

  def deleteVatScheme(regId: String, validStatuses: VatRegStatus.Value*)(implicit hc: HeaderCarrier): Future[Boolean] = {
    for {
      someDocument <- registrationRepository.retrieveVatScheme(RegistrationId(regId))
      document     <- someDocument.fold(throw new MissingRegDocument(RegistrationId(regId)))(doc => Future.successful(doc))
      deleted      <- if(validStatuses.contains(document.status)) {
        registrationRepository.deleteVatScheme(regId)
      } else {
        throw new InvalidSubmissionStatus(s"[deleteVatScheme] - VAT reg doc for regId $regId was not deleted as the status was ${document.status}; not ${validStatuses.toString}")
      }
    } yield deleted
  }

  def clearDownDocument(transId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    registrationRepository.clearDownDocument(transId)
  }

  def retrieveAcknowledgementReference(id: RegistrationId)(implicit hc: HeaderCarrier): ServiceResult[String] = {
    retrieveVatScheme(id).subflatMap(_.acknowledgementReference.toRight(ResourceNotFound("AcknowledgementId")))
  }

}
