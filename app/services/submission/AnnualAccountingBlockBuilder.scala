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

import models.api.AnnualAccountingScheme
import play.api.libs.json.JsObject
import repositories.RegistrationMongoRepository
import utils.JsonUtils.jsonObject

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AnnualAccountingBlockBuilder @Inject()(registrationMongoRepository: RegistrationMongoRepository)(implicit ec: ExecutionContext) {

  def buildAnnualAccountingBlock(regId: String): Future[Option[JsObject]] = for {
    optAnnualAccounting <- registrationMongoRepository.fetchAnnualAccountingScheme(regId)
  } yield optAnnualAccounting match {
    case Some(annualAccountingScheme: AnnualAccountingScheme) if annualAccountingScheme.joinAAS =>
      Some(jsonObject(
        "submissionType" -> annualAccountingScheme.submissionType,
        "customerRequest" -> annualAccountingScheme.customerRequest.map { customerRequest =>
          jsonObject(
            "paymentMethod" -> customerRequest.paymentMethod,
            "annualStagger" -> customerRequest.annualStagger,
            "paymentFrequency" -> customerRequest.paymentFrequency,
            "estimatedTurnover" -> customerRequest.estimatedTurnover,
            "reqStartDate" -> customerRequest.requestedStartDate
          )
        }
      ))
    case _ => None

  }

}
