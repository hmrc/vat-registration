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

package controllers.registrations

import auth.Authorisation
import models.registration.RegistrationSectionId
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationSectionController @Inject()(val authConnector: AuthConnector,
                                              val registrationService: RegistrationService,
                                              val sectionValidationService: SectionValidationService,
                                              controllerComponents: ControllerComponents,
                                              cipherService: CipherService
                                             )(implicit executionContext: ExecutionContext) extends BackendController(controllerComponents) with Authorisation {

  /** GET /registrations/:regId/sections/:sectionId
   * ===Purpose===
   * Retrieve the data of the named section in the given registration
   * ===Detail===
   *
   * @param regId
   * @param sectionId
   * @return OK - Json representation of the named section<br>
   *         NOT_FOUND - The named section doesn't exist in the named section
   */
  def getSection(regId: String, section: RegistrationSectionId): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      registrationService.getSection[JsValue](internalId, regId, section).flatMap {
        case Some(sectionData) =>
          sectionValidationService.validate(internalId, regId, section, cipherService.conditionallyDecrypt(section, sectionData)).map {
            case Right(ValidSection(value)) => Ok(value)
            case Left(response@InvalidSection(_)) => InternalServerError(response.asString)
          }
        case _ =>
          Future.successful(NotFound)
      }
    }
  }

  /** PATCH /registrations/:regId/sections/:sectionId
   * ===Purpose===
   * merge the given JSON with the stored JSON
   * ===Detail===
   *
   * @param regId
   * @param sectionKey
   * @return OK - Json representation of the updated section<br>
   *         BAD_REQUEST - The request json was not valid for the given section
   */
  def upsertSection(regId: String, section: RegistrationSectionId): Action[JsValue] = Action.async(parse.json) { implicit request =>
    isAuthenticated { internalId =>
      for {
        existingSection <- registrationService.getSection[JsObject](internalId, regId, section)
          .map(_.getOrElse(Json.obj()))
        validationResponse <- sectionValidationService.validate(internalId, regId, section, existingSection.deepMerge(request.body.as[JsObject]))
        validatedData = validationResponse.map(_.validatedModel).getOrElse(Json.obj())
        encryptedData = cipherService.conditionallyEncrypt(section, validatedData)
        update <- registrationService.upsertSection(internalId, regId, section, encryptedData)
      } yield (validationResponse, update) match {
        case (Right(ValidSection(_)), Some(updatedSection)) =>
          Ok(updatedSection)
        case (_, None) =>
          InternalServerError(s"[RegistrationSectionController][upsertSection] Unable to update section ${section.key}")
        case (Left(response@InvalidSection(_)), _) =>
          logger.debug(s"[RegistrationSectionController][upsertSection] Missing keys: ${response.asString}")
          BadRequest(response.asString)
      }
    }
  }

  /** PUT /registrations/:regId/sections/:sectionId
   * ===Purpose===
   * Replaces the named section in the given registration with the given JSON
   * ===Detail===
   *
   * @param regId
   * @param sectionKey
   * @return OK - Json representation of the updated section<br>
   *         BAD_REQUEST - The request json was not valid for the given section
   */
  def replaceSection(regId: String, section: RegistrationSectionId): Action[JsValue] = Action.async(parse.json) { implicit request =>
    isAuthenticated { internalId =>
      sectionValidationService.validate(internalId, regId, section, request.body).flatMap {
        case Right(ValidSection(validatedJson)) =>
          val encryptedJson = cipherService.conditionallyEncrypt(section, validatedJson)
          registrationService.upsertSection[JsValue](internalId, regId, section, encryptedJson).map {
            case Some(updatedSectionJson) =>
              Ok(updatedSectionJson)
            case _ =>
              InternalServerError(s"[RegistrationSectionController][upsertSection] Unable to upsert section '${section.key}' for regId '$regId'")
          }
        case Left(response@InvalidSection(_)) =>
          logger.debug(s"[RegistrationSectionController][upsertSection] Missing keys: ${response.asString}")
          Future.successful(BadRequest(response.asString))
      }
    }
  }

  /** DELETE /registrations/:regId/sections/:sectionId
   * ===Purpose===
   * Delete the named section in the given registration
   * ===Detail===
   *
   * @param regId
   * @param sectionKey
   * @return NO_CONTENT
   */
  def deleteSection(regId: String, section: RegistrationSectionId): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      registrationService.deleteSection(internalId, regId, section).map { _ =>
        NoContent
      }
    }
  }

}
