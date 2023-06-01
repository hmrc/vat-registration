/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.mvc.Request
import services.AttachmentsService
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._
import utils.LoggingUtils

import javax.inject.{Inject, Singleton}

@Singleton
class AdminBlockBuilder @Inject()(attachmentsService: AttachmentsService) extends LoggingUtils{

  private val MTDfB = "2"

  def buildAdminBlock(vatScheme: VatScheme)(implicit request: Request[_]): JsObject = {
    val mandatoryAttachmentList = attachmentsService.mandatoryAttachmentList(vatScheme)
    (vatScheme.eligibilitySubmissionData, vatScheme.vatApplication) match {
      case (Some(eligibilityData), Some(vatApplication)) =>
        jsonObject(
          "additionalInformation" -> jsonObject(
            "customerStatus" -> MTDfB,
            conditional(List(NETP, NonUkNonEstablished).contains(eligibilityData.partyType))(
              "overseasTrader" -> !eligibilityData.fixedEstablishmentInManOrUk
            ),
            conditional(vatScheme.business.exists(_.welshLanguage.exists(_ == true)))("welshLanguage" -> true)
          ),
          "attachments" -> (jsonObject(
            optional("EORIrequested" -> vatApplication.eoriRequested)) ++ {
            vatScheme.attachments.map(_.method) match {
              case Some(attachmentMethod) if mandatoryAttachmentList.nonEmpty =>
                val fullAttachmentList = mandatoryAttachmentList ++ attachmentsService.optionalAttachmentList(vatScheme)
                AttachmentType.submissionWrites(attachmentMethod).writes(fullAttachmentList).as[JsObject]
              case _ =>
                Json.obj()
            }
          })
        )
      case (None, Some(_)) =>
        errorLog("[AdminBlockBuilder][buildAdminBlock] - Could not build admin block for submission due to missing eligibility data")
        throw new InternalServerException("Could not build admin block for submission due to missing eligibility data")
      case (Some(_), None) =>
        errorLog("[AdminBlockBuilder][buildAdminBlock] - Could not build admin block for submission due to missing vat application data")
        throw new InternalServerException("Could not build admin block for submission due to missing vat application data")
      case _ =>
        errorLog("[AdminBlockBuilder][buildAdminBlock] - Could not build admin block for submission due to no available data")
        throw new InternalServerException("Could not build admin block for submission due to no available data")
    }
  }
}