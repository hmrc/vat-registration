/*
 * Copyright 2021 HM Revenue & Customs
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

import models.api.{EligibilitySubmissionData, VatScheme}
import play.api.libs.json.{JsObject, JsResultException}
import repositories.RegistrationMongoRepository
import utils.EligibilityDataJsonUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EligibilityService @Inject()(val registrationRepository: RegistrationMongoRepository) {

  def getEligibilityData(regId: String): Future[Option[JsObject]] =
    registrationRepository.fetchEligibilityData(regId)

  def updateEligibilityData(regId: String, eligibilityData: JsObject)(implicit ex: ExecutionContext): Future[JsObject] = {
    EligibilityDataJsonUtils.toJsObject(eligibilityData)
      .validate[EligibilitySubmissionData](EligibilitySubmissionData.eligibilityReads).fold(
      invalid => throw JsResultException(invalid),
      eligibilitySubmissionData => for {
        _ <- registrationRepository.fetchEligibilitySubmissionData(regId).flatMap {
          case Some(oldEligibilitySubmissionData) =>
            removeInvalidFields(regId, eligibilitySubmissionData, oldEligibilitySubmissionData)
          case None =>
            Future.successful()
        }
        _ <- registrationRepository.updateEligibilitySubmissionData(regId, eligibilitySubmissionData)
        result <- registrationRepository.updateEligibilityData(regId, eligibilityData)
      } yield result
    )
  }

  private def removeInvalidFields(regId: String,
                                  eligibilityData: EligibilitySubmissionData,
                                  oldEligibilityData: EligibilitySubmissionData
                                 )(implicit executionContext: ExecutionContext): Future[VatScheme] = {

    registrationRepository.retrieveVatScheme(regId).flatMap {
      case Some(vatScheme) =>
        oldEligibilityData match {
          case EligibilitySubmissionData(_, _, _, _, oldPartyType) if !oldPartyType.equals(eligibilityData.partyType) =>
            registrationRepository.insertVatScheme(VatScheme(
              id = vatScheme.id,
              internalId = vatScheme.internalId,
              status = vatScheme.status
            ))

          case EligibilitySubmissionData(_, _, oldTurnoverEstimates, _, _) if !oldTurnoverEstimates.equals(eligibilityData.estimates) =>
            val clearedFRS = if (oldTurnoverEstimates.turnoverEstimate > 150000L) {
              None
            } else {
              vatScheme.flatRateScheme
            }

            val clearedReturns = if (oldTurnoverEstimates.turnoverEstimate > 1350000L &&
              vatScheme.returns.map(_.annualAccountingDetails).isDefined) {
              None
            } else {
              vatScheme.returns
            }

            registrationRepository.insertVatScheme(vatScheme.copy(flatRateScheme = clearedFRS, returns = clearedReturns))

          case _ =>
            Future.successful(vatScheme)
        }
    }
  }
}
