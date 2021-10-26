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

package controllers

import auth.{Authorisation, AuthorisationResource}
import models.api.UpscanDetails
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.VatSchemeRepository
import services.UpscanService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class UpscanController @Inject()(controllerComponents: ControllerComponents,
                                 upscanService: UpscanService,
                                 registrationRepository: VatSchemeRepository,
                                 val authConnector: AuthConnector
                                )(implicit ec: ExecutionContext)
  extends BackendController(controllerComponents) with Authorisation {

  override val resourceConn: AuthorisationResource = registrationRepository

  def createUpscanDetails(regId: String): Action[String] = Action.async(parse.json[String]) { implicit request =>
    isAuthorised(regId) { authResult =>
      authResult.ifAuthorised(regId, "UpscanController", "createUpscanDetails") {
        upscanService.createUpscanDetails(regId, request.body).map(_ => Ok)
      }
    }
  }

  def getUpscanDetails(regId: String, reference: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthorised(regId) { authResult =>
      authResult.ifAuthorised(regId, "UpscanController", "getUpscanDetails") {
        upscanService.getUpscanDetails(reference).map {
          case Some(upscanDetails) => Ok(Json.toJson(upscanDetails))
          case None => NotFound
        }
      }
    }
  }

  def upscanDetailsCallback: Action[UpscanDetails] = Action.async(parse.json[UpscanDetails]) { implicit request =>
    upscanService.getUpscanDetails(request.body.reference).flatMap {
      case Some(details) =>
        val updatedDetails = request.body.copy(registrationId = details.registrationId)
        upscanService.upsertUpscanDetails(updatedDetails).map(_ => Ok)
      case None =>
        throw new InternalServerException("[UpscanController] Callback attempted to update non-existant UpscanDetails")
    }
  }

}
