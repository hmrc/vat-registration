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

package controllers

import auth.Authorisation
import models.api.{UpscanCreate, UpscanDetails}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.UpscanService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpscanController @Inject() (
  controllerComponents: ControllerComponents,
  upscanService: UpscanService,
  val authConnector: AuthConnector
)(implicit val executionContext: ExecutionContext)
    extends BackendController(controllerComponents)
    with Authorisation {

  def createUpscanDetails(regId: String): Action[UpscanCreate] = Action.async(parse.json[UpscanCreate]) {
    implicit request =>
      isAuthenticated { _ =>
        upscanService.createUpscanDetails(regId, request.body).map(_ => Ok)
      }
  }

  def getUpscanDetails(regId: String, reference: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { _ =>
      infoLog(
        s"[UpscanController][getUpscanDetails] attempting to get upscan details regId: $regId reference $reference"
      )
      upscanService.getUpscanDetails(reference).map {
        case Some(upscanDetails) =>
          infoLog(
            s"[UpscanController][getUpscanDetails] successfully retrieved upscan details from mongo. regID $regId reference: $reference"
          )
          Ok(Json.toJson(upscanDetails))
        case None                =>
          warnLog(
            s"[UpscanController][getUpscanDetails] unable to retrieve upscan details from mongo. regID $regId reference: $reference"
          )
          NotFound
      }
    }
  }

  def getAllUpscanDetails(regId: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { _ =>
      upscanService.getAllUpscanDetails(regId).map(upscanDetails => Ok(Json.toJson(upscanDetails)))
    }
  }

  def upscanDetailsCallback: Action[UpscanDetails] = Action.async(parse.json[UpscanDetails]) { implicit request =>
    upscanService.getUpscanDetails(request.body.reference).flatMap {
      case Some(details) =>
        infoLog(
          s"[UpscanController][upscanDetailsCallback] upscan details successfully retrieved. Attempting to update with callback details. " +
            s"regId: ${request.body.registrationId} reference: ${request.body.reference}"
        )
        val updatedDetails = request.body.copy(registrationId = details.registrationId)
        upscanService.upsertUpscanDetails(updatedDetails).map(_ => Ok)
      case None =>
        errorLog(
          s"[UpscanController][upscanDetailsCallback] Callback attempted to update non-existent UpscanDetails. " +
            s"regId: ${request.body.registrationId} reference: ${request.body.reference}"
        )
        Future.successful(NotFound)
    }
  }

  def deleteUpscanDetails(regId: String, reference: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { _ =>
      infoLog(
        s"[UpscanController][deleteUpscanDetails] attempting to delete upscan details. regId: $regId reference: $reference"
      )
      upscanService.deleteUpscanDetails(reference).map { _ =>
        NoContent
      }
    }
  }

  def deleteAllUpscanDetails(regId: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { _ =>
      upscanService.deleteAllUpscanDetails(regId).map { _ =>
        NoContent
      }
    }
  }
}
