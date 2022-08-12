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

sealed trait NoUKBankAccount

case object BeingSetup extends NoUKBankAccount
case object OverseasAccount extends NoUKBankAccount
case object NameChange extends NoUKBankAccount
case object AccountNotInBusinessName extends NoUKBankAccount
case object DontWantToProvide extends NoUKBankAccount

object NoUKBankAccount {

  val beingSetup: String = "BeingSetup"
  val beingSetupId: String = "1"
  val overseasAccount: String = "OverseasAccount"
  val overseasAccountId: String = "3"
  val nameChange: String = "NameChange"
  val nameChangeId: String = "4"
  val accountNotInBusinessName: String = "AccountNotInBusinessName"
  val accountNotInBusinessNameId: String = "6"
  val dontWantToProvide: String = "DontWantToProvide"
  val dontWantToProvideId: String = "7"

  implicit val reads: Reads[NoUKBankAccount] = Reads[NoUKBankAccount] {
    case JsString(`beingSetup`) => JsSuccess(BeingSetup)
    case JsString(`overseasAccount`) => JsSuccess(OverseasAccount)
    case JsString(`nameChange`) => JsSuccess(NameChange)
    case JsString(`accountNotInBusinessName`) => JsSuccess(AccountNotInBusinessName)
    case JsString(`dontWantToProvide`) => JsSuccess(DontWantToProvide)
    case _ => JsError("Could not parse reason for no UK bank account")
  }

  implicit val writes: Writes[NoUKBankAccount] = Writes[NoUKBankAccount] {
    case BeingSetup => JsString(beingSetup)
    case OverseasAccount => JsString(overseasAccount)
    case NameChange => JsString(nameChange)
    case AccountNotInBusinessName => JsString(accountNotInBusinessName)
    case DontWantToProvide => JsString(dontWantToProvide)
  }

  implicit val format: Format[NoUKBankAccount] = Format(reads, writes)

  def reasonId(reason: NoUKBankAccount): String = reason match {
    case BeingSetup => beingSetupId
    case OverseasAccount => overseasAccountId
    case NameChange => nameChangeId
    case AccountNotInBusinessName => accountNotInBusinessNameId
    case DontWantToProvide => dontWantToProvideId
  }


}
