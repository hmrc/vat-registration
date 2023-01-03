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
import play.api.Logging
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.VatSchemeRepository
import services.submission.SubmissionPayloadBuilder
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RetrieveVatSubmissionController @Inject()(cc: ControllerComponents,
                                                submissionPayloadBuilder: SubmissionPayloadBuilder,
                                                val authConnector: AuthConnector,
                                                val resourceConn: VatSchemeRepository
                                               )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging with Authorisation {

  def retrieveSubmissionJson(regId: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      for {
        vatScheme <- resourceConn.getRegistration(internalId, regId)
          .map(_.getOrElse(throw new InternalServerException("Missing VatScheme")))
        payload = submissionPayloadBuilder.buildSubmissionPayload(vatScheme)
      } yield Ok(payload)
    }
  }

}
