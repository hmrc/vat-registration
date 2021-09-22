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

package controllers.test

import org.openapi4j.core.validation.ValidationException
import org.openapi4j.operation.validator.model.Request
import org.openapi4j.operation.validator.model.impl._
import org.openapi4j.operation.validator.validation.RequestValidator
import org.openapi4j.parser.OpenApi3Parser
import org.openapi4j.parser.model.v3.OpenApi3
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

@Singleton
class StubVatSubmissionController @Inject()(cc: ControllerComponents) extends BackendController(cc) with Logging {

  lazy val api: OpenApi3 = new OpenApi3Parser().parse(getClass.getResource("/vat-registration-api-schema.yaml"), false)

  lazy val requestValidator = new RequestValidator(api)

  val processSubmission: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      logger.info(s"[StubVatSubmissionController][processSubmission] Received submission: ${Json.prettyPrint(request.body)}")

      val openApiRequest: Request = {
        val requestBuilder = new DefaultRequest.Builder(
          request.uri.replace("/vatreg/test-only", ""),
          Request.Method.getMethod(request.method)
        )

        requestBuilder.body(Body.from(request.body.toString()))
        requestBuilder.headers(request.headers.toMap.map { case (string, sequence) =>
          string -> sequence.asJavaCollection
        }.asJava)

        requestBuilder.build()
      }

      Try {
        requestValidator.validate(openApiRequest)
      } match {
        case Success(_) => Future.successful(Ok(Json.stringify(Json.obj("formBundle" -> "1234"))))
        case Failure(exception: ValidationException) =>
          logger.error(s"[StubVatSubmissionController][processSubmission] ${exception.results().toString}")
          Future.successful(BadRequest)
      }
  }
}
