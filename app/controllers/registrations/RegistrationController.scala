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
import models.VatSchemeHeader
import models.api.VatScheme
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.RegistrationService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationController @Inject() (
  val authConnector: AuthConnector,
  val registrationService: RegistrationService,
  controllerComponents: ControllerComponents
)(implicit val executionContext: ExecutionContext)
    extends BackendController(controllerComponents)
    with Authorisation {

  /** GET /registrations
    * ===Purpose===
    * Return a list of all registrations in a reduced header format for the users's Internal ID
    * ===Detail===
    * @return
    *   OK - A JSON array of all the data for each registration found for the user's internal ID
    */
  def getAllRegistrationHeaders: Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      registrationService
        .getAllRegistrations[VatSchemeHeader](internalId)
        .map(registrations => Ok(Json.toJson(registrations)))
    }
  }

  /** POST /registrations
    * ===Purpose===
    * Creates a new registration against the user's internal ID
    * ===Detail===
    * @return
    *   CREATED - JSON representation of the newly created registration
    */
  def newRegistration: Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      registrationService
        .newRegistration(internalId)
        .map(registration => Created(Json.toJson(registration)(VatScheme.format())))
        .recover { case _ =>
          InternalServerError(
            "[VatRegistrationController][newVatRegistration] Unexpected error when creating new registration"
          )
        }
    }
  }

  /** GET /registrations/:regId
    * ===Purpose===
    * Deletes a registration that matches the given Registration ID and the user's Internal ID
    * ===Detail===
    * @param regId
    *   \- the unique ID for the desired registration
    * @return
    *   OK - JSON representation of the registration for the given Registration ID<br> NOT_FOUND - No registration
    *   exists for the given Registration ID
    */
  def getRegistration(regId: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      registrationService.getRegistration(internalId, regId).map {
        case Some(registration) =>
          Ok(Json.toJson(registration)(VatScheme.format()))
        case _                  =>
          NotFound
      }
    }
  }

  /** PUT /registrations/:regId
    * ===Purpose===
    * Upsert the complete details of a registration
    * ===Detail===
    * @param regId
    *   \- the unique ID for the desired registration
    * @return
    *   OK - JSON the details of the updated registration
    */
  def upsertRegistration(regId: String): Action[JsValue] = Action.async(parse.json) { implicit request =>
    isAuthenticated { internalId =>
      implicit val format: Format[VatScheme] = VatScheme.format()
      val json                               = request.body.as[JsObject] ++ Json.obj("internalId" -> internalId)

      json.validate[VatScheme] match {
        case JsSuccess(scheme, _) =>
          registrationService.upsertRegistration(internalId, regId, scheme).map { updatedRegistration =>
            Ok(Json.toJson(updatedRegistration))
          }
        case JsError(_)           =>
          Future.successful(
            BadRequest(
              "[VatRegistrationController][upsertRegistration] Request JSON could not be parsed as a VAT scheme"
            )
          )
      }
    }
  }

  /** DELETE /registrations/:regId
    * ===Purpose===
    * Delete a registration with the given Registration ID
    * ===Detail===
    * @return
    *   NoContent - regardless of whether the record exists or not
    */
  def deleteRegistration(regId: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { internalId =>
      registrationService
        .deleteRegistration(internalId, regId)
        .map(_ => NoContent)
    }
  }

}
