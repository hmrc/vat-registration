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

package services.submission

import play.api.libs.json.{JsObject, Json}
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils.{jsonObject, optional}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdminBlockBuilder @Inject()(registrationMongoRepository: RegistrationMongoRepository)
                                 (implicit ec: ExecutionContext) {

  def buildAdminBlock(regId: String): Future[JsObject] = for {
    optEligibilityData <- registrationMongoRepository.fetchEligibilitySubmissionData(regId)
    optTradingDetails <- registrationMongoRepository.retrieveTradingDetails(regId)
  } yield (optEligibilityData, optTradingDetails) match {
    case (Some(eligibilityData), Some(tradingDetails)) =>
      Json.obj(
        "additionalInformation" -> Json.obj(
          "customerStatus" -> eligibilityData.customerStatus
        ),
        "attachments" -> jsonObject(
          optional("EORIrequested" -> tradingDetails.eoriRequested)
        )
      )
    case (None, Some(_)) =>
      throw new InternalServerException("Could not build admin block for submission due to missing eligibility data")
    case (Some(_), None) =>
      throw new InternalServerException("Could not build admin block for submission due to missing trading details data")
    case _ =>
      throw new InternalServerException("Could not build admin block for submission due to no available data")
  }

}
