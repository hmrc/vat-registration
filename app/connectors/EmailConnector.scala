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
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsHttpResponse, HttpResponse, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailConnector @Inject()(httpClient: HttpClientV2)
                              (implicit ec: ExecutionContext, appConfig: BackendConfig) extends HttpReadsHttpResponse with Logging {

  def sendEmail(email: String, template: String, params: Map[String, String], force: Boolean)
               (implicit hc: HeaderCarrier): Future[EmailResponse] =

    httpClient.post(url"${appConfig.sendEmailUrl}")
      .withBody(
        Json.obj(
          "to" -> Json.arr(email),
          "templateId" -> template,
          "parameters" -> Json.toJson(params),
          "force" -> force
        )
      )
      .execute[HttpResponse]
      .map { res =>
        res.status match {
          case ACCEPTED =>
            EmailSent
          case status =>
            logger.warn(s"Unexpected status returned from Email service: $status")
            EmailFailedToSend
        }
      }

}

sealed trait EmailResponse

case object EmailSent extends EmailResponse

case object EmailFailedToSend extends EmailResponse

