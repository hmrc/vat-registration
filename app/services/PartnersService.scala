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

package services

import common.exceptions.UpdateFailed
import models.api.Partner
import models.registration.sections.PartnersSection
import play.api.libs.json.Reads
import repositories.VatSchemeRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PartnersService @Inject()(registrationMongoRepository: VatSchemeRepository)
                               (implicit ec: ExecutionContext) {

  val partnersBlock = "partners"
  val indexOffset = 1
  val recordsToReplace = 1

  def getPartner(regId: String, index: Int): Future[Option[Partner]] = {
    getPartners(regId).map {
      case Some(section) => section.partners.lift(index - indexOffset)
      case _ => None
    }
  }

  def storePartner(regId: String, partner: Partner, index: Int)(implicit reads: Reads[Partner]): Future[Partner] = {
    registrationMongoRepository.fetchBlock[PartnersSection](regId, partnersBlock)
      .map(_.getOrElse(PartnersSection(Nil)))
      .flatMap { section =>
        registrationMongoRepository.updateBlock(
          regId = regId,
          data = PartnersSection(section.partners.patch(index - indexOffset, List(partner), recordsToReplace)),
          key = partnersBlock
        ).map(_.partners.lift(index - indexOffset).getOrElse(throw UpdateFailed(regId, partnersBlock)))
      }
  }

  def getPartners(regId: String): Future[Option[PartnersSection]] =
    registrationMongoRepository.fetchBlock[PartnersSection](regId, partnersBlock)

  def deletePartner(regId: String, index: Int): Future[PartnersSection] =
    registrationMongoRepository.fetchBlock[PartnersSection](regId, partnersBlock)
      .map(_.getOrElse(PartnersSection(Nil)))
      .flatMap { section =>
        registrationMongoRepository.updateBlock[PartnersSection](
          regId = regId,
          data = PartnersSection(section.partners.patch(index - indexOffset, Nil, recordsToReplace)),
          key = partnersBlock
        )
      }

}

