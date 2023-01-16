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
import org.mongodb.scala.model.Filters.equal
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.VatSchemeRepository
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class DeleteAllRegistrationsController @Inject()(val authConnector: AuthConnector,
                                                 vatSchemeRepository: VatSchemeRepository)
                                                (implicit val executionContext: ExecutionContext,
                                                 cc: ControllerComponents) extends BackendController(cc) with Authorisation {

  def deleteAllRegistrations(): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { intId =>
      vatSchemeRepository.collection.deleteMany(equal("internalId", intId)).toFuture().map {
        case wr if wr.getDeletedCount > 0 => NoContent
        case _ => InternalServerError
      }
    }
  }

}
