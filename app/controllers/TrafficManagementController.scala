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
import models.submission.PartyType
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.{Allocated, QuotaReached, TrafficManagementService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TrafficManagementController @Inject()(controllerComponents: ControllerComponents,
                                            trafficManagementService: TrafficManagementService,
                                            val authConnector: AuthConnector)
                                           (implicit ec: ExecutionContext)
  extends BackendController(controllerComponents) with Authorisation {

  override val resourceConn: AuthorisationResource = trafficManagementService.trafficManagementRepository

  def allocate(regId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    isAuthenticated { internalId =>
      val optPartyType = (request.body \ "partyType").validate[PartyType].asOpt
      val optIsEnrolled = (request.body \ "isEnrolled").validate[Boolean].asOpt

      (optPartyType, optIsEnrolled) match {
        case (Some(partyType), Some(isEnrolled)) =>
          trafficManagementService.allocate(internalId, regId, partyType, isEnrolled) map {
            case Allocated => Created
            case QuotaReached => TooManyRequests
          }
        case _ => Future.successful(BadRequest)
      }
    }
  }

  @deprecated("Use getRegInfoById method", "0.332.0")
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

  def getRegInfoById(registrationId: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      trafficManagementService.getRegInfoById(internalId, registrationId) map {
        case Some(regInfo) =>
          Ok(Json.toJson(regInfo))
        case _ =>
          NotFound
      }
    }
  }

  @deprecated("Use upsertRegInfoById method", "0.332.0")
  def upsertRegistrationInformation(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    isAuthenticated { internalId =>
      val regId = (request.body \ "registrationId").as[String]
      val status = (request.body \ "status").as[RegistrationStatus]
      val regStartDate = (request.body \ "regStartDate").as[LocalDate]
      val channel = (request.body \ "channel").as[RegistrationChannel]

      trafficManagementService.upsertRegInfo(internalId, regId, status, regStartDate, channel) map {
        regInfo =>
          Ok(Json.toJson(regInfo))
      }
    }
  }

  def upsertRegInfoById(registrationId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    isAuthenticated { internalId =>
      val optStatus = (request.body \ "status").asOpt[RegistrationStatus]
      val optRegStartDate = (request.body \ "regStartDate").asOpt[LocalDate]
      val optChannel = (request.body \ "channel").asOpt[RegistrationChannel]

      (optStatus, optRegStartDate, optChannel) match {
        case (Some(status), Some(startDate), Some(channel)) =>
          trafficManagementService
            .upsertRegInfoById(internalId, registrationId, status, startDate, channel)
            .map { regInfo =>
              Ok(Json.toJson(regInfo))
            }
        case _ =>
          Future.successful(BadRequest)
      }
    }
  }

  @deprecated("Use deleteRegInfoById method", "0.332.0")
  def clearDocument(): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      trafficManagementService.clearDocument(internalId) map {
        case true => NoContent
        case _ => PreconditionFailed
      }
    }
  }

  def deleteRegInfoById(registrationId: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      trafficManagementService.deleteRegInfoById(internalId, registrationId) map {
        case true => NoContent
        case _ => PreconditionFailed
      }
    }
  }

}
