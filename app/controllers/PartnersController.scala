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
import models.api.Partner
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import repositories.RegistrationMongoRepository
import services.PartnersService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnersController @Inject()(val authConnector: AuthConnector,
                                   mongo: RegistrationMongoRepository,
                                   partnersService: PartnersService,
                                   controllerComponents: ControllerComponents)
                                  (implicit ec: ExecutionContext) extends BackendController(controllerComponents) with Authorisation {

  override val resourceConn: AuthorisationResource = mongo
  private val minAllowableIndex = 1

  def getPartner(regId: String, index: Int): Action[AnyContent] = Action.async { implicit request =>
    isAuthorised(regId) { authResult =>
      authResult.ifAuthorised(regId, "PartnersController", "getPartner") {
        if (index < minAllowableIndex) {
          Future.successful(BadRequest)
        } else {
          partnersService.getPartner(regId, index).map {
            case Some(partner) =>
              Ok(Json.toJson(partner))
            case _ =>
              NotFound
          }
        }
      }
    }
  }

  def upsertPartner(regId: String, index: Int): Action[Partner] = Action.async(parse.json[Partner]) { implicit request =>
    isAuthorised(regId) { authResult =>
      authResult.ifAuthorised(regId, "PartnersController", "storePartner") {
        if (index < minAllowableIndex) {
          Future.successful(BadRequest)
        } else {
          partnersService.storePartner(regId, request.body, index)
            .map(partner => Created(Json.toJson(partner)))
        }
      }
    }
  }

  def getPartners(regId: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthorised(regId) { authResult =>
      authResult.ifAuthorised(regId, "PartnersController", "getAllPartners") {
        partnersService.getPartners(regId).map {
          case Some(partners) => Ok(Json.toJson(partners))
          case _ => NotFound
        }
      }
    }
  }

  def deletePartner(regId: String, index: Int): Action[AnyContent] = Action.async { implicit request =>
    isAuthorised(regId) { authResult =>
      authResult.ifAuthorised(regId, "PartnersController", "deletePartner") {
        if (index < minAllowableIndex) {
          Future.successful(BadRequest)
        } else {
          partnersService.deletePartner(regId, index).map(_ => NoContent)
        }
      }
    }
  }

}
