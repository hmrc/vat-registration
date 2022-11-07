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

package models.api

import play.api.libs.json._

import java.time.LocalDate

case class FlatRateScheme(joinFrs: Option[Boolean] = None,
                          overBusinessGoods: Option[Boolean] = None,
                          estimateTotalSales: Option[BigDecimal] = None,
                          overBusinessGoodsPercent: Option[Boolean] = None,
                          useThisRate: Option[Boolean] = None,
                          frsStart: Option[LocalDate] = None,
                          categoryOfBusiness: Option[String] = None,
                          percent: Option[BigDecimal] = None,
                          limitedCostTrader: Option[Boolean] = None)

object FlatRateScheme {
  val oldRepoReads: Reads[FlatRateScheme] = (json: JsValue) => {
    val joinFrs = (json \ "joinFrs").as[Boolean]
    val details = (json \ "frsDetails").validateOpt[JsObject].get
    val start = details.flatMap { js =>
      val date = (js \ "startDate").asOpt[LocalDate]
      (joinFrs, date) match {
        case (true, None) => None
        case (false, _) => None
        case _ => date
      }
    }

    val businessGoods = details.flatMap(js => (js \ "businessGoods").validateOpt[JsObject].get)

    JsSuccess(FlatRateScheme(
      Some(joinFrs),
      if (!joinFrs && details.isEmpty) None else Some(businessGoods.isDefined),
      businessGoods.map(js => (js \ "estimatedTotalSales").as[BigDecimal]),
      businessGoods.map(js => (js \ "overTurnover").as[Boolean]),
      if (details.isEmpty) None else Some(joinFrs),
      start,
      details.flatMap(js => (js \ "categoryOfBusiness").asOpt[String]),
      details.flatMap(js => (js \ "percent").asOpt[BigDecimal]),
      details.flatMap(js => (js \ "limitedCostTrader").asOpt[Boolean])
    ))
  }

  val fallbackReads: Reads[FlatRateScheme] = //TODO: replace with Json.reads[FlatRateScheme] 2 weeks after this is merged
    (__ \ "frsDetails").readNullable[JsObject].flatMap {
      case None => Json.reads[FlatRateScheme]
      case _ => oldRepoReads
    }
  val writes: Writes[FlatRateScheme] = Json.writes[FlatRateScheme]
  implicit val format: Format[FlatRateScheme] = Format(fallbackReads, writes)
}