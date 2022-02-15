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

package services

import models.api.EligibilitySubmissionData.{exceptionKey, exemptionKey}
import models.api.{EligibilitySubmissionData, VatScheme}
import play.api.Logging
import play.api.libs.json.{JsObject, JsResultException}
import repositories.VatSchemeRepository
import utils.EligibilityDataJsonUtils
import utils.JsonUtils.{jsonObject, optional}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EligibilityService @Inject()(val registrationRepository: VatSchemeRepository) extends Logging {

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
        _ <- logEligibilityPayload(regId, eligibilitySubmissionData)
        result <- registrationRepository.updateEligibilityData(regId, eligibilityData)
      } yield result
    )
  }

  private def logEligibilityPayload(regId: String,
                                    eligibilitySubmissionData: EligibilitySubmissionData): Future[Unit] = {

    val exceptionOrExemption = eligibilitySubmissionData.exceptionOrExemption match {
      case `exceptionKey` => Some("Exception")
      case `exemptionKey` => Some("Exemption")
      case _ => None
    }

    logger.info(jsonObject(
      "logInfo" -> "EligibilityPayloadLog",
      "regId" -> regId,
      "partyType" -> eligibilitySubmissionData.partyType.toString,
      "regReason" -> eligibilitySubmissionData.registrationReason.toString,
      "isTransactor" -> eligibilitySubmissionData.isTransactor,
      optional("exceptionOrExemption" -> exceptionOrExemption)
    ).toString())

    Future.successful()
  }

  private def removeInvalidFields(regId: String,
                                  eligibilityData: EligibilitySubmissionData,
                                  oldEligibilityData: EligibilitySubmissionData
                                 )(implicit executionContext: ExecutionContext): Future[VatScheme] = {

    registrationRepository.retrieveVatScheme(regId).flatMap {
      case Some(vatScheme) =>
        oldEligibilityData match {
          case EligibilitySubmissionData(_, _, _, _, oldPartyType, _, _, _, _) if !oldPartyType.equals(eligibilityData.partyType) =>
            registrationRepository.insertVatScheme(vatScheme.copy(
              tradingDetails = None,
              returns = None,
              sicAndCompliance = None,
              businessContact = None,
              bankAccount = None,
              applicantDetails = None,
              transactorDetails = None,
              flatRateScheme = None,
              partners = None,
              attachments = None,
              nrsSubmissionPayload = None,
              eligibilityData = None,
              eligibilitySubmissionData = None
            ))

          case EligibilitySubmissionData(_, _, oldTurnoverEstimates, _, _, _, _, oldTransactorFlag, _)
            if !oldTurnoverEstimates.equals(eligibilityData.estimates) || !oldTransactorFlag.equals(eligibilityData.isTransactor) =>
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

            val clearedTransactor = if (oldTransactorFlag != eligibilityData.isTransactor) {
              None
            } else {
              vatScheme.transactorDetails
            }

            registrationRepository.insertVatScheme(vatScheme.copy(
              flatRateScheme = clearedFRS,
              returns = clearedReturns,
              transactorDetails = clearedTransactor
            ))

          case _ =>
            Future.successful(vatScheme)
        }
    }
  }
}
