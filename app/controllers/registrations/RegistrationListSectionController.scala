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

package controllers.registrations

import auth.Authorisation
import models.registration.CollectionSectionId
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import services.{InvalidSection, RegistrationService, SectionValidationService, ValidSection}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.LoggingUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationListSectionController @Inject()(val authConnector: AuthConnector,
                                                  val registrationService: RegistrationService,
                                                  val sectionValidationService: SectionValidationService,
                                                  controllerComponents: ControllerComponents
                                                 )(implicit val executionContext: ExecutionContext) extends BackendController(controllerComponents) with Authorisation {

  def getSectionIndex(regId: String, section: CollectionSectionId, index: Int): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      isValidIndex(section, index) {
        registrationService.getSectionIndex(internalId, regId, section, index).flatMap {
          case Some(sectionData) =>
            sectionValidationService.validateIndex(section, sectionData).map {
              case Right(ValidSection(validatedJson)) =>
                Ok(validatedJson)
              case Left(response@InvalidSection(_)) =>
                InternalServerError(response.asString)
            }
          case None =>
            Future.successful(NotFound)
        }
      }
    }
  }

  def replaceSectionIndex(regId: String, section: CollectionSectionId, index: Int): Action[JsValue] = Action.async(parse.json) { implicit request =>
    isAuthenticated { internalId =>
      isValidIndex(section, index) {
        sectionValidationService.validateIndex(section, request.body).flatMap {
          case Right(ValidSection(validatedJson)) =>
            registrationService.upsertSectionIndex(internalId, regId, section, validatedJson, index).map {
              case Some(_) =>
                Ok(validatedJson)
              case _ =>
                errorLog(s"[RegistrationListSectionController][replaceSectionIndex] errored with Unable to upsert section '${section.key}' for regId '$regId'")
                InternalServerError(s"[RegistrationListSectionController] Unable to upsert section '${section.key}' for regId '$regId'")
            }
          case Left(response@InvalidSection(_)) =>
            errorLog(s"[RegistrationListSectionController][replaceSectionIndex] errored with ${response.asString}")
            Future.successful(BadRequest(response.asString))
        }
      }
    }
  }

  def deleteSectionIndex(regId: String, section: CollectionSectionId, index: Int): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      isValidIndex(section, index) {
        registrationService.deleteSectionIndex(internalId, regId, section, index).map { _ =>
          NoContent
        }
      }
    }
  }

  private def isValidIndex(section: CollectionSectionId, index: Int)(function: Future[Result]): Future[Result] =
    if (index > section.maxIndex || index < section.minIndex) {
      Future.successful(BadRequest(s"[RegistrationListSectionController] Index out of bounds for ${section.toString}"))
    } else {
      function
    }
}
