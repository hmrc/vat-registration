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

import play.api.libs.json.JsObject
import repositories.VatSchemeRepository
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils.{optional, _}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ComplianceBlockBuilder @Inject()(registrationMongoRepository: VatSchemeRepository)
                                      (implicit ec: ExecutionContext) {

  def buildComplianceBlock(regId: String): Future[Option[JsObject]] = {
    for {
      optSicAndCompliance <- registrationMongoRepository.fetchSicAndCompliance(regId)
    } yield optSicAndCompliance match {
      case Some(sicAndCompliance) => sicAndCompliance.labourCompliance map (labourCompliance =>
        jsonObject(
          optional("numOfWorkersSupplied" -> labourCompliance.numOfWorkersSupplied),
          optional("intermediaryArrangement" -> labourCompliance.intermediaryArrangement),
          "supplyWorkers" -> labourCompliance.supplyWorkers
        ))
      case _ =>
        throw new InternalServerException("Couldn't build compliance block due to missing sicAndCompliance data")
    }
  }

}
