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

package services.monitoring

import models.api.VatScheme
import models.submission.{NETP, NonUkNonEstablished}
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils.jsonObject

import javax.inject.Singleton

@Singleton
class BankAuditBlockBuilder {

  def buildBankAuditBlock(vatScheme: VatScheme): JsObject = {
    vatScheme.bankAccount match {
      case Some(bankAccount) =>
        if (bankAccount.isProvided) {
          bankAccount.details match {
            case Some(bankAccountDetails) =>
              jsonObject(
                "accountName" -> bankAccountDetails.name,
                "sortCode" -> bankAccountDetails.sortCode,
                "accountNumber" -> bankAccountDetails.number
              )
            case None =>
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
          "reasonBankAccNotProvided" -> "3"
        )
      case None =>
        throw new InternalServerException("BankAuditBlockBuilder: Could not build bank details block for audit due to missing bank account")
    }

  }
}