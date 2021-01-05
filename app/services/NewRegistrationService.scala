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

import java.util.UUID

import featureswitch.core.config.FeatureSwitching
import javax.inject.{Inject, Singleton}
import models.api.VatScheme
import repositories.RegistrationMongoRepository

import scala.concurrent.Future

@Singleton
class NewRegistrationService @Inject()(registrationRepository: RegistrationMongoRepository) extends FeatureSwitching {

  def newRegistration(internalId: String): Future[VatScheme] = {
    val regId = generateRegistrationId()

    registrationRepository.createNewVatScheme(regId, internalId)
  }

  private[services] def generateRegistrationId(): String = UUID.randomUUID().toString

}
