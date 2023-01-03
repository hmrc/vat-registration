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
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc.{Action, ControllerComponents}
import services.EligibilityService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class EligibilityController @Inject()(val eligibilityService: EligibilityService,
                                      val authConnector: AuthConnector,
                                      controllerComponents: ControllerComponents
                                     ) extends BackendController(controllerComponents) with Authorisation {

  def updateEligibilityData(regId: String): Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      isAuthenticated { internalId =>
        withJsonBody[JsObject] { eligibilityData =>
          eligibilityService.updateEligibilityData(internalId, regId, eligibilityData).map {
            case Some(json) => Ok(json)
            case None => NotFound
          }
        }
      }
  }
}
