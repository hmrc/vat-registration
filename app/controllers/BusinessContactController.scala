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

import auth.Authorisation
import javax.inject.{Inject, Singleton}
import models.api.BusinessContact
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.RegistrationMongoRepository
import services.BusinessContactService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class BusinessContactController @Inject()(val businessContactService: BusinessContactService,
                                          val authConnector: AuthConnector,
                                          controllerComponents: ControllerComponents
                                         ) extends BackendController(controllerComponents) with Authorisation {

  val resourceConn: RegistrationMongoRepository = businessContactService.registrationRepository

  def getBusinessContact(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "BusinessContactController", "getBusinessContact") {
          businessContactService.getBusinessContact(regId) sendResult("getBusinessContact", regId)
        }
      }
  }

  def updateBusinessContact(regId: String): Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "BusinessContactController", "updateBusinessContact") {
          withJsonBody[BusinessContact] { businessCont =>
            businessContactService.updateBusinessContact(regId, businessCont) sendResult("updateBusinessContact", regId)
          }
        }
      }
  }
}