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
import models.api.ApplicantDetails
import play.api.libs.json.{JsValue, Reads}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.{ApplicantDetailsService, VatRegistrationService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class ApplicantDetailsController @Inject()(val applicantDetailsService: ApplicantDetailsService,
                                           val authConnector: AuthConnector,
                                           vatRegistrationService: VatRegistrationService,
                                           controllerComponents: ControllerComponents) extends BackendController(controllerComponents) with Authorisation {

  val resourceConn: AuthorisationResource = applicantDetailsService.registrationRepository

  def getApplicantDetailsData(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "ApplicantDetailsController", "getApplicantDetailsData") {
          vatRegistrationService.getPartyType(regId).flatMap {
            case Some(partyType) =>
              applicantDetailsService.getApplicantDetailsData(regId, partyType) sendResult("getApplicantDetailsData", regId)
            case err =>
              Future.successful(NotFound)
          }
        }
      }
  }

  def updateApplicantDetailsData(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      isAuthorised(regId) { authResult =>
        authResult.ifAuthorised(regId, "ApplicantDetailsController", "updateApplicantDetailsData") {
          vatRegistrationService.getPartyType(regId).flatMap {
            case Some(partyType) =>
              implicit val rds: Reads[ApplicantDetails] = ApplicantDetails.reads(partyType)
              withJsonBody[ApplicantDetails] { applicantDetails =>
                applicantDetailsService.updateApplicantDetailsData(regId, applicantDetails) sendResult("updateApplicantDetailsData", regId)
              }
            case _ =>
              Future.successful(NotFound)
          }
        }
      }
  }

}
