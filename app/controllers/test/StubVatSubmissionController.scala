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

package controllers.test

import javax.inject.{Inject, Singleton}
import models.api.VatSubmission
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.Future

@Singleton
class StubVatSubmissionController @Inject()(cc: ControllerComponents) extends BackendController(cc) with Logging {

  val processSubmission: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      Json.fromJson[VatSubmission](request.body)(VatSubmission.submissionReads) match {
        case JsSuccess(_, _) =>
          logger.info(s"[StubVatSubmissionController][processSubmission] Received submission: ${Json.prettyPrint(request.body)}")
          Future.successful(Ok)
        case JsError(errors) =>
          Future.successful(BadRequest(errors.toString()))
      }

  }
}