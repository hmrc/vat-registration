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

package services

import models.api.{EligibilitySubmissionData, VatScheme}
import models.registration.{EligibilityJsonSectionId, EligibilitySectionId}
import play.api.libs.json.{JsObject, JsResultException}
import play.api.mvc.Request
import repositories.VatSchemeRepository
import utils.JsonUtils.jsonObject
import utils.LoggingUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EligibilityService @Inject()(val registrationRepository: VatSchemeRepository) extends LoggingUtils {

  def updateEligibilityData(internalId: String, regId: String, eligibilityData: JsObject)(implicit ex: ExecutionContext, request: Request[_]): Future[Option[JsObject]] = {
    eligibilityData.validate[EligibilitySubmissionData](EligibilitySubmissionData.eligibilityReads).fold(
      invalid => throw JsResultException(invalid),
      eligibilitySubmissionData => for {
        _ <- registrationRepository.getSection[EligibilitySubmissionData](internalId, regId, EligibilitySectionId.repoKey).flatMap {
          case Some(oldEligibilitySubmissionData) =>
            removeInvalidFields(internalId, regId, eligibilitySubmissionData, oldEligibilitySubmissionData)
          case None =>
            Future.successful()
        }
        _ <- registrationRepository.upsertSection[EligibilitySubmissionData](internalId, regId, EligibilitySectionId.repoKey, eligibilitySubmissionData)
        _ <- logEligibilityPayload(regId, eligibilitySubmissionData)
        result <- registrationRepository.upsertSection[JsObject](internalId, regId, EligibilityJsonSectionId.repoKey, eligibilityData)
      } yield result
    )
  }

  private def logEligibilityPayload(regId: String,
                                    eligibilitySubmissionData: EligibilitySubmissionData)(implicit request: Request[_]): Future[Unit] = {

    infoLog(jsonObject(
      "logInfo" -> "EligibilityPayloadLog",
      "regId" -> regId,
      "partyType" -> eligibilitySubmissionData.partyType.toString,
      "regReason" -> eligibilitySubmissionData.registrationReason.toString,
      "isTransactor" -> eligibilitySubmissionData.isTransactor
    ).toString())

    Future.successful()
  }

  // scalastyle:off
  private def removeInvalidFields(internalId: String,
                                  regId: String,
                                  eligibilityData: EligibilitySubmissionData,
                                  oldEligibilityData: EligibilitySubmissionData
                                 )(implicit executionContext: ExecutionContext): Future[Option[VatScheme]] = {

    registrationRepository.getRegistration(internalId, regId).flatMap {
      case Some(vatScheme) =>
        oldEligibilityData match {
          case EligibilitySubmissionData(_, _, oldPartyType, _, _, _, _, oldFixedEstablishment)
            if !oldPartyType.equals(eligibilityData.partyType) || !oldFixedEstablishment.equals(eligibilityData.fixedEstablishmentInManOrUk) =>
            registrationRepository.upsertRegistration(internalId, regId, vatScheme.copy(
              bankAccount = None,
              flatRateScheme = None,
              eligibilityJson = None,
              eligibilitySubmissionData = None,
              applicantDetails = None,
              transactorDetails = None,
              nrsSubmissionPayload = None,
              entities = None,
              attachments = None,
              otherBusinessInvolvements = None,
              business = None,
              vatApplication = None
            ))

          case EligibilitySubmissionData(_, _, _, _, _, oldTransactorFlag, _, _)
            if !oldTransactorFlag.equals(eligibilityData.isTransactor) || eligibilityData.appliedForException.contains(true) =>

            val vatApplicationWithClearedExemption = if (eligibilityData.appliedForException.contains(true)) {
              vatScheme.vatApplication.map(_.copy(appliedForExemption = None))
            } else {
              vatScheme.vatApplication
            }

            val clearedTransactor = if (oldTransactorFlag != eligibilityData.isTransactor) {
              None
            } else {
              vatScheme.transactorDetails
            }

            registrationRepository.upsertRegistration(internalId, regId, vatScheme.copy(
              transactorDetails = clearedTransactor,
              vatApplication = vatApplicationWithClearedExemption
            ))

          case _ =>
            Future.successful(Some(vatScheme))
        }
    }
  }
}
