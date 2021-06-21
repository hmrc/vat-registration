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

import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import repositories.trafficmanagement.{DailyQuotaRepository, TrafficManagementRepository}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.TimeMachine

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpdateTrafficManagementController @Inject()(cc: ControllerComponents,
                                                  dailyQuotaRepository: DailyQuotaRepository,
                                                  trafficManagementRepository: TrafficManagementRepository,
                                                  timeMachine: TimeMachine
                                                )(implicit ec: ExecutionContext) extends BackendController(cc) {

  val updateQuota: Action[JsValue] = Action.async(parse.json) { implicit request =>
    val optQuota = (request.body \ "quota").validate[Int].asOpt
    val optPartyType = (request.body \ "partyType").validate[String].asOpt
    val optIsEnrolled = (request.body \ "isEnrolled").validate[Boolean].asOpt

    (optQuota, optPartyType, optIsEnrolled) match {
      case (Some(quota), Some(partyType), Some(isEnrolled)) =>
        val query = Json.obj(
          "date" -> timeMachine.today,
          "partyType" -> partyType,
          "isEnrolled" -> isEnrolled
        )
        val update = Json.obj("$set" -> Json.obj("currentTotal" -> quota))

        dailyQuotaRepository.findAndUpdate(query, update, upsert = true) map (_ => Ok)
      case _ =>
        Future.successful(BadRequest)
    }
  }

  val clear: Action[AnyContent] = Action.async { implicit request =>
    trafficManagementRepository.removeAll().map (_ => NoContent)
  }

}
