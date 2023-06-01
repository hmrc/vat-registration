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

package controllers.test

import models.api.schemas.API1364
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import services.SchemaValidationService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.LoggingUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class StubVatSubmissionController @Inject()(schemaValidationService: SchemaValidationService,
                                            apiSchema: API1364,
                                            cc: ControllerComponents) extends BackendController(cc) with LoggingUtils {

  val processSubmission: Action[JsValue] = Action.async(parse.json) { implicit request =>
    infoLog(s"[StubVatSubmissionController][processSubmission] Received submission: ${Json.prettyPrint(request.body)}")

    schemaValidationService.validate(apiSchema, request.body.toString()) match {
      case map if map.isEmpty =>
        Future.successful(Ok(Json.stringify(Json.obj("formBundle" -> "123412341234"))))
      case errorMap =>
        errorLog(s"[StubVatSubmissionController][processSubmission] Bad request errors:\n ${Json.prettyPrint(Json.toJson(errorMap))}")
        Future.successful(BadRequest(Json.obj("failures" -> Json.arr(Json.obj("code" -> "INVALID_PAYLOAD")))))
    }
  }
}
