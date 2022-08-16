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

package connectors

import config.BackendConfig
import models.nonrepudiation._
import play.api.http.Status.ACCEPTED
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpReadsHttpResponse, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NonRepudiationConnector @Inject()(httpClient: HttpClient, config: BackendConfig)(implicit ec: ExecutionContext)
  extends HttpReadsHttpResponse {

  def submitNonRepudiation(encodedPayloadString: String,
                           nonRepudiationMetadata: NonRepudiationMetadata,
                           digitalAttachmentIds: Seq[String])
                          (implicit hc: HeaderCarrier): Future[NonRepudiationSubmissionResult] = {
    val attachmentJson = if (digitalAttachmentIds.nonEmpty) Json.obj("attachmentIds" -> digitalAttachmentIds) else Json.obj()
    val jsonBody = Json.obj(
      "payload" -> encodedPayloadString,
      "metadata" -> (Json.toJson(nonRepudiationMetadata).as[JsObject] ++ attachmentJson)
    )

    httpClient.POST[JsValue, HttpResponse](
      url = config.nonRepudiationSubmissionUrl,
      body = jsonBody,
      headers = Seq("X-API-Key" -> config.nonRepudiationApiKey)
    ).map {
      response =>
        response.status match {
          case ACCEPTED =>
            val submissionId = (response.json \ "nrSubmissionId").as[String]
            NonRepudiationSubmissionAccepted(submissionId)
          case _ =>
            NonRepudiationSubmissionFailed(response.body, response.status)
        }
    }
  }

  def submitAttachmentNonRepudiation(payload: NonRepudiationAttachment)(implicit hc: HeaderCarrier): Future[NonRepudiationAttachmentResult] =
    httpClient.POST[NonRepudiationAttachment, HttpResponse](
      url = config.attachmentNonRepudiationSubmissionUrl,
      body = payload,
      headers = Seq("X-API-Key" -> config.nonRepudiationApiKey)
    ).map {
      response =>
        response.status match {
          case ACCEPTED =>
            val attachmentId = (response.json \ "attachmentId").as[String]
            NonRepudiationAttachmentAccepted(attachmentId)
          case _ =>
            NonRepudiationAttachmentFailed(response.body, response.status)
        }
    }
}
