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

import auth.{Authorisation, AuthorisationResource}
import play.api.libs.json.JsString
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.VatSchemeRepository
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DeleteAllRegistrationsController @Inject()(val authConnector: AuthConnector,
                                                 vatSchemeRepository: VatSchemeRepository)
                                                (implicit ec: ExecutionContext, cc: ControllerComponents) extends BackendController(cc) with Authorisation {

  override val resourceConn: AuthorisationResource = vatSchemeRepository

  def deleteAllRegistrations: Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { intId =>
      vatSchemeRepository.remove("internalId" -> JsString(intId)).map {
        case wr if wr.ok => NoContent
        case _ => InternalServerError
      }
    }
  }

}
