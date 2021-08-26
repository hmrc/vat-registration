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
import httpparsers.VatSubmissionHttpParser.VatSubmissionHttpReads
import play.api.libs.json.JsObject
import uk.gov.hmrc.http._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VatSubmissionConnector @Inject()(appConfig: BackendConfig,
                                       http: HttpClient
                                      )(implicit executionContext: ExecutionContext) {

  def submit(submissionData: JsObject, correlationId: String, credentialId: String)(implicit hc: HeaderCarrier): Future[String] = {

    val submissionHeaders = Seq(
      "Authorization" -> appConfig.urlHeaderAuthorization,
      "Environment" -> appConfig.urlHeaderEnvironment,
      "CorrelationId" -> correlationId,
      "Credential-Id" -> credentialId,
      "Content-Type" -> "application/json"
    ) ++ hc.headers(Seq("X-Session-ID"))

    http.POST[JsObject, String](
      url = appConfig.vatSubmissionUrl,
      body = submissionData,
      headers = submissionHeaders
    )(
      wts = JsObject.writes,
      rds = VatSubmissionHttpReads,
      hc = hc,
      ec = executionContext
    )

  }

}
