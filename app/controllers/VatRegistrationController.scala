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

package controllers

import javax.inject.Inject

import auth.Authorisation
import cats.instances.FutureInstances
import common.RegistrationId
import common.exceptions.{InvalidSubmissionStatus, LeftState}
import config.AuthClientConnector
import enums.VatRegStatus
import models.api._
import play.api.libs.json._
import play.api.mvc._
import repositories.{RegistrationMongo, RegistrationMongoRepository}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.Upstream5xxResponse
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController
import utils.VATFeatureSwitches

import scala.concurrent.Future

class VatRegistrationControllerImpl @Inject()(val registrationService: RegistrationService,
                                              val submissionService: SubmissionService,
                                              val registrationMongo: RegistrationMongo) extends VatRegistrationController {

  val registrationRepository: RegistrationMongoRepository = registrationMongo.store
  val resourceConn = submissionService.registrationRepository
  override lazy val authConnector:AuthConnector = AuthClientConnector
  private[controllers] override def useMockSubmission: Boolean = VATFeatureSwitches.mockSubmission.enabled
}

trait VatRegistrationController extends BaseController with Authorisation with FutureInstances {

  val authConnector: AuthConnector
  val registrationService: RegistrationService
  val submissionService: SubmissionService
  val registrationRepository: RegistrationMongoRepository

  private[controllers] def useMockSubmission: Boolean

  val errorHandler: (LeftState) => Result = err => err.toResult

  def newVatRegistration: Action[AnyContent] = Action.async {
    implicit request =>
      isAuthenticated { internalId =>
        implicit val writes = VatScheme.apiWrites
        registrationService.createNewRegistration(internalId)
          .fold(errorHandler, vatScheme => Created(Json.toJson(vatScheme)))
      }
  }

  def retrieveVatScheme(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(id.value) { authResult =>
        authResult.ifAuthorised(id.value, "VatRegistrationController", "retrieveVatScheme") {
          implicit val writes = VatScheme.apiWrites
          registrationService.retrieveVatScheme(id).fold(errorHandler, vatScheme => Ok(Json.toJson(vatScheme)))
        }
      }
  }

// TODO: this returns 404 when other methods return 204. Refactor to return 204 at some point
  def fetchReturns(regId: String) : Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "fetchReturns") {
          registrationRepository.fetchReturns(regId) map {
            case Some(returns) => Ok(Json.toJson(returns))
            case None => NotFound
          }
        }
      }
  }

  def updateReturns(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "updateReturns") {
          withJsonBody[Returns] { returns =>
            registrationRepository.updateReturns(regId, returns) map (_ => Ok)
          }
        }
      }
  }

  def submitVATRegistration(id: RegistrationId) : Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(id.value) { authResult =>
        authResult.ifAuthorised(id.value, "VatRegistrationController", "submitVATRegistration") {
          if (!useMockSubmission) {
            submissionService.submitVatRegistration(id).map { ackRefs =>
              Ok(Json.toJson(ackRefs))
            } recover {
              case ex: Upstream5xxResponse => BadGateway(s"Timeout received when attempting to submit: ${ex.getMessage}")
              case ex => BadRequest(s"Registration was submitted without full data: ${ex.getMessage}")
            }
          } else {
            Future.successful(Ok(Json.toJson("BRVT000000" + id)))
          }
        }
      }
  }

  def getAcknowledgementReference(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(id.value) { authResult =>
        authResult.ifAuthorised(id.value, "VatRegistrationController", "getAcknowledgementReference") {
          if (!useMockSubmission) {
            submissionService.getAcknowledgementReference(id).fold(errorHandler, ackRefNumber => Ok(Json.toJson(ackRefNumber)))
          } else {
            Future.successful(Ok(Json.toJson("BRVT000000" + id)))
          }
        }
      }
  }

  def fetchTurnoverEstimates(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "fetchTurnoverEstimates") {
          registrationRepository.fetchTurnoverEstimates(regId) sendResult("fetchTurnoverEstimates", regId)
        }
      }
  }

  def updateTurnoverEstimates(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "updateTurnoverEstimates") {
          withJsonBody[TurnoverEstimates] { turnoverEstimates =>
            registrationRepository.updateTurnoverEstimates(regId, turnoverEstimates) map (_ => Ok)
          }
        }
      }
  }

  def deleteVatScheme(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "deleteVatScheme") {
          registrationService.deleteVatScheme(regId, VatRegStatus.draft, VatRegStatus.rejected) map { deleted =>
            if (deleted) Ok else InternalServerError
          } recover {
            case _: InvalidSubmissionStatus => PreconditionFailed
          }
        }
      }
  }

  def clearDownDocument(transId: String): Action[AnyContent] = Action.async {
    implicit request =>
      registrationService.clearDownDocument(transId).map {
        case true => Ok
        case _ => InternalServerError
      }
  }

  // TODO: this returns 404 when other methods return 204. Refactor to return 204 at some point
  def fetchBankAccountDetails(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "fetchBankAccountDetails") {
          registrationRepository.fetchBankAccount(regId) map {
            case Some(bankAccount) => Ok(Json.toJson(bankAccount))
            case None => NotFound
          }
        }
      }
  }

  def updateBankAccountDetails(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "updateBankAccountDetails") {
          withJsonBody[BankAccount] { bankAccount =>
            registrationRepository.updateBankAccount(regId, bankAccount)
              .sendResult("updateBackAccountDetails",regId)
          }
        }
      }
  }

  def getDocumentStatus(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(id.value) { authResult =>
        authResult.ifAuthorised(id.value, "VatRegistrationController", "getDocumentStatus") {
          registrationService.getStatus(id).sendResult("getDocumentStatus", id.value)
        }
      }
  }

  def saveTransId(id: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      withJsonBody[JsValue] { json =>
        val transId: String = (json \ "transactionID").as[String]
        registrationRepository.saveTransId(transId, RegistrationId(id)) map { _ =>
          Ok
        }
      }
  }
}