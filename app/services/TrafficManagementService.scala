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

import config.BackendConfig
import models.api._
import models.submission._
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model._
import repositories.trafficmanagement.{DailyQuotaRepository, TrafficManagementRepository}
import uk.gov.hmrc.mongo.play.json.Codecs
import utils.TimeMachine

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TrafficManagementService @Inject()(dailyQuotaRepository: DailyQuotaRepository,
                                         val trafficManagementRepository: TrafficManagementRepository,
                                         timeMachine: TimeMachine)
                                        (implicit ec: ExecutionContext, config: BackendConfig) {

  private val defaultQuota = 0

  def dailyQuota(partyType: PartyType, isEnrolled: Boolean): Int =
    partyType match {
      case UkCompany if isEnrolled => config.DailyQuotas.enrolledUkCompany
      case UkCompany => config.DailyQuotas.ukCompany
      case Individual if isEnrolled => config.DailyQuotas.enrolledSoleTrader
      case Individual => config.DailyQuotas.soleTrader
      case NETP if isEnrolled => config.DailyQuotas.enrolledNetp
      case NETP => config.DailyQuotas.netp
      case NonUkNonEstablished if isEnrolled => config.DailyQuotas.enrolledNonUkCompany
      case NonUkNonEstablished => config.DailyQuotas.nonUkCompany
      case RegSociety if isEnrolled => config.DailyQuotas.enrolledRegSociety
      case RegSociety => config.DailyQuotas.regSociety
      case CharitableOrg if isEnrolled => config.DailyQuotas.enrolledCharitableIncorpOrg
      case CharitableOrg => config.DailyQuotas.charitableIncorpOrg
      case Partnership if isEnrolled => config.DailyQuotas.enrolledPartnership
      case Partnership => config.DailyQuotas.partnership
      case LtdPartnership if isEnrolled => config.DailyQuotas.enrolledLtdPartnership
      case LtdPartnership => config.DailyQuotas.ltdPartnership
      case ScotPartnership if isEnrolled => config.DailyQuotas.enrolledScotPartnership
      case ScotPartnership => config.DailyQuotas.scotPartnership
      case ScotLtdPartnership if isEnrolled => config.DailyQuotas.enrolledScotLtdPartnership
      case ScotLtdPartnership => config.DailyQuotas.scotLtdPartnership
      case LtdLiabilityPartnership if isEnrolled => config.DailyQuotas.enrolledLtdLiabilityPartnership
      case LtdLiabilityPartnership => config.DailyQuotas.ltdLiabilityPartnership
      case Trust if isEnrolled => config.DailyQuotas.enrolledTrust
      case Trust => config.DailyQuotas.trust
      case UnincorpAssoc if isEnrolled => config.DailyQuotas.enrolledUnincorpAssoc
      case UnincorpAssoc => config.DailyQuotas.unincorpAssoc
      case _ => defaultQuota
    }

  def currentHour: Int = timeMachine.timestamp.getHour

  def allocate(internalId: String, regId: String, partyType: PartyType, isEnrolled: Boolean): Future[AllocationResponse] = {
    val isWithinOpeningHours = currentHour >= config.allowUsersFrom && currentHour < config.allowUsersUntil
    for {
      currentTotal <- dailyQuotaRepository.currentTotal(partyType, isEnrolled)
      canAllocate = currentTotal < dailyQuota(partyType, isEnrolled) && isWithinOpeningHours
      _ <- if (canAllocate) {
        dailyQuotaRepository.incrementTotal(partyType, isEnrolled).flatMap(_ =>
          trafficManagementRepository.upsertRegInfoById(internalId, regId, Draft, timeMachine.today, VatReg, timeMachine.timestamp))
        } else {
          trafficManagementRepository.upsertRegInfoById(internalId, regId, Draft, timeMachine.today, OTRS, timeMachine.timestamp)
        }
    } yield if (canAllocate) Allocated else QuotaReached
  }

  def getRegInfoById(internalId: String, registrationId: String): Future[Option[RegistrationInformation]] =
    trafficManagementRepository.getRegInfoById(internalId, registrationId)

  def upsertRegInfoById(internalId: String,
                        registrationId: String,
                        status: RegistrationStatus,
                        regStartDate: LocalDate,
                        channel: RegistrationChannel): Future[RegistrationInformation] =
    trafficManagementRepository.upsertRegInfoById(
      internalId = internalId,
      regId = registrationId,
      status = status,
      regStartDate = regStartDate,
      channel = channel,
      lastModified = timeMachine.timestamp
    )

  def updateStatus(regId: String, status: RegistrationStatus): Future[Option[RegistrationInformation]] = {
    trafficManagementRepository.collection.findOneAndUpdate(
      filter = equal("registrationId", regId),
      update = set("status", Codecs.toBson(status)),
      options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER).upsert(false)
    ).toFutureOption()
  }

  def deleteRegInfoById(internalId: String, registrationId: String): Future[Boolean] =
    trafficManagementRepository.deleteRegInfoById(internalId, registrationId)
}

sealed trait AllocationResponse

case object QuotaReached extends AllocationResponse

case object Allocated extends AllocationResponse