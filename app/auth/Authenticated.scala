/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.{AuthConnector, Authority}
import play.api.Logger
import play.api.mvc.Result
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait AuthenticationResult {}

case object NotLoggedIn extends AuthenticationResult

final case class LoggedIn(authContext: Authority) extends AuthenticationResult

trait Authenticated {

  val auth: AuthConnector

  def authenticated(f: => AuthenticationResult => Future[Result])(implicit hc: HeaderCarrier): Future[Result] = {
    Logger.debug(s"Current user id is ${hc.userId}") // always outputs NONE :-(

    for {
      authority <- auth.getCurrentAuthority()
      result <- f(mapToAuthResult(authority))
    } yield {
      Logger.debug(s"Got authority = $authority")
      result
    }
  }

  private def mapToAuthResult(authContext: Option[Authority]): AuthenticationResult = {
    authContext match {
      case None => NotLoggedIn
      case Some(context) => LoggedIn(context)
    }
  }
}
