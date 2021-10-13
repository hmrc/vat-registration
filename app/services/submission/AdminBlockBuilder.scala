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

import models.api.{AttachmentType, Post}
import models.submission.{NETP, NonUkNonEstablished}
import play.api.libs.json.JsObject
import repositories.RegistrationMongoRepository
import services.AttachmentsService
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils.{conditional, jsonObject, optional}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AdminBlockBuilder @Inject()(registrationMongoRepository: RegistrationMongoRepository,
                                  attachmentsService: AttachmentsService)
                                 (implicit ec: ExecutionContext) {

  def buildAdminBlock(regId: String): Future[JsObject] = for {
    optEligibilityData <- registrationMongoRepository.fetchEligibilitySubmissionData(regId)
    optTradingDetails <- registrationMongoRepository.retrieveTradingDetails(regId)
    attachments <- attachmentsService.getAttachmentList(regId)
  } yield (optEligibilityData, optTradingDetails) match {
    case (Some(eligibilityData), Some(tradingDetails)) =>
      jsonObject(
        "additionalInformation" -> jsonObject(
          "customerStatus" -> eligibilityData.customerStatus,
          conditional(List(NETP, NonUkNonEstablished).contains(eligibilityData.partyType))(
            "overseasTrader" -> true
          )
        ),
        "attachments" -> (jsonObject(
          optional("EORIrequested" -> tradingDetails.eoriRequested)
        ) ++ AttachmentType.submissionWrites(Post).writes(attachments).as[JsObject])
      )
    case (None, Some(_)) =>
      throw new InternalServerException("Could not build admin block for submission due to missing eligibility data")
    case (Some(_), None) =>
      throw new InternalServerException("Could not build admin block for submission due to missing trading details data")
    case _ =>
      throw new InternalServerException("Could not build admin block for submission due to no available data")
  }

}
