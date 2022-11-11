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

package controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.VatThresholdService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}

@Singleton
class VatThresholdController @Inject()(val vatThresholdService: VatThresholdService,
                                       controllerComponents: ControllerComponents) extends BackendController(controllerComponents){

  def getThresholdForDate(date: String): Action[AnyContent] = Action {
    _ =>
      val inputDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
      vatThresholdService.getThresholdForGivenDate(inputDate) match {
        case Some(vatThreshold) => Ok(Json.toJson(vatThreshold))
        case _ => NotFound
      }
  }
}