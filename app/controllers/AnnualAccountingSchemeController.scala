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
import models.api.AnnualAccountingScheme
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.AnnualAccountingService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class AnnualAccountingSchemeController @Inject()(annualAccountingService: AnnualAccountingService,
                                                 val authConnector: AuthConnector,
                                                 controllerComponents: ControllerComponents)
  extends BackendController(controllerComponents) with Authorisation {

  val resourceConn: AuthorisationResource = annualAccountingService.registrationRepository

  def getAnnualAccountingScheme(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "AnnualAccountingSchemeController", "getAnnualAccountingScheme") {
          annualAccountingService.retrieveAnnualAccountingScheme(regId) sendResult("getAnnualAccountingScheme", regId)
        }
      }
  }

  def updateAnnualAccountingScheme(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "AnnualAccountingSchemeController", "updateAnnualAccountingScheme") {
          withJsonBody[AnnualAccountingScheme] { annualAccountingScheme =>
            annualAccountingService.updateAnnualAccountingScheme(regId, annualAccountingScheme)
              .sendResult("updateAnnualAccountingScheme", regId)
          }
        }
      }
  }
}
