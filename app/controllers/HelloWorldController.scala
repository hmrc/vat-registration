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

package controllers

import auth.{Authenticated, LoggedIn, NotLoggedIn}
import connectors.AuthConnector
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future

object HelloWorldController extends HelloWorldController {

  // $COVERAGE-OFF$
  override val auth = AuthConnector
  // $COVERAGE-ON$

}

trait HelloWorldController extends BaseController with Authenticated {

  def hello: Action[AnyContent] = Action.async {
    implicit request =>
      authenticated {
        case NotLoggedIn => Future.successful(Forbidden)
        case LoggedIn(context) =>
          Future.successful(Ok(Json.toJson(context)))
      }
  }

}
