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

package controllers.test

import config.BackendConfig
import models.sdes.PropertyExtractor.locationKey
import models.sdes.{Property, SdesCallback, SdesNotification}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpReadsHttpResponse, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class StubSdesController @Inject() (cc: ControllerComponents, httpClient: HttpClientV2, config: BackendConfig)(implicit
  executionContext: ExecutionContext
) extends BackendController(cc)
    with Logging
    with HttpReadsHttpResponse {

  def notificationStub: Action[SdesNotification] = Action.async(parse.json[SdesNotification]) { implicit request =>
    val url     = config.vatRegistrationUrl + controllers.routes.SdesController.sdesCallback.url
    val payload = SdesCallback(
      notification = "FileReceived",
      filename = request.body.file.name,
      correlationID = request.body.audit.correlationID,
      dateTime = LocalDateTime.now(),
      checksumAlgorithm = Some(request.body.file.checksum.algorithm),
      checksum = Some(request.body.file.checksum.value),
      availableUntil = Some(LocalDateTime.now().plusDays(1)),
      properties = request.body.file.properties ++ Some(Property(locationKey, "testLocation")),
      failureReason = None
    )

    httpClient
      .post(url"$url")
      .withBody(Json.toJson(payload))
      .execute[HttpResponse]
      .map(_ => NoContent)
  }
}
