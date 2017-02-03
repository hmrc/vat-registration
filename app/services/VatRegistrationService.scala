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

package services

import javax.inject.Inject

import common.exceptions.{ForbiddenException, GenericServiceException, NotFoundException, UpdateFailed}
import connectors._
import models.{VatChoice, VatScheme}
import repositories.RegistrationRepository
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait RegistrationService {
  def updateVatChoice(registrationId: String, vatChoice: VatChoice)(implicit headerCarrier: HeaderCarrier): Future[ServiceResult[VatChoice]]
  def createNewRegistration(implicit headerCarrier: HeaderCarrier): Future[ServiceResult[VatScheme]]

}

class VatRegistrationService @Inject()(brConnector: BusinessRegistrationConnector,
                                       registrationRepository: RegistrationRepository
                                      ) extends RegistrationService {

  import scala.concurrent.ExecutionContext.Implicits.global

  override def updateVatChoice(registrationId: String, vatChoice: VatChoice)(implicit headerCarrier: HeaderCarrier): Future[ServiceResult[VatChoice]] = {
    (registrationRepository.updateVatChoice(registrationId, vatChoice) flatMap (vatChoice =>  Future.successful(Right(vatChoice)))).recover {
      case t: Throwable => Left(GenericServiceException(t))
    }
  }

  override def createNewRegistration(implicit headerCarrier: HeaderCarrier): Future[ServiceResult[VatScheme]] = {
    brConnector.retrieveCurrentProfile flatMap {
      case BusinessRegistrationSuccessResponse(profile) =>
        registrationRepository.retrieveVatScheme(profile.registrationID) flatMap {
          case Some(registration) => Future.successful(Right(registration))
          case None => (registrationRepository.createNewVatScheme(profile.registrationID) map (vatScheme => Right(vatScheme))).recover {
            case t: Throwable => Left(GenericServiceException(t))
          }
        }
      case BusinessRegistrationForbiddenResponse => Future.successful(Left(ForbiddenException))
      case BusinessRegistrationNotFoundResponse => Future.successful(Left(NotFoundException))
      case BusinessRegistrationErrorResponse(err) => Future.successful(Left(GenericServiceException(err)))
    }
  }

}
