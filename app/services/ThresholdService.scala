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

import models.api.Threshold
import repositories.{RegistrationMongo, RegistrationMongoRepository, RegistrationRepository}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ThresholdService @Inject()(val registrationMongo: RegistrationMongo) extends ThresholdSrv {
  val registrationRepository: RegistrationMongoRepository = registrationMongo.store
}

trait ThresholdSrv {
  val registrationRepository: RegistrationMongoRepository

  def upsertThreshold(regId: String, threshold: Threshold)(implicit hc: HeaderCarrier): Future[Threshold] =
    registrationRepository.updateThreshold(regId, threshold)

  def getThreshold(regId: String)(implicit hc: HeaderCarrier): Future[Option[Threshold]] =
    registrationRepository.getThreshold(regId)
}
