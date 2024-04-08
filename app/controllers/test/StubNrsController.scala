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

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class StubNrsController @Inject() (cc: ControllerComponents) extends BackendController(cc) with Logging {

  val processSubmission: Action[AnyContent] = Action.async {
    Future.successful(
      Accepted(
        Json.obj(
          "nrSubmissionId" -> UUID.randomUUID()
        )
      )
    )
  }

  val processAttachmentSubmission: Action[AnyContent] = Action.async {
    Future.successful(
      Accepted(
        Json.obj(
          "attachmentId" -> UUID.randomUUID()
        )
      )
    )
  }
}
