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

import models.api.LodgingOfficer
import repositories.{RegistrationMongo, RegistrationMongoRepository}

import scala.concurrent.{ExecutionContext, Future}

class LodgingOfficerService @Inject()(registrationMongo: RegistrationMongo) extends LodgingOfficerSrv {
  val registrationRepository: RegistrationMongoRepository = registrationMongo.store
}

trait LodgingOfficerSrv {
  val registrationRepository: RegistrationMongoRepository

  def getLodgingOfficer(regId: String)(implicit ec: ExecutionContext): Future[Option[LodgingOfficer]] =
    registrationRepository.getLodgingOfficer(regId)

  def updateLodgingOfficer(regId: String, officer: LodgingOfficer)(implicit ec: ExecutionContext): Future[LodgingOfficer] =
    registrationRepository.updateLodgingOfficer(regId,officer)

  def updateIVStatus(regId: String, ivStatus: Boolean)(implicit ec: ExecutionContext): Future[Boolean] = {
    registrationRepository.updateIVStatus(regId, ivStatus)
  }
}