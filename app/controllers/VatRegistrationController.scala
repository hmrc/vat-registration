/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.Inject

import common.exceptions.GenericServiceException
import connectors.AuthConnector
import models.VatChoice
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import services.RegistrationService

import scala.concurrent.ExecutionContext.Implicits.global

class VatRegistrationController @Inject()(val auth: AuthConnector, vatRegistrationService: RegistrationService) extends VatRegistrationBaseController {

  def newVatRegistration: Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { user =>
        vatRegistrationService.createNewRegistration map {
          case Right(vatScheme) => Created(Json.toJson(vatScheme))
          case Left(GenericServiceException(t)) =>
            Logger.warn("Exception in service call", t)
            ServiceUnavailable
        }
      }
  }


    def updateVatChoice(registrationId: String) : Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      authenticated { user =>
        withJsonBody[VatChoice] { vatChoice =>
          vatRegistrationService.updateVatChoice(registrationId, vatChoice) map {
            case Right(vatChoice) => Created(Json.toJson(vatChoice))
            case Left(GenericServiceException(t)) =>
              Logger.warn("Exception in service call", t)
              ServiceUnavailable
          }
        }
      }
  }

}
