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

package controllers

import auth.Authorisation
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.AttachmentsService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AttachmentsController @Inject()(controllerComponents: ControllerComponents,
                                      val authConnector: AuthConnector,
                                      attachmentsService: AttachmentsService
                                     )(implicit executionContext: ExecutionContext) extends BackendController(controllerComponents) with Authorisation {

  def getAttachmentList(regId: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      attachmentsService.getAttachmentList(internalId, regId).map { attachmentList =>
        Ok(Json.toJson(attachmentList))
      }
    }
  }

  def getIncompleteAttachments(regId: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      attachmentsService.getIncompleteAttachments(internalId, regId).map { attachmentList =>
        Ok(Json.toJson(attachmentList))
      }
    }
  }
}
