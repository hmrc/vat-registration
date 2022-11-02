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
import httpparsers.VatSubmissionHttpParser.{VatSubmissionHttpReads, VatSubmissionResponse}
import play.api.libs.json.JsObject
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatSubmissionConnector @Inject()(appConfig: BackendConfig,
                                       http: HttpClientV2
                                      )(implicit executionContext: ExecutionContext) {

  def submit(submissionData: JsObject, correlationId: String, credentialId: String)(implicit hc: HeaderCarrier): Future[VatSubmissionResponse] =
    http.post(url"${appConfig.vatSubmissionUrl}")
      .withBody(submissionData)
      .setHeader("Authorization" -> appConfig.urlHeaderAuthorization)
      .setHeader("Environment" -> appConfig.urlHeaderEnvironment)
      .setHeader("CorrelationId" -> correlationId)
      .setHeader("Credential-Id" -> credentialId)
      .setHeader("Content-Type" -> "application/json")
      .setHeader(hc.headers(Seq("X-Session-ID")): _*)
      .execute[VatSubmissionResponse](VatSubmissionHttpReads, executionContext)

}
