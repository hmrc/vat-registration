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

package services.submission

import models.api.NoUKBankAccount.reasonId
import models.api._
import models.submission.{Individual, NonUkNonEstablished}
import play.api.libs.json.JsObject
import play.api.mvc.Request
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._
import utils.LoggingUtils

import javax.inject.{Inject, Singleton}

@Singleton
class BankDetailsBlockBuilder @Inject() () extends LoggingUtils {

  def buildBankDetailsBlock(vatScheme: VatScheme)(implicit request: Request[_]): Option[JsObject] =
    (vatScheme.bankAccount, vatScheme.partyType) match {
      case (Some(BankAccount(true, Some(details), _)), Some(partyType)) =>
        Some(
          jsonObject(
            "UK" -> jsonObject(
              "accountName"   -> details.name,
              "sortCode"      -> details.sortCode.replaceAll("-", ""),
              "accountNumber" -> details.number,
              conditional(List(IndeterminateStatus, InvalidStatus).contains(details.status))(
                "bankDetailsNotValid" -> true
              )
            )
          )
        )
      case (Some(BankAccount(false, _, Some(reason))), _)               =>
        Some(
          jsonObject(
            "UK" -> jsonObject(
              "reasonBankAccNotProvided" -> reasonId(reason)
            )
          )
        )
      case (_, Some(Individual | NonUkNonEstablished))                        =>
        Some(
          jsonObject(
            "UK" -> jsonObject(
              "reasonBankAccNotProvided" -> reasonId(OverseasAccount)
            )
          )
        )
      case _                                                            =>
        errorLog(
          "[BankDetailsBlockBuilder][buildBankDetailsBlock] - Could not build bank details block for submission due to missing bank account"
        )
        throw new InternalServerException(
          "Could not build bank details block for submission due to missing bank account"
        )
    }
}
