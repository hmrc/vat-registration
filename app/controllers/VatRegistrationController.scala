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
import cats.instances.FutureInstances
import common.exceptions.LeftState
import enums.VatRegStatus._
import play.api.libs.json._
import play.api.mvc._
import repositories.VatSchemeRepository
import services._
import services.submission.SubmissionService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.ConflictException
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatRegistrationController @Inject()(val registrationService: VatRegistrationService,
                                          val submissionService: SubmissionService,
                                          val registrationRepository: VatSchemeRepository,
                                          val authConnector: AuthConnector,
                                          val newRegistrationService: RegistrationService,
                                          controllerComponents: ControllerComponents
                                         )(implicit executionContext: ExecutionContext)
  extends BackendController(controllerComponents) with Authorisation with FutureInstances {


  override val resourceConn: AuthorisationResource = registrationRepository
  val errorHandler: LeftState => Result = err => err.toResult

  def submitVATRegistration(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthenticated { internalId =>
        val userHeaders = (request.body \ "userHeaders").asOpt[Map[String, String]].getOrElse(Map.empty)

        registrationService.getStatus(regId).flatMap {
          case `locked` => Future.successful(TooManyRequests)
          case `submitted` => Future.successful(Ok)
          case `duplicateSubmission` => Future.successful(Conflict)
          case _ => submissionService.submitVatRegistration(regId, userHeaders).map { _ =>
            Ok
          }.recover {
            case ex: ConflictException => Conflict
            case ex: Throwable => throw ex
          }
        }
      }
  }
}
