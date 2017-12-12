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

import javax.inject.Inject

import common.RegistrationId
import common.exceptions.{InvalidSubmissionStatus, LeftState, MissingRegDocument, ResourceNotFound}
import connectors.AuthConnector
import enums.VatRegStatus
import models.ElementPath
import models.api._
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Result}
import repositories.{RegistrationMongo, RegistrationMongoRepository}
import repositories.RegistrationMongoFormats.encryptedFinancials
import services._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import utils.VATFeatureSwitches

import scala.concurrent.Future

class VatRegistrationController @Inject()(val auth: AuthConnector,
                                          registrationService: RegistrationService,
                                          submissionService: SubmissionService,
                                          registrationMongo: RegistrationMongo) extends VatRegistrationBaseController {

  val registrationRepository: RegistrationMongoRepository = registrationMongo.store

  val errorHandler: (LeftState) => Result = err => err.toResult

  private[controllers] def useMockSubmission: Boolean = VATFeatureSwitches.mockSubmission.enabled

  def newVatRegistration: Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationService.createNewRegistration.fold(errorHandler, vatScheme => Created(Json.toJson(vatScheme)))
      }
  }

  def retrieveVatScheme(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationService.retrieveVatScheme(id).fold(errorHandler, vatScheme => Ok(Json.toJson(vatScheme)))
      }
  }

  def updateVatFinancials(id: RegistrationId): Action[JsValue] = {
    implicit val format: Format[VatFinancials] = Format(VatFinancials.format, encryptedFinancials)
    patch[VatFinancials](registrationService, id)
  }

  @deprecated
  def updateVatTradingDetails(id: RegistrationId): Action[JsValue] = patch[VatTradingDetails](registrationService, id)

  def updateTradingDetails(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      authenticated { _ =>
        withJsonBody[TradingDetails]{ tradingDetails =>
          registrationRepository.updateTradingDetails(regId, tradingDetails) map ( _ => Ok)
        }
      }
  }

  def updateSicAndCompliance(id: RegistrationId): Action[JsValue] = patch[VatSicAndCompliance](registrationService, id)

  def updateVatContact(id: RegistrationId): Action[JsValue] = patch[VatContact](registrationService, id)

  @deprecated("Use updateEligibility or updateThreshold instead", "12/12/2017")
  def updateVatEligibility(id: RegistrationId): Action[JsValue] = patch[VatServiceEligibility](registrationService, id)

  def updateLodgingOfficer(id: RegistrationId): Action[JsValue] = patch[VatLodgingOfficer](registrationService, id)

  def updateFlatRateScheme(id: RegistrationId): Action[JsValue] = patch[VatFlatRateScheme](registrationService, id)

  def submitVATRegistration(id: RegistrationId) : Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        if (!useMockSubmission) {
          submissionService.submitVatRegistration(id).map { ackRefs =>
            Ok(Json.toJson(ackRefs))
          } recover {
            case ex => BadRequest(s"Registration was submitted without full data: ${ex.getMessage}")
          }
        } else {
          Future.successful(Ok(Json.toJson("BRVT000000" + id)))
        }
      }
  }

  def getAcknowledgementReference(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        if (!useMockSubmission) {
          submissionService.getAcknowledgementReference(id).fold(errorHandler, ackRefNumber => Ok(Json.toJson(ackRefNumber)))
        } else {
          Future.successful(Ok(Json.toJson("BRVT000000" + id)))
        }
      }
  }

  def deleteVatScheme(regId: String): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationService.deleteVatScheme(regId, VatRegStatus.draft, VatRegStatus.rejected) map { deleted =>
          if(deleted) Ok else InternalServerError
        } recover {
          case _: InvalidSubmissionStatus => PreconditionFailed
        }
      }
  }

  def getDocumentStatus(id: RegistrationId): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { _ =>
        registrationService.getStatus(id) map { statusJson =>
          Ok(statusJson)
        } recover {
          case _: MissingRegDocument => NotFound
          case e                     =>
            logger.error(s"[getDocumentStatus] - There was a problem getting the document status for regId: ${id.value}", e)
            InternalServerError
        }
      }
  }


  def deleteByElement(id: RegistrationId, elementPath: ElementPath): Action[AnyContent] = delete(registrationService, id, elementPath)

  def updateIVStatus(regId: String): Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      authenticated { _ =>
        withJsonBody[JsValue] { json =>
          registrationService.updateIVStatus(regId, json.\("ivPassed").as[Boolean]) map { _ =>
            Ok(json)
          } recover {
            case _ =>
              logger.error(s"[VatRegistrationController] - [updateIVStatus] - There was a problem updating the IV status for regId $regId")
              InternalServerError
          }
        }
      }
  }
}
