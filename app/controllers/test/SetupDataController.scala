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

import auth.Authorisation
import models.api.{UpscanDetails, VatScheme}
import play.api.libs.json.JsError.toJson
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import repositories.VatSchemeRepository
import services.{RegistrationService, UpscanService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SetupDataController @Inject()(val authConnector: AuthConnector,
                                    registrationService: RegistrationService,
                                    upscanService: UpscanService,
                                    vatSchemeRepository: VatSchemeRepository)
                                   (implicit val executionContext: ExecutionContext,
                                                 cc: ControllerComponents) extends BackendController(cc) with Authorisation {




  def setUpData(regId: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { interalId =>
      Json.toJson(request.body.asJson).validate[VatScheme](VatScheme.format()) match {
              case JsSuccess(value, path) =>
                val data = value.copy(internalId = interalId, registrationId = regId)
                registrationService.upsertRegistration(internalId = interalId, data.registrationId, data).map {
                  updatedRegistration =>
                    Created(Json.toJson(value.registrationId))
                }.recover {
                  case _ => NotFound(Json.toJson("error"))
                }
              case _ => Future.successful(NotFound(Json.toJson("error")))
            }
          }
    }

  def setUpUpscan(regId: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { _ =>
      Json.toJson(request.body.asJson).validate[Seq[UpscanDetails]] match {
        case JsSuccess(value, _) =>
          val data = value.map(_.copy(registrationId = Some(regId)))
          Future.sequence(data.map(upscanService.upsertUpscanDetails(_))).map(_ => Created(Json.toJson(value)))
        case error: JsError =>
          Future.successful(NotFound(Json.toJson("error")))
      }
    }
  }

}
