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

package auth

import play.api.Logging
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.internalId
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait AuthorisationResult
case object NotLoggedInOrAuthorised extends AuthorisationResult
case class NotAuthorised(intId: String) extends AuthorisationResult
case class Authorised(intId: String) extends AuthorisationResult
case class AuthResourceNotFound(intId: String) extends AuthorisationResult

trait Authorisation extends AuthorisedFunctions with Logging {

  val resourceConn : AuthorisationResource

  def isAuthenticated(f: String => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    authorised().retrieve(internalId) { id =>
      id.fold {
        logger.warn("[Authorisation] - [isAuthenticated] : No internalId present; FORBIDDEN")
        Future.successful(Forbidden("Missing internalId for the logged in user"))
      }(f)
    }.recoverWith {
      case e: AuthorisationException =>
        logger.warn("[Authorisation] - [isAuthenticated]: AuthorisationException (auth returned a 401")
        Future.successful(Forbidden)
      case ex: Exception => Future.failed(throw ex)
    }
  }
// TODO: implement this on every controller where it is needed when user story is played
  def isAuthorised(regId: String)(f: => AuthorisationResult => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    authorised().retrieve(internalId) { id =>
      resourceConn.getInternalId(regId) flatMap { resource =>
        f(mapToAuthResult(id, resource))
      }
    } recoverWith {
      case ar: AuthorisationException =>
        logger.warn(s"[Authorisation] - [isAuthorised]: An error occurred, err: ${ar.getMessage}")
        f(NotLoggedInOrAuthorised)
      case e =>
        throw e
    }
  }

  private def mapToAuthResult(authContext: Option[String], resource: Option[String] ) : AuthorisationResult = {
    authContext match {
      case None =>
        logger.warn("[mapToAuthResult]: No authority was found")
        NotLoggedInOrAuthorised
      case Some(id) =>
        resource match {
        case None =>
          logger.info("[Authorisation] [mapToAuthResult]: No auth resource was found for the current user")
          AuthResourceNotFound(id)
        case Some(resourceId) if resourceId == id =>
          Authorised(id)
        case _ =>
          logger.warn("[mapToAuthResult]: The current user is not authorised to access this resource")
          NotAuthorised(id)
      }
    }
  }
}
