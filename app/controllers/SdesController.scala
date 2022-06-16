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

package controllers

import auth.{Authorisation, AuthorisationResource}
import models.sdes.SdesCallback
import play.api.mvc.{Action, ControllerComponents}
import repositories.VatSchemeRepository
import services.SdesService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SdesController @Inject()(sdesService: SdesService,
                               val authConnector: AuthConnector,
                               controllerComponents: ControllerComponents,
                               registrationRepository: VatSchemeRepository)
                              (implicit executionContext: ExecutionContext)
  extends BackendController(controllerComponents) with Authorisation {

  val resourceConn: AuthorisationResource = registrationRepository

  def sdesCallback: Action[SdesCallback] = Action.async(parse.json[SdesCallback]) { implicit request =>
    sdesService.processCallback(request.body).map(_ => Accepted)
  }

}
