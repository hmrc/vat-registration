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

package controllers

import auth.{Authorisation, AuthorisationResource}
import cats.instances.FutureInstances
import common.exceptions.{InvalidSubmissionStatus, LeftState}
import enums.VatRegStatus
import enums.VatRegStatus._
import models.api._
import play.api.libs.json._
import play.api.mvc._
import repositories.VatSchemeRepository
import services._
import services.submission.SubmissionService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatRegistrationController @Inject()(val registrationService: VatRegistrationService,
                                          val submissionService: SubmissionService,
                                          val registrationRepository: VatSchemeRepository,
                                          val authConnector: AuthConnector,
                                          val newRegistrationService: RegistrationService,
                                          controllerComponents: ControllerComponents
                                         )(implicit executionContext: ExecutionContext)
  extends BackendController(controllerComponents) with Authorisation with FutureInstances {


  override val resourceConn: AuthorisationResource = registrationRepository
  val errorHandler: LeftState => Result = err => err.toResult

  def newVatRegistration(implicit format: Format[VatScheme] = VatScheme.format()): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      newRegistrationService.newRegistration(internalId) map { scheme =>
        Created(Json.toJson(scheme))
      } recover {
        case _ => InternalServerError(
          "[VatRegistrationController][newVatRegistration] Unexpected error when creating new registration"
        )
      }
    }
  }

  def insertVatScheme(implicit format: Format[VatScheme] = VatScheme.format()): Action[VatScheme] = Action.async(parse.json[VatScheme]) { implicit request =>
    isAuthenticated { _ =>
      newRegistrationService.insertVatScheme(request.body).map { vatScheme =>
        Created(Json.toJson(vatScheme))
      }
    }
  }

  def retrieveVatScheme(id: String)(implicit format: Format[VatScheme] = VatScheme.format()): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(id) { authResult =>
        authResult.ifAuthorised(id, "VatRegistrationController", "retrieveVatScheme") {
          registrationService.retrieveVatScheme(id).fold(errorHandler, vatScheme => Ok(Json.toJson(vatScheme)))
        }
      }
  }

  def retrieveVatSchemeByInternalId(): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthenticated { internalId =>
        implicit val writes: OWrites[VatScheme] = VatScheme.writes()

        registrationService.retrieveVatSchemeByInternalId(internalId).fold(errorHandler, vatScheme => Ok(Json.toJson(vatScheme)))
      }
  }

  def submitVATRegistration(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthenticated { internalId =>
        val userHeaders = (request.body \ "userHeaders").asOpt[Map[String, String]].getOrElse(Map.empty)

        registrationService.getStatus(regId).flatMap {
          case `locked` => Future.successful(TooManyRequests(Json.obj()))
          case `submitted` => Future.successful(Ok(Json.obj()))
          case `duplicateSubmission` => Future.successful(Conflict(Json.obj()))
          case _ => submissionService.submitVatRegistration(internalId, regId, userHeaders).map { _ =>
            Ok(Json.obj())
          }
        }
      }
  }

  def getAcknowledgementReference(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "getAcknowledgementReference") {
          registrationService.retrieveAcknowledgementReference(regId).fold(errorHandler, ackRefNumber => Ok(Json.toJson(ackRefNumber)))
        }
      }
  }

  def getTurnoverEstimates(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "getTurnoverEstimates") {
          registrationService.getTurnoverEstimates(regId) sendResult("getTurnoverEstimates", regId)
        }
      }
  }

  def getThreshold(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "getThreshold") {
          registrationService.getThreshold(regId) sendResult("getThreshold", regId)
        }
      }
  }

  def deleteVatScheme(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "deleteVatScheme") {
          registrationService.deleteVatScheme(regId, VatRegStatus.draft) map { deleted =>
            if (deleted) Ok else InternalServerError
          } recover {
            case _: InvalidSubmissionStatus => PreconditionFailed
          }
        }
      }
  }

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
              .sendResult("updateBackAccountDetails", regId)
          }
        }
      }
  }

  def getDocumentStatus(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "VatRegistrationController", "getDocumentStatus") {
          registrationService.getStatus(regId).sendResult("getDocumentStatus", regId)
        }
      }
  }

  def storeHonestyDeclaration(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      withJsonBody[JsValue] { json =>
        val honestyDeclarationStatus: Boolean = (json \ "honestyDeclaration").as[Boolean]
        registrationService.storeHonestyDeclaration(regId, honestyDeclarationStatus).map {
          _ => Ok
        }
      }
  }

}
