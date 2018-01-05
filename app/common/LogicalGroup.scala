/*
 * Copyright 2018 HM Revenue & Customs
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

package common

import models.api._

trait LogicalGroup[T] {
  val name: String
}

object LogicalGroup {
  def apply[T]()(implicit t: LogicalGroup[T]): LogicalGroup[T] = t

  def apply[T](s: String): LogicalGroup[T] = new LogicalGroup[T] {
    val name: String = s
  }

  implicit val vatTradingDetails    = LogicalGroup[VatTradingDetails]("tradingDetails")
  implicit val vatSicAndCompliance  = LogicalGroup[VatSicAndCompliance]("vatSicAndCompliance")
  implicit val vatFinancials        = LogicalGroup[VatFinancials]("financials")
  implicit val vatContact           = LogicalGroup[VatContact]("vatContact")
  implicit val vatEligibility       = LogicalGroup[VatServiceEligibility]("vatEligibility")
  implicit val vatLodgingOfficer    = LogicalGroup[LodgingOfficer]("lodgingOfficer")
  implicit val vatFlatRateScheme    = LogicalGroup[VatFlatRateScheme]("vatFlatRateScheme")
  implicit val returns              = LogicalGroup[Returns]("returns")
}
