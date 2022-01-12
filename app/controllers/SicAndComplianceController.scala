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
import javax.inject.{Inject, Singleton}
import models.api.SicAndCompliance
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.SicAndComplianceService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class SicAndComplianceController @Inject()(val sicAndComplianceService: SicAndComplianceService,
                                               val authConnector: AuthConnector,
                                               controllerComponents: ControllerComponents)
                                               extends BackendController(controllerComponents) with Authorisation {

  val resourceConn: AuthorisationResource = sicAndComplianceService.registrationRepository

  def getSicAndCompliance(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "SicAndComplianceController", "getSicAndCompliance") {
          sicAndComplianceService.getSicAndCompliance(regId) sendResult("getSicAndCompliance", regId)
        }
      }
  }

  def updateSicAndCompliance(regId: String): Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "SicAndComplianceController", "updateSicAndCompliance") {
          withJsonBody[SicAndCompliance] { sicAndComp =>
            sicAndComplianceService.updateSicAndCompliance(regId, sicAndComp)
              .sendResult("updateSicAndCompliance", regId)
          }
        }
      }
  }
}
