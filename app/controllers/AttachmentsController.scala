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

package controllers

import auth.{Authorisation, AuthorisationResource}
import models.api.Attachments
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.AttachmentsService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.JsonUtils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AttachmentsController @Inject()(controllerComponents: ControllerComponents,
                                      val authConnector: AuthConnector,
                                      attachmentsService: AttachmentsService
                                     )(implicit executionContext: ExecutionContext) extends BackendController(controllerComponents) with Authorisation {

  val resourceConn: AuthorisationResource = attachmentsService.registrationRepository

  def getAttachmentList(regId: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthorised(regId) { authResult =>
      authResult.ifAuthorised(regId, "AttachmentsController", "getAttachmentList") {
        for {
          attachmentList <- attachmentsService.getAttachmentList(regId)
          optAttachmentDetails <- attachmentsService.getAttachmentDetails(regId)
        } yield Ok(
          jsonObject(
            optional("method" -> optAttachmentDetails.map(_.method)),
            "attachments" -> Json.toJson(attachmentList)
          )
        )
      }
    }
  }

  def storeAttachmentDetails(regId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    isAuthorised(regId) { authResult =>
      authResult.ifAuthorised(regId, "AttachmentsController", "storeAttachmentDetails") {
        request.body.validate[Attachments] match {
          case JsSuccess(value, _) =>
            attachmentsService.storeAttachmentDetails(regId, value).map { attachmentDetails =>
              Ok(Json.toJson(attachmentDetails))
            }
          case JsError(errors) =>
            logger.debug(errors.toString())
            Future.successful(BadRequest)
        }
      }
    }
  }

}
