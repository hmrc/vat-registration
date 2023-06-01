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

package services.monitoring

import models.api._
import models.submission._
import play.api.libs.json.JsObject
import play.api.mvc.Request
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._
import utils.LoggingUtils

import javax.inject.Singleton

@Singleton
class BankAuditBlockBuilder extends LoggingUtils{

  def buildBankAuditBlock(vatScheme: VatScheme)(implicit request:Request[_]): JsObject = {
    vatScheme.bankAccount match {
      case Some(bankAccount) =>
        if (bankAccount.isProvided) {
          bankAccount.details match {
            case Some(bankAccountDetails: BankAccountDetails) =>
              jsonObject(
                "accountName" -> bankAccountDetails.name,
                "sortCode" -> bankAccountDetails.sortCode,
                "accountNumber" -> bankAccountDetails.number,
                conditional(bankAccountDetails.status.equals(IndeterminateStatus))("bankDetailsNotValid" -> true)
              )
            case None =>
              errorLog("[BankAuditBlockBuilder][buildBankAuditBlock] - Could not build bank details block for audit due to missing bank account details")
              throw new InternalServerException("[BankAuditBlockBuilder]: Could not build bank details block for audit due to missing bank account details")
          }
        }
        else {
          jsonObject(
            "reasonBankAccNotProvided" -> bankAccount.reason
          )
        }
      case None if vatScheme.eligibilitySubmissionData.exists(data => List(NETP, NonUkNonEstablished).contains(data.partyType)) =>
        jsonObject(
          "reasonBankAccNotProvided" -> NoUKBankAccount.overseasAccount
        )
      case None =>
        errorLog("[BankAuditBlockBuilder][buildBankAuditBlock] - Could not build bank details block for audit due to missing bank account")
        throw new InternalServerException("BankAuditBlockBuilder: Could not build bank details block for audit due to missing bank account")
    }

  }
}