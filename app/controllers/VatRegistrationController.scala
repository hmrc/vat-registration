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

import common.exceptions.{GenericServiceException, NotFoundException}
import connectors.AuthConnector
import models.{VatChoice, VatTradingDetails}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, Result}
import services.{RegistrationService, ServiceResult}

import scala.concurrent.ExecutionContext.Implicits.global

class VatRegistrationController @Inject()(val auth: AuthConnector, registrationService: RegistrationService) extends VatRegistrationBaseController {

  private[controllers] def handle[T](f: (T) => Result): ServiceResult[T] => Result = {
    case Right(entity) => f(entity)
    case Left(NotFoundException) => Gone
    case Left(GenericServiceException(t)) => Logger.warn("Exception in service call", t); ServiceUnavailable
    case _ => ServiceUnavailable
  }

  def newVatRegistration: Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { user =>
        registrationService.createNewRegistration.map {
          handle(newVatScheme => Created(Json.toJson(newVatScheme)))
        }
      }
  }

  def updateVatChoice(registrationId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      authenticated { user =>
        withJsonBody[VatChoice] { vatChoice =>
          registrationService.updateVatChoice(registrationId, vatChoice).map {
            handle(updated => Created(Json.toJson(updated)))
          }
        }
      }
  }


  def updateTradingDetails(registrationId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      authenticated { user =>
        withJsonBody[VatTradingDetails] { tradingDetails =>
          registrationService.updateTradingDetails(registrationId, tradingDetails).map {
            handle(updated => Created(Json.toJson(updated)))
          }
        }
      }
  }

}