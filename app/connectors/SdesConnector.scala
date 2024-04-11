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

import config.BackendConfig
import httpparsers.SdesNotificationHttpParser.SdesNotificationHttpReads
import models.sdes.{SdesNotification, SdesNotificationResult}
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsHttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SdesConnector @Inject() (httpClient: HttpClientV2, config: BackendConfig)(implicit
  executionContext: ExecutionContext
) extends HttpReadsHttpResponse {

  def notifySdes(payload: SdesNotification)(implicit hc: HeaderCarrier): Future[SdesNotificationResult] =
    httpClient
      .post(url"${config.sdesNotificationUrl}")
      .withBody(Json.toJson(payload)(SdesNotification.format))
      .setHeader("x-client-id" -> config.sdesAuthorizationToken)
      .setHeader("Content-Type" -> "application/json")
      .execute[SdesNotificationResult](SdesNotificationHttpReads, executionContext)

}
