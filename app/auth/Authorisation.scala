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

package auth

import play.api.mvc.{Request, Result}
import play.api.mvc.Results._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.internalId
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier
import utils.LoggingUtils

import scala.concurrent.{ExecutionContext, Future}

sealed trait AuthorisationResult

case object NotLoggedInOrAuthorised extends AuthorisationResult
case class NotAuthorised(intId: String) extends AuthorisationResult
case class Authorised(intId: String) extends AuthorisationResult
case class AuthResourceNotFound(intId: String) extends AuthorisationResult

trait Authorisation extends AuthorisedFunctions with LoggingUtils {

  implicit val executionContext: ExecutionContext

  def isAuthenticated(f: String => Future[Result])(implicit hc: HeaderCarrier, request: Request[_]): Future[Result] = {
    authorised().retrieve(internalId) { id =>
      id.fold {
        warnLog("[Authorisation] - [isAuthenticated] : No internalId present; FORBIDDEN")
        Future.successful(Forbidden("Missing internalId for the logged in user"))
      }(f)
    }.recoverWith {
      case e: AuthorisationException =>
        warnLog("[Authorisation] - [isAuthenticated]: AuthorisationException (auth returned a 401")
        Future.successful(Forbidden)
      case ex: Exception => Future.failed(throw ex)
    }
  }

}
