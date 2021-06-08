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
import models.api.{RegistrationChannel, RegistrationStatus}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.{Allocated, QuotaReached, TrafficManagementService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TrafficManagementController @Inject()(controllerComponents: ControllerComponents,
                                            trafficManagementService: TrafficManagementService,
                                            val authConnector: AuthConnector)
                                           (implicit ec: ExecutionContext)
  extends BackendController(controllerComponents) with Authorisation {

  override val resourceConn: AuthorisationResource = trafficManagementService.trafficManagementRepository

  def allocate(regId: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      trafficManagementService.allocate(internalId, regId) map {
        case Allocated => Created
        case QuotaReached => TooManyRequests
      }
    }
  }

  def getRegistrationInformation: Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      trafficManagementService.getRegistrationInformation(internalId) map {
        case Some(regInfo) =>
          Ok(Json.toJson(regInfo))
        case _ =>
          NotFound
      }
    }
  }

  def upsertRegistrationInformation(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    isAuthenticated { internalId =>
      val regId = (request.body \ "registrationId").as[String]
      val status = (request.body \ "status").as[RegistrationStatus]
      val regStartDate = (request.body \ "regStartDate").as[LocalDate]
      val channel = (request.body \ "channel").as[RegistrationChannel]

      trafficManagementService.upsertRegistrationInformation(internalId, regId, status, regStartDate, channel) map {
        regInfo =>
          Ok(Json.toJson(regInfo))
      }
    }
  }

  def clearDocument(): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      trafficManagementService.clearDocument(internalId) map {
        case true => NoContent
        case _ => PreconditionFailed
      }
    }
  }

}
