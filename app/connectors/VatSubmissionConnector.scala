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

package connectors

import config.BackendConfig
import play.api.http.Status.{BAD_REQUEST, CONFLICT, OK}
import play.api.libs.json.{JsObject, JsSuccess}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatSubmissionConnector @Inject()(appConfig: BackendConfig,
                                       http: HttpClient
                                      )(implicit executionContext: ExecutionContext) {

  private val CodeKey = "code"
  private val InvalidPayloadKey = "INVALID_PAYLOAD"
  private val InvalidSessionIdKey = "INVALID_SESSIONID"
  private val InvalidCredentialIdKey = "INVALID_CREDENTIALID"

  def submit(submissionData: JsObject, correlationId: String, credentialId: String)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val submissionHeaders = Seq(
      "Authorization" -> appConfig.urlHeaderAuthorization,
      "Environment" -> appConfig.urlHeaderEnvironment,
      "CorrelationId" -> correlationId,
      "Credential-Id" -> credentialId,
      "Content-Type" -> "application/json"
    ) ++ hc.headers(Seq("X-Session-ID"))

    http.POST[JsObject, HttpResponse](appConfig.vatSubmissionUrl, submissionData, submissionHeaders) map { response =>
      response.status match {
        case OK =>
          response
        case BAD_REQUEST =>
          (response.json \ CodeKey).validate[String] match {
            case JsSuccess(InvalidPayloadKey, _) =>
              throw new InternalServerException("VAT Submission API - invalid payload")
            case JsSuccess(InvalidSessionIdKey, _) =>
              throw new InternalServerException("VAT Submission API - invalid Session ID")
            case JsSuccess(InvalidCredentialIdKey, _) =>
              throw new InternalServerException("VAT Submission API - invalid Credential ID")
            case _ =>
              throw new InternalServerException(s"Unexpected Json response for this status: $BAD_REQUEST")
          }
        case CONFLICT =>
          throw new InternalServerException("VAT Submission API - application already in progress")
        case _ =>
          throw new InternalServerException(
            s"Unexpected response from VAT Submission API - status = ${response.status}, body = ${response.body}"
          )
      }
    }

  }

}
