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

package services.submission

import models.api.NoUKBankAccount.reasonId
import models.api.{BankAccount, IndeterminateStatus, OverseasAccount}
import models.submission.{NETP, NonUkNonEstablished}
import play.api.libs.json.JsObject
import repositories.VatSchemeRepository
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BankDetailsBlockBuilder @Inject()(registrationMongoRepository: VatSchemeRepository)(implicit ec: ExecutionContext) {

  def buildBankDetailsBlock(regId: String): Future[Option[JsObject]] = for {
    optBankAccount <- registrationMongoRepository.fetchBankAccount(regId)
    optPartyType <- registrationMongoRepository.fetchEligibilitySubmissionData(regId).map(_.map(_.partyType))
  } yield (optBankAccount, optPartyType) match {
    case (Some(BankAccount(true, Some(details), _, _)), Some(partyType)) if !List(NETP, NonUkNonEstablished).contains(partyType) =>
      Some(jsonObject(
        "UK" -> jsonObject(
          "accountName" -> details.name,
          "sortCode" -> details.sortCode.replaceAll("-", ""),
          "accountNumber" -> details.number,
          conditional(details.status.equals(IndeterminateStatus))("bankDetailsNotValid" -> true)
        )
      ))
    case (Some(BankAccount(true, _, Some(overseasDetails), _)), Some(NETP | NonUkNonEstablished)) =>
      Some(jsonObject(
        "Overseas" -> jsonObject(
          "name" -> overseasDetails.name,
          "bic" -> overseasDetails.bic,
          "iban" -> overseasDetails.iban
        )
      ))
    case (Some(BankAccount(false, _, _, Some(reason))), _) =>
      Some(jsonObject(
        "UK" -> jsonObject(
          "reasonBankAccNotProvided" -> reasonId(reason)
        )
      ))
    case (None, Some(NETP | NonUkNonEstablished)) =>
      Some(jsonObject("UK" -> jsonObject(
        "reasonBankAccNotProvided" -> reasonId(OverseasAccount)
      )))
    case _ => throw new InternalServerException("Could not build bank details block for submission due to missing bank account")
  }
}
