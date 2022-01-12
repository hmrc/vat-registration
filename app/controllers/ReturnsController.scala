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
import models.api.returns.Returns
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.ReturnsService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ReturnsController @Inject()(returnsService: ReturnsService,
                                  val authConnector: AuthConnector,
                                  controllerComponents: ControllerComponents
                                 )(implicit executionContext: ExecutionContext)
  extends BackendController(controllerComponents) with Authorisation {

  val resourceConn: AuthorisationResource = returnsService.registrationRepository

  def getReturns(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "ReturnsController", "getReturns") {
          returnsService.retrieveReturns(regId)
            .sendResult("getReturns", regId)
        }
      }
  }

  def updateReturns(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "ReturnsController", "updateReturns") {
          withJsonBody[Returns] { returns =>
            returnsService.updateReturns(regId, returns)
              .sendResult("updateReturns", regId)
          }
        }
      }
  }
}
