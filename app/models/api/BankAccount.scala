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

import auth.CryptoSCRS
import models.registration.RegistrationSection
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class BankAccount(isProvided: Boolean,
                       details: Option[BankAccountDetails],
                       overseasDetails: Option[BankAccountOverseasDetails],
                       reason: Option[NoUKBankAccount]) extends RegistrationSection[BankAccount] {

  override def isComplete: BankAccount => Boolean = {
    _ => true
  }

}

case class BankAccountDetails(name: String,
                              sortCode: String,
                              number: String,
                              status: BankAccountDetailsStatus)

case class BankAccountOverseasDetails(name: String,
                                      bic: String,
                                      iban: String)

object BankAccount {
  implicit val format: Format[BankAccount] = Json.format[BankAccount]
}

object BankAccountDetails extends VatBankAccountValidator {
  implicit val format: Format[BankAccountDetails] = Json.format[BankAccountDetails]
}

object BankAccountOverseasDetails extends VatBankAccountValidator {
  implicit val format: Format[BankAccountOverseasDetails] = Json.format[BankAccountOverseasDetails]
}

object BankAccountDetailsMongoFormat extends VatBankAccountValidator {
  def format(crypto: CryptoSCRS): Format[BankAccountDetails] = (
    (__ \ "name").format[String] and
      (__ \ "sortCode").format[String] and
      (__ \ "number").format[String](crypto.rds)(crypto.wts) and
      (__ \ "status").format[BankAccountDetailsStatus]
    ) (BankAccountDetails.apply, unlift(BankAccountDetails.unapply))
}

object BankAccountOverseasDetailsMongoFormat extends VatBankAccountValidator {
  def format(crypto: CryptoSCRS): Format[BankAccountOverseasDetails] = (
    (__ \ "name").format[String] and
      (__ \ "bic").format[String](crypto.rds)(crypto.wts) and
      (__ \ "iban").format[String](crypto.rds)(crypto.wts)
    ) (BankAccountOverseasDetails.apply, unlift(BankAccountOverseasDetails.unapply))
}


object BankAccountMongoFormat extends VatBankAccountValidator {
  def encryptedFormat(crypto: CryptoSCRS): OFormat[BankAccount] = (
    (__ \ "isProvided").format[Boolean] and
      (__ \ "details").formatNullable[BankAccountDetails](BankAccountDetailsMongoFormat.format(crypto)) and
      (__ \ "overseasDetails").formatNullable[BankAccountOverseasDetails](BankAccountOverseasDetailsMongoFormat.format(crypto)) and
      (__ \ "reason").formatNullable[NoUKBankAccount]
    ) (BankAccount.apply, unlift(BankAccount.unapply))
}