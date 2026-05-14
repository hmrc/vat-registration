/*
 * Copyright 2026 HM Revenue & Customs
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

import models.BuildFailure
import models.api.NoUKBankAccount.reasonId
import models.api._
import models.submission.{Individual, NonUkNonEstablished}
import play.api.libs.json.JsObject
import utils.JsonUtils._

object BankDetailsBlockBuilder {

  def buildBankDetailsBlock(vatScheme: VatScheme): Either[BuildFailure, JsObject] =
    (vatScheme.bankAccount, vatScheme.partyType) match {
      // yes + bank details + any partyType
      case (Some(BankAccount(true, Some(bankAccountDetails), _, _)), Some(_)) => // TODO does partyType need to exist
        Right(buildJsonForBankDetails(bankAccountDetails, bankAccountDetails.status.isInvalid))
      // no + reason
      case (Some(BankAccount(false, _, Some(reason), _)), _) =>
        Right(buildJsonForReasonWithNoBankDetails(reason))
      case (_, Some(Individual | NonUkNonEstablished)) =>
        Right(buildJsonForReasonWithNoBankDetails(OverseasAccount))
      case _ =>
        Left(
          BuildFailure(
            "Unable to build submission model as user has not give bank details, no bank details reason, nor is a NonUK/NonEstablished user "))
    }

  private def buildJsonForBankDetails(bankAccountDetails: BankAccountDetails, detailsAreInvalid: Boolean): JsObject =
    jsonObject(
      "UK" -> jsonObject(
        "accountName"   -> bankAccountDetails.name,
        "sortCode"      -> bankAccountDetails.sortCode.replaceAll("-", ""),
        "accountNumber" -> bankAccountDetails.number,
        optional("rollNumber"                                -> bankAccountDetails.rollNumber),
        conditional(detailsAreInvalid)("bankDetailsNotValid" -> true)
      )
    )

  private def buildJsonForReasonWithNoBankDetails(reason: NoUKBankAccount): JsObject =
    jsonObject(
      "UK" -> jsonObject(
        "reasonBankAccNotProvided" -> reasonId(reason)
      )
    )

}
