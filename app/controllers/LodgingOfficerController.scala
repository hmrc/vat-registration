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

import common.exceptions.MissingRegDocument
import connectors.AuthConnector
import models.api.LodgingOfficer
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import services.LodgingOfficerService
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

class LodgingOfficerControllerImpl @Inject()(val lodgingOfficerService: LodgingOfficerService,
                                          val auth: AuthConnector) extends LodgingOfficerController

trait LodgingOfficerController extends VatRegistrationBaseController {

  val lodgingOfficerService: LodgingOfficerService

  def getLodgingOfficer(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { authority =>
        lodgingOfficerService.getLodgingOfficer(regId) map {
          _.fold(NotFound(""))(officer => Ok(Json.toJson(officer)))
        }
      }
  }

  def updateLodgingOfficer(regId: String): Action[JsValue] = Action.async[JsValue](parse.json) {
    implicit request =>
      authenticated {
        context =>
          withJsonBody[LodgingOfficer] { officer =>
            lodgingOfficerService.updateLodgingOfficer(regId, officer) map {
              officerResponse => Ok(Json.toJson(officerResponse))
            } recover {
              case _: MissingRegDocument => NotFound(s"Registration not found for regId: $regId")
              case e => InternalServerError(s"An error occurred while updating lodging officer: ${e.getMessage}")
            }
          }
      }
  }
}
