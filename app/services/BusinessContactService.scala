/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import models.api.BusinessContact
import repositories.RegistrationMongoRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BusinessContactService @Inject()(val registrationRepository: RegistrationMongoRepository) {

  def getBusinessContact(regId: String)(implicit ec: ExecutionContext): Future[Option[BusinessContact]] =
    registrationRepository.fetchBusinessContact(regId)

  def updateBusinessContact(regId: String, businessCont: BusinessContact)(implicit ec: ExecutionContext): Future[BusinessContact] =
    registrationRepository.updateBusinessContact(regId,businessCont)
}
