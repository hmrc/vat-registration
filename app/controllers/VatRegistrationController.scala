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

package controllers

import auth.Authorisation
import cats.instances.FutureInstances
import enums.VatRegStatus._
import httpparsers.{VatSubmissionFailure, VatSubmissionSuccess}
import models.monitoring.SubmissionFailureErrorsAuditModel
import play.api.libs.json._
import play.api.mvc._
import services._
import services.monitoring.AuditService
import services.submission.SubmissionService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatRegistrationController @Inject()(val registrationService: VatRegistrationService,
                                          val submissionService: SubmissionService,
                                          val authConnector: AuthConnector,
                                          controllerComponents: ControllerComponents
                                         )(implicit val executionContext: ExecutionContext)
  extends BackendController(controllerComponents) with Authorisation with FutureInstances {

  def submitVATRegistration(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthenticated { internalId =>
        val userHeaders = (request.body \ "userHeaders").asOpt[Map[String, String]].getOrElse(Map.empty)
        val lang = (request.body \ "lang").asOpt[String].getOrElse("en")

        registrationService.getStatus(internalId, regId).flatMap {
          case `locked` => Future.successful(TooManyRequests)
          case `submitted` => Future.successful(Ok)
          case `duplicateSubmission` => Future.successful(Conflict)
          case `contact` => Future.successful(UnprocessableEntity)
          case _ => submissionService.submitVatRegistration(internalId, regId, userHeaders, lang).map {
            case Right(VatSubmissionSuccess(_)) =>
              Ok
            case Left(VatSubmissionFailure(BAD_REQUEST, _)) =>
              BadRequest
            case Left(VatSubmissionFailure(CONFLICT, _)) =>
              Conflict
            case _ =>
              InternalServerError
          }
        }
      }
  }


}
