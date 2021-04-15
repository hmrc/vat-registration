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

package services.submission

import models.api.NoUKBankAccount
import play.api.libs.json.JsObject
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils.jsonObject

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BankDetailsBlockBuilder @Inject()(registrationMongoRepository: RegistrationMongoRepository)(implicit ec: ExecutionContext) {

  def buildBankDetailsBlock(regId: String): Future[JsObject] = for {
    optBankAccount <- registrationMongoRepository.fetchBankAccount(regId)
  } yield optBankAccount match {
    case Some(bankAccount) =>
      if (bankAccount.isProvided) {
        bankAccount.details match {
          case Some(bankAccountDetails) =>
            jsonObject(
              "UK" -> jsonObject(
                "accountName" -> bankAccountDetails.name,
                "sortCode" -> bankAccountDetails.sortCode.replaceAll("-", ""),
                "accountNumber" -> bankAccountDetails.number
              )
            )
          case None => throw new InternalServerException("Could not build bank details block for submission due to missing bank account details")
        }
      }
      else {
        jsonObject(
          "UK" -> jsonObject(
            "reasonBankAccNotProvided" -> bankAccount.reason.map(NoUKBankAccount.reasonId)
          )
        )
      }
    case None => throw new InternalServerException("Could not build bank details block for submission due to missing bank account")
  }

}
