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

package services

import auth.CryptoSCRS
import models.api._
import models.registration._
import play.api.libs.json._
import play.api.mvc.Request
import uk.gov.hmrc.http.InternalServerException
import utils.LoggingUtils

import javax.inject.{Inject, Singleton}

@Singleton
class CipherService @Inject() (crypto: CryptoSCRS) extends LoggingUtils {

  def conditionallyEncrypt(section: RegistrationSectionId, json: JsValue)(implicit request: Request[_]): JsValue =
    section match {
      case BankAccountSectionId =>
        val bankAccount = json
          .validate[BankAccount]
          .getOrElse {
            errorLog("[CipherService][conditionallyEncrypt] - BankAccount format is invalid for encryption")
            throw new InternalServerException("BankAccount format is invalid for encryption.")
          }
        Json.toJson(bankAccount)(BankAccountMongoFormat.encryptedFormat(crypto))
      case _                    => json
    }

  def conditionallyDecrypt(section: RegistrationSectionId, json: JsValue)(implicit request: Request[_]): JsValue =
    section match {
      case BankAccountSectionId =>
        val bankAccount = json
          .validate[BankAccount](BankAccountMongoFormat.encryptedFormat(crypto))
          .getOrElse {
            errorLog("[CipherService][conditionallyDecrypt] - BankAccount format is invalid for decryption")
            throw new InternalServerException("BankAccount format is invalid for decryption.")
          }
        Json.toJson(bankAccount)
      case _                    => json
    }
}
