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

package models.api

import auth.CryptoSCRS
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class BankAccount(isProvided: Boolean, details: Option[BankAccountDetails], reason: Option[NoUKBankAccount])

case class BankAccountDetails(name: String, sortCode: String, number: String, status: BankAccountDetailsStatus)

object BankAccount {
  implicit val format: Format[BankAccount] = Json.format[BankAccount]
}

object BankAccountDetails {
  implicit val format: Format[BankAccountDetails] = Json.format[BankAccountDetails]
}

object BankAccountDetailsMongoFormat {
  def format(crypto: CryptoSCRS): Format[BankAccountDetails] = (
    (__ \ "name").format[String] and
      (__ \ "sortCode").format[String] and
      (__ \ "number").format[String](crypto.rds)(crypto.wts) and
      (__ \ "status").format[BankAccountDetailsStatus]
  )(BankAccountDetails.apply, unlift(BankAccountDetails.unapply))
}

object BankAccountMongoFormat {
  def encryptedFormat(crypto: CryptoSCRS): OFormat[BankAccount] = (
    (__ \ "isProvided").format[Boolean] and
      (__ \ "details").formatNullable[BankAccountDetails](BankAccountDetailsMongoFormat.format(crypto)) and
      (__ \ "reason").formatNullable[NoUKBankAccount]
  )(BankAccount.apply, unlift(BankAccount.unapply))
}
