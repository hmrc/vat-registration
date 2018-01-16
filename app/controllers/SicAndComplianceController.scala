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
import play.api.mvc.{Action, AnyContent}
import services.SicAndComplianceService
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import connectors.AuthConnector
import models.api.SicAndCompliance
import play.api.libs.json.{JsValue, Json}

class SicAndComplianceControllerImpl @Inject()(val sicAndComplianceService: SicAndComplianceService,
                                             val auth: AuthConnector) extends SicAndComplianceController

trait SicAndComplianceController extends VatRegistrationBaseController {
  val sicAndComplianceService: SicAndComplianceService

  def getSicAndCompliance(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        sicAndComplianceService.getSicAndCompliance(regId) sendResult
      }
  }

  def updateSicAndCompliance(regId: String) = Action.async[JsValue](parse.json) {
    implicit request =>
      authenticated { _ =>
        withJsonBody[SicAndCompliance] { sicAndComp =>
          sicAndComplianceService.updateSicAndCompliance(regId, sicAndComp)
            .map(a => Ok(Json.toJson(a)))
            .recover {
              case _: MissingRegDocument => NotFound(s"Registration not found for regId: $regId")
              case e: Exception => InternalServerError(s"An error occurred while updating SicAndCompliance for regId: $regId ${e.getMessage}")
            }
        }
      }
  }
}