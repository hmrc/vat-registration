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

import java.time.LocalDate

import javax.inject.{Inject, Singleton}
import models.api._
import play.api.libs.json.Json
import repositories.trafficmanagement.{DailyQuotaRepository, TrafficManagementRepository}
import utils.TimeMachine

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TrafficManagementService @Inject()(dailyQuotaRepository: DailyQuotaRepository,
                                         val trafficManagementRepository: TrafficManagementRepository,
                                         timeMachine: TimeMachine)
                                        (implicit ec: ExecutionContext) {

  def allocate(internalId: String, regId: String): Future[AllocationResponse] = {
    for {
      quotaReached <- dailyQuotaRepository.checkQuota
      channel = if (quotaReached) OTRS else VatReg
      _ <- trafficManagementRepository.upsertRegistrationInformation(internalId, regId, Draft, timeMachine.today, channel, timeMachine.today)
    } yield if (quotaReached) QuotaReached else Allocated
  }

  def getRegistrationInformation(internalId: String): Future[Option[RegistrationInformation]] =
    trafficManagementRepository.getRegistrationInformation(internalId)

  def upsertRegistrationInformation(internalId: String,
                                    registrationId: String,
                                    status: RegistrationStatus,
                                    regStartDate: LocalDate,
                                    channel: RegistrationChannel): Future[RegistrationInformation] =
    trafficManagementRepository.upsertRegistrationInformation(
      internalId = internalId,
      regId = registrationId,
      status = status,
      regStartDate = regStartDate,
      channel = channel,
      lastModified = timeMachine.today
    )

  def updateStatus(regId: String, status: RegistrationStatus): Future[Option[RegistrationInformation]] =
    trafficManagementRepository.findAndUpdate(
      query = Json.obj("registrationId" -> regId),
      update = Json.obj("$set" -> Json.obj(
        "status" -> RegistrationStatus.toJsString(status))
      ),
      upsert = true
    ) map (_.result[RegistrationInformation])

  def clearDocument(internalId: String): Future[Boolean] =
    trafficManagementRepository.clearDocument(internalId)
}

sealed trait AllocationResponse

case object QuotaReached extends AllocationResponse

case object Allocated extends AllocationResponse