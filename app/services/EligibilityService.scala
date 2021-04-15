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

import models.api.EligibilitySubmissionData
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
        _ <- registrationRepository.updateEligibilitySubmissionData(regId, eligibilitySubmissionData)
        result <- registrationRepository.updateEligibilityData(regId, eligibilityData)
      } yield result
    )
  }
}
