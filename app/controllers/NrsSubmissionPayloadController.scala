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
import controllers.NrsSubmissionPayloadController.payloadKey
import play.api.libs.json.JsPath
import play.api.mvc.{Action, ControllerComponents}
import services.NrsSubmissionPayloadService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NrsSubmissionPayloadController @Inject()(controllerComponents: ControllerComponents,
                                               val authConnector: AuthConnector,
                                               val nrsSubmissionPayloadService: NrsSubmissionPayloadService
                                              )(implicit executionContext: ExecutionContext)
  extends BackendController(controllerComponents) with Authorisation {

  val resourceConn: AuthorisationResource = nrsSubmissionPayloadService.registrationMongoRepository

  def storeNrsSubmissionPayload(regId: String): Action[String] =
    Action.async[String](parse.json((JsPath \ payloadKey).read[String])) {
      implicit request =>
        isAuthorised(regId) { authResult =>
          authResult.ifAuthorised(regId, "NrsSubmissionPayloadController", "storeNrsSubmissionPayload") {
            nrsSubmissionPayloadService.storeNrsSubmissionPayload(regId, request.body)
              .sendResult("storeNrsSubmissionPayload", regId)
          }
        }
    }

}

object NrsSubmissionPayloadController {
  val payloadKey = "payload"
}