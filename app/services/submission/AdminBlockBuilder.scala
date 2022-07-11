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

package services.submission

import models.api.{AttachmentType, VatScheme}
import models.submission.{NETP, NonUkNonEstablished}
import play.api.libs.json.{JsObject, Json}
import services.AttachmentsService
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.{Inject, Singleton}

@Singleton
class AdminBlockBuilder @Inject()(attachmentsService: AttachmentsService) {

  private val MTDfB = "2"

  def buildAdminBlock(vatScheme: VatScheme): JsObject = {
    val attachmentList = attachmentsService.attachmentList(vatScheme)
    (vatScheme.eligibilitySubmissionData, vatScheme.vatApplication) match {
      case (Some(eligibilityData), Some(vatApplication)) =>
        jsonObject(
          "additionalInformation" -> jsonObject(
            "customerStatus" -> MTDfB,
            conditional(List(NETP, NonUkNonEstablished).contains(eligibilityData.partyType))(
              "overseasTrader" -> true
            )
          ),
          "attachments" -> (jsonObject(
            optional("EORIrequested" -> vatApplication.eoriRequested)) ++ {
            vatScheme.attachments.map(_.method) match {
              case Some(attachmentMethod) => AttachmentType.submissionWrites(attachmentMethod).writes(attachmentList).as[JsObject]
              case None => Json.obj()
            }
          })
        )
      case (None, Some(_)) =>
        throw new InternalServerException("Could not build admin block for submission due to missing eligibility data")
      case (Some(_), None) =>
        throw new InternalServerException("Could not build admin block for submission due to missing vat application data")
      case _ =>
        throw new InternalServerException("Could not build admin block for submission due to no available data")
    }
  }
}