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

import featureswitch.core.config.{FeatureSwitching, SubmitBarsInvalidBankDetailsToAPI}
import models.BuildFailure
import models.api.NoUKBankAccount.reasonId
import models.api._
import models.submission.{Individual, NonUkNonEstablished}
import play.api.libs.json.JsObject
import utils.JsonUtils._

object BankDetailsBlockBuilder extends FeatureSwitching {

  def buildBankDetailsBlock(vatScheme: VatScheme): Either[BuildFailure, JsObject] =
    if (isEnabled(SubmitBarsInvalidBankDetailsToAPI)) buildBankDetailsBlockNew(vatScheme) else buildBankDetailsBlockOld(vatScheme)

  private def buildBankDetailsBlockNew(vatScheme: VatScheme): Either[BuildFailure, JsObject] =
    vatScheme.bankAccount match {
      case Some(BankAccount(_, Some(bankAccountDetails), _, _)) if bankAccountDetails.status.isEmpty =>
        Left(BuildFailure("[BankDetailsBlockBuilder] Unable to build submission model: bank details have not yet been BARS checked"))

      // overseas-account = reason + no-bank
      case _ if vatScheme.partyTypeIsIndividualOrNonUkNonEstablished =>
        Right(buildJsonForBankDetailsAndOrReason(bankAccountDetails = None, reason = Some(OverseasAccount)))

      // yes -> bank-details -> success = bank + valid
      case Some(BankAccount(true, Some(bankAccountDetails), None, _)) =>
        Right(buildJsonForBankDetailsAndOrReason(Some(bankAccountDetails), reason = None))

      // yes -> bank-details -> 3x fail = bank + invalid + lockout-fail-reason
      case Some(BankAccount(true, Some(bankAccountDetails), Some(DontWantToProvide), _)) =>
        Right(buildJsonForBankDetailsAndOrReason(Some(bankAccountDetails), Some(DontWantToProvide)))

      // yes -> bank-details -> fail -> back -> no -> reason = bank + invalid + reason
      case Some(BankAccount(false, Some(bankAccountDetails), Some(reason), _)) =>
        Right(buildJsonForBankDetailsAndOrReason(Some(bankAccountDetails), Some(reason)))

      // yes -> bank-details -> success -> back -> no -> reason = reason
      // no  -> reason = reason
      case Some(BankAccount(false, None, Some(reason), _)) =>
        Right(buildJsonForBankDetailsAndOrReason(bankAccountDetails = None, Some(reason)))

      case invalidDetails =>
        Left(
          BuildFailure(
            s"[BankDetailsBlockBuilder] Unable to build submission model: " +
              s"${invalidDetails.fold("No BankAccount model")(_.invalidBuildReason)}"))
    }

  private def buildJsonForBankDetailsAndOrReason(bankAccountDetails: Option[BankAccountDetails], reason: Option[NoUKBankAccount]): JsObject =
    jsonObject(
      "UK" -> jsonObject(
        optional("accountName"                                                           -> bankAccountDetails.map(_.name)),
        optional("sortCode"                                                              -> bankAccountDetails.map(_.sortCode.replaceAll("-", ""))),
        optional("accountNumber"                                                         -> bankAccountDetails.map(_.number)),
        optional("rollNumber"                                                            -> bankAccountDetails.flatMap(_.rollNumber)),
        conditional(bankAccountDetails.exists(_.statusIsNotValid))("bankDetailsNotValid" -> true),
        optional("reasonBankAccNotProvided"                                              -> reason.map(reasonId))
      )
    )

  private def buildBankDetailsBlockOld(vatScheme: VatScheme): Either[BuildFailure, JsObject] =
    (vatScheme.bankAccount, vatScheme.partyType) match {
      case (Some(BankAccount(true, Some(details), _, _)), Some(_)) =>
        Right(
          jsonObject(
            "UK" -> jsonObject(
              "accountName"   -> details.name,
              "sortCode"      -> details.sortCode.replaceAll("-", ""),
              "accountNumber" -> details.number,
              optional("rollNumber"                                       -> details.rollNumber),
              conditional(details.statusIsNotValid)("bankDetailsNotValid" -> true)
            )
          )
        )
      case (Some(BankAccount(false, _, Some(reason), _)), _) =>
        Right(
          jsonObject(
            "UK" -> jsonObject(
              "reasonBankAccNotProvided" -> reasonId(reason)
            )
          )
        )
      case (_, Some(Individual | NonUkNonEstablished)) =>
        Right(
          jsonObject(
            "UK" -> jsonObject(
              "reasonBankAccNotProvided" -> reasonId(OverseasAccount)
            )
          )
        )
      case _ =>
        Left(BuildFailure(
          "[BankDetailsBlockBuilder] Unable to build submission model as user has not given bank details, nor bank details reason, nor is a NonUK/NonEstablished user"))
    }

}
