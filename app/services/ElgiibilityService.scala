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

import models.api.Eligibility
import repositories.{RegistrationMongo, RegistrationMongoRepository, RegistrationRepository}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class EligibilityService @Inject()(val registrationMongo: RegistrationMongo) extends EligibilitySrv {
  val registrationRepository: RegistrationMongoRepository = registrationMongo.store
}

trait EligibilitySrv {

  val registrationRepository: RegistrationMongoRepository

  def upsertEligibility(regId: String, eligibility: Eligibility)(implicit hc: HeaderCarrier): Future[Eligibility] =
    registrationRepository.updateEligibility(regId, eligibility)

  def getEligibility(regId: String)(implicit hc: HeaderCarrier): Future[Option[Eligibility]] =
    registrationRepository.getEligibility(regId)
}
