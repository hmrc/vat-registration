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

package models.api

import play.api.libs.json._

import java.time.LocalDate

case class AnnualAccountingScheme(joinAAS: Boolean,
                                  submissionType: Option[String],
                                  customerRequest: Option[AASDetails])

object AnnualAccountingScheme {
  implicit val format: Format[AnnualAccountingScheme] = Json.format[AnnualAccountingScheme]
}

case class AASDetails(paymentMethod: PaymentMethod,
                      annualStagger: AnnualStagger,
                      paymentFrequency: PaymentFrequency,
                      estimatedTurnover: BigDecimal,
                      requestedStartDate: LocalDate
                     )

object AASDetails {

  implicit val format: Format[AASDetails] = Json.format[AASDetails]

}
