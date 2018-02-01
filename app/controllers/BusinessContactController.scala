/*
 * Copyright 2018 HM Revenue & Customs
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

import common.exceptions.MissingRegDocument
import connectors.AuthConnector
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import services.BusinessContactService
import models.api.BusinessContact
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

class BusinessContactControllerImpl @Inject()(val businessContactService: BusinessContactService,
                                             val auth: AuthConnector
                                             ) extends BusinessContactController

trait BusinessContactController extends VatRegistrationBaseController {

val businessContactService:BusinessContactService

  def getBusinessContact(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        businessContactService.getBusinessContact(regId).sendResult
      }
  }

  def updateBusinessContact(regId: String): Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      authenticated { _ =>
        withJsonBody[BusinessContact] { businessCont =>
          businessContactService.updateBusinessContact(regId, businessCont) map {
            businessConResponse => Ok(Json.toJson(businessConResponse))
          } recover {
            case _: MissingRegDocument => NotFound(s"Registration not found for regId: $regId")
            case e => InternalServerError(s"An error occurred while updating Business Contact: for regId: $regId ${e.getMessage}")
          }
        }
      }
  }
}