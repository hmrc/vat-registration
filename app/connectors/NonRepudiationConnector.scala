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

package connectors

import org.apache.pekko.actor.Scheduler
import config.BackendConfig
import models.nonrepudiation._
import play.api.http.Status
import play.api.http.Status.{ACCEPTED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsHttpResponse, HttpResponse, StringContextOps}
import utils.{Delayer, LoggingUtils, Retrying}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Success, Try}

@Singleton
class NonRepudiationConnector @Inject() (httpClient: HttpClientV2, config: BackendConfig)(implicit
  val scheduler: Scheduler,
  val ec: ExecutionContext
) extends HttpReadsHttpResponse
    with Retrying
    with Delayer
    with LoggingUtils {

  def submitNonRepudiation(
    encodedPayloadString: String,
    nonRepudiationMetadata: NonRepudiationMetadata,
    digitalAttachmentIds: Seq[String]
  )(implicit hc: HeaderCarrier, request: Request[_]): Future[NonRepudiationSubmissionResult] = {
    val attachmentJson =
      if (digitalAttachmentIds.nonEmpty) Json.obj("attachmentIds" -> digitalAttachmentIds) else Json.obj()
    val jsonBody       = Json.obj(
      "payload"  -> encodedPayloadString,
      "metadata" -> (Json.toJson(nonRepudiationMetadata).as[JsObject] ++ attachmentJson)
    )

    val retryCondition: Try[NonRepudiationSubmissionResult] => Boolean = {
      case Success(value: NonRepudiationSubmissionFailed) if Status.isServerError(value.status) => true
      case _                                                                                    => false
    }

    retry[NonRepudiationSubmissionResult](config.nrsRetries, retryCondition) { _ =>
      httpClient
        .post(url"${config.nonRepudiationSubmissionUrl}")
        .withBody(jsonBody)
        .setHeader("X-API-Key" -> config.nonRepudiationApiKey)
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case ACCEPTED =>
              val submissionId = (response.json \ "nrSubmissionId").as[String]
              NonRepudiationSubmissionAccepted(submissionId)
            case _        =>
              NonRepudiationSubmissionFailed(response.body, response.status)
          }
        }
        .recover { case NonFatal(e) =>
          errorLog(s"[NonRepudiationConnector][submitNonRepudiation] errored with ${e.getMessage}")
          NonRepudiationSubmissionFailed(e.getMessage, INTERNAL_SERVER_ERROR)
        }
    }
  }

  def submitAttachmentNonRepudiation(
    payload: NonRepudiationAttachment
  )(implicit hc: HeaderCarrier, request: Request[_]): Future[NonRepudiationAttachmentResult] = {

    val retryCondition: Try[NonRepudiationAttachmentResult] => Boolean = {
      case Success(value: NonRepudiationAttachmentFailed) if Status.isServerError(value.status) => true
      case _                                                                                    => false
    }

    retry[NonRepudiationAttachmentResult](config.nrsRetries, retryCondition) { _ =>
      httpClient
        .post(url"${config.attachmentNonRepudiationSubmissionUrl}")
        .withBody(Json.toJson(payload))
        .setHeader("X-API-Key" -> config.nonRepudiationApiKey)
        .execute[HttpResponse]
        .map { response =>
          response.status match {
            case ACCEPTED =>
              val attachmentId = (response.json \ "attachmentId").as[String]
              NonRepudiationAttachmentAccepted(attachmentId)
            case _        =>
              NonRepudiationAttachmentFailed(response.body, response.status)
          }
        }
        .recover { case NonFatal(e) =>
          errorLog(s"[NonRepudiationConnector][submitNonRepudiation] errored with ${e.getMessage}")
          NonRepudiationAttachmentFailed(e.getMessage, INTERNAL_SERVER_ERROR)
        }
    }
  }
}
