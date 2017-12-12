/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import auth.Authenticated
import cats.instances.FutureInstances
import common.{LogicalGroup, RegistrationId}
import models.ElementPath
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import services.RegistrationService
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController

abstract class VatRegistrationBaseController extends BaseController with Authenticated with FutureInstances {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  @deprecated
  protected def patch[G: LogicalGroup : Format : Manifest](service: RegistrationService, id: RegistrationId): Action[JsValue] =
    Action.async(parse.json) {
      implicit request =>
        authenticated { _ =>
          withJsonBody((g: G) => {
            service.updateLogicalGroup(id, g).fold(
              a => a.toResult,
              b => Accepted(Json.toJson(b))
            )
          })
        }
    }

  @deprecated
  protected def delete[T](service: RegistrationService,
                          id: RegistrationId, elementPath: ElementPath): Action[AnyContent] =
    Action.async { implicit request =>
      authenticated { _ =>
        service.deleteByElement(id, elementPath).fold(
          a => a.toResult,
          b => Ok(Json.toJson(true)))
      }
    }

}
