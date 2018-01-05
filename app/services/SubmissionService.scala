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

import java.time.LocalDate
import javax.inject.{Inject, Singleton}

import cats.instances.FutureInstances
import common.exceptions._
import common.{RegistrationId, TransactionId}
import config.MicroserviceAuditConnector
import connectors.{CompanyRegistrationConnector, DESConnector, IncorporationInformationConnector}
import enums.VatRegStatus
import models.api.VatScheme
import models.external.{IncorpStatus, IncorporationStatus}
import models.submission.{DESSubmission, TopUpSubmission}
import org.joda.time.DateTime
import repositories._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class SubmissionService @Inject()(val sequenceMongo: SequenceMongo,
                                  val vatRegistrationService: VatRegistrationService,
                                  val registrationMongo: RegistrationMongo,
                                  val companyRegistrationConnector: CompanyRegistrationConnector,
                                  val desConnector: DESConnector,
                                  val incorporationInformationConnector : IncorporationInformationConnector) extends SubmissionSrv {
  val auditConnector = MicroserviceAuditConnector
  val registrationRepository = registrationMongo.store
  val sequenceRepository = sequenceMongo.store
}

trait SubmissionSrv extends FutureInstances {

  val sequenceRepository: SequenceRepository
  val vatRegistrationService: VatRegistrationService
  val registrationRepository : RegistrationRepository
  val companyRegistrationConnector : CompanyRegistrationConnector
  val desConnector: DESConnector
  val incorporationInformationConnector: IncorporationInformationConnector
  val auditConnector: AuditConnector

  private val REGIME = "vat"
  private val SUBSCRIBER = "scrs"

  def submitVatRegistration(regId : RegistrationId)(implicit hc : HeaderCarrier) : Future[String] = {
    for {
      _             <- getValidDocumentStatus(regId)
      ackRefs       <- ensureAcknowledgementReference(regId)
      transID       <- fetchCompanyRegistrationTransactionID(regId)
      incorpStatus  <- registerForInterest(transID)
      incorpDate    =  incorpStatus.map(status => getIncorpDate(status))
      companyName   <- getCompanyName(regId, TransactionId(transID))
      submission    <- buildDesSubmission(regId, ackRefs, companyName, incorpDate)
      _             <- desConnector.submitToDES(submission, regId.toString)
      _             <- updateSubmissionStatus(regId, incorpDate)
    } yield {
      ackRefs
    }
  }

  def submitTopUpVatRegistration(incorpUpdate: IncorpStatus)(implicit hc : HeaderCarrier): Future[Boolean] = {
    for {
      regId         <- getRegistrationIDByTxId(incorpUpdate.transactionId)
      ackRefs       <- ensureAcknowledgementReference(regId)
      submission    <- buildTopUpSubmission(regId, ackRefs, incorpUpdate.status, incorpUpdate.incorporationDate)
      _             <- desConnector.submitTopUpToDES(submission, regId.toString)
      _             <- updateTopUpSubmissionStatus(regId, incorpUpdate.status)
    } yield true
  }

  private[services] def getRegistrationIDByTxId(transactionId: String)(implicit hc: HeaderCarrier): Future[RegistrationId] = {
    registrationRepository.fetchRegByTxId(transactionId) map {
      case Some(vs) => vs.id
      case None => throw NoVatSchemeWithTransId(TransactionId(transactionId))
    }
  }

  def getAcknowledgementReference(id: RegistrationId)(implicit hc: HeaderCarrier): ServiceResult[String] =
    vatRegistrationService.retrieveAcknowledgementReference(id)

  private[services] def generateAcknowledgementReference(implicit hc: HeaderCarrier): Future[String] =
    sequenceRepository.getNext("AcknowledgementID").map(ref => f"BRVT$ref%011d")

  private[services] def ensureAcknowledgementReference(regId: RegistrationId)(implicit hc : HeaderCarrier, ec: ExecutionContext): Future[String] = {
    registrationRepository.retrieveVatScheme(regId) flatMap {
      case Some(vs) => vs.acknowledgementReference.fold(
        for {
          newAckref   <- generateAcknowledgementReference
          _           <- registrationRepository.prepareRegistrationSubmission(regId, newAckref)
        } yield newAckref
      )(ar => Future.successful(ar))
      case _ => throw new MissingRegDocument(regId)
    }
  }

  private[services] def retrieveVatStartDate(vatScheme: VatScheme, regId: RegistrationId) : Option[LocalDate] = {
    vatScheme.returns match {
      case Some(returns) => returns.start.date
      case None => throw NoReturns()
    }
  }

  private[services] def buildDesSubmission(regId: RegistrationId, ackRef: String, companyName: String, incorpDate: Option[LocalDate])
                                          (implicit hc: HeaderCarrier): Future[DESSubmission] = {
    registrationRepository.retrieveVatScheme(regId) map {
      case Some(vs)     =>
        DESSubmission(
          ackRef,
          companyName,
          incorpDate.flatMap(_ => retrieveVatStartDate(vs, regId)),
          incorpDate
        )
      case None         => throw MissingRegDocument(regId)
    }
  }

  def buildTopUpSubmission(regId: RegistrationId, ackRef: String, status: String, incorpDate: Option[DateTime])
                          (implicit hc: HeaderCarrier) : Future[TopUpSubmission] = {
    registrationRepository.retrieveVatScheme(regId) map {
      case Some(vs) =>
        val startDate = retrieveVatStartDate(vs, regId)
        status match {
          case "accepted" => TopUpSubmission(ackRef, status, startDate, incorpDate)
          case "rejected" => TopUpSubmission(ackRef, status)
          case _ => throw UnknownIncorpStatus(s"Status of $status did not match recognised status for building a top up submission")
        }
      case None => throw MissingRegDocument(regId)
    }

  }

  private[services] def getValidDocumentStatus(regID : RegistrationId)(implicit hc : HeaderCarrier) : Future[String] = {
    registrationRepository.retrieveVatScheme(regID) map {
      case Some(registration) => registration.status match {
        case VatRegStatus.draft => registration.status.toString
        case _                  => throw new InvalidSubmissionStatus(s"VAT submission status was in a ${registration.status} state")
      }
      case None => throw new MissingRegDocument(regID)
    }
  }

  private[services] def fetchCompanyRegistrationTransactionID(regId: RegistrationId)(implicit hc: HeaderCarrier): Future[String] = {
    companyRegistrationConnector.fetchCompanyRegistrationDocument(regId) flatMap { response =>
      Try((response.json \ "confirmationReferences" \ "transaction-id").as[String]) match {
        case Success(transactionID) => saveTransId(transactionID, regId)
        case Failure(_)             => throw NoTransactionId(s"Fetching of transaction id failed for regId: $regId")
      }
    }
  }

  private[services] def saveTransId(transId: String, regId: RegistrationId)(implicit hc:HeaderCarrier): Future[String] = {
    registrationRepository.saveTransId(transId, regId)
  }

  private[services] def registerForInterest(transID: String)(implicit hc: HeaderCarrier): Future[Option[IncorporationStatus]] = {
    incorporationInformationConnector.retrieveIncorporationStatus(TransactionId(transID), REGIME, SUBSCRIBER)
  }

  private[services] def getIncorpDate(incorpStatus: IncorporationStatus): LocalDate = {
    incorpStatus.statusEvent.incorporationDate match {
      case Some(incorpDate) => incorpDate
      case None             => throw NoIncorpDate(s"No incorp date found in the incorpStatus for transID: ${incorpStatus.subscription.transactionId}")
    }
  }

  private[services] def updateSubmissionStatus(regId : RegistrationId, incorpDate : Option[LocalDate])
                                              (implicit hc : HeaderCarrier): Future[VatRegStatus.Value] = {
    registrationRepository.finishRegistrationSubmission(
      regId,
      if (incorpDate.isDefined) VatRegStatus.submitted else VatRegStatus.held
    )
  }

  private[services] def updateTopUpSubmissionStatus(regId : RegistrationId, status : String)
                                                   (implicit hc : HeaderCarrier): Future[VatRegStatus.Value] = {
    registrationRepository.finishRegistrationSubmission(
      regId,
      if (status == "accepted") VatRegStatus.submitted else VatRegStatus.rejected
    )
  }

  private[services] def getCompanyName(regId: RegistrationId, txId: TransactionId)(implicit hc: HeaderCarrier): Future[String] = {
    incorporationInformationConnector.getCompanyName(regId, txId) map { resp =>
      Try((resp.json \ "company_name").as[String]) match {
        case Success(companyName)   => companyName
        case Failure(_)             => throw NoCompanyName(s"Could not retrieve company name from II with regId: $regId and transId: $txId")
      }
    }
  }
}
