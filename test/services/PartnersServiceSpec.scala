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

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.Partner
import models.registration.sections.PartnersSection
import models.submission.{Individual, Partnership, UkCompany}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api.libs.json.{Reads, Writes}
import play.api.test.Helpers._

import scala.concurrent.Future

class PartnersServiceSpec extends VatRegSpec with VatRegistrationFixture {
  val partnersBlockKey = "partners"

  val testSoleTraderPartner = Partner(testSoleTraderEntity, Individual, isLeadPartner = true)
  val testLtdCoPartner = testSoleTraderPartner.copy(details = testLtdCoEntity, partyType = UkCompany)
  val testPartnershipPartner = testSoleTraderPartner.copy(details = testGeneralPartnershipEntity, partyType = Partnership)

  object Service extends PartnersService(mockRegistrationMongoRepository)

  "getPartner" must {
    "return a partner if it exists" in {
      when(mockRegistrationMongoRepository.fetchBlock[PartnersSection](ArgumentMatchers.eq(testRegId), ArgumentMatchers.eq(partnersBlockKey))(any()))
        .thenReturn(Future.successful(Some(PartnersSection(List(testSoleTraderPartner)))))

      val res = await(Service.getPartner(testRegId, index = 1))

      res mustBe Some(testSoleTraderPartner)
    }
    "return None if the partner doesn't exist" in {
      when(mockRegistrationMongoRepository.fetchBlock[PartnersSection](ArgumentMatchers.eq(testRegId), ArgumentMatchers.eq(partnersBlockKey))(any()))
        .thenReturn(Future.successful(None))

      val res = await(Service.getPartner(testRegId, index = 1))

      res mustBe None
    }
  }

  "upsertPartner" must {
    "add a partner at the specified index" in {
      val updatedList = List(testSoleTraderPartner, testLtdCoPartner)

      when(mockRegistrationMongoRepository.fetchBlock[PartnersSection](
        ArgumentMatchers.eq(testRegId),
        ArgumentMatchers.eq(partnersBlockKey))(any[Reads[PartnersSection]])
      ).thenReturn(Future.successful(Some(PartnersSection(List(testSoleTraderPartner)))))

      when(mockRegistrationMongoRepository.updateBlock[PartnersSection](
        ArgumentMatchers.eq(testRegId),
        ArgumentMatchers.eq(PartnersSection(updatedList)),
        ArgumentMatchers.eq(partnersBlockKey)
      )(any[Writes[PartnersSection]])).thenReturn(Future.successful(PartnersSection(updatedList)))

      val res = await(Service.storePartner(testRegId, index = 2, partner = testLtdCoPartner))

      res mustBe testLtdCoPartner
    }
  }

  "getPartners" must {
    "return all partners" in {
      val partnerList = List(testSoleTraderPartner)

      when(mockRegistrationMongoRepository.fetchBlock[PartnersSection](ArgumentMatchers.eq(testRegId), ArgumentMatchers.eq(partnersBlockKey))(any()))
        .thenReturn(Future.successful(Some(PartnersSection(partnerList))))

      val res = await(Service.getPartners(testRegId))

      res mustBe Some(PartnersSection(partnerList))
    }
  }

  "deletePartner" must {
    "remove a partner at the specified index" in {
      when(mockRegistrationMongoRepository.fetchBlock[PartnersSection](ArgumentMatchers.eq(testRegId), ArgumentMatchers.eq(partnersBlockKey))(any()))
        .thenReturn(Future.successful(Some(PartnersSection(List(testSoleTraderPartner)))))

      when(mockRegistrationMongoRepository.updateBlock[PartnersSection](
        ArgumentMatchers.eq(testRegId),
        ArgumentMatchers.eq(PartnersSection(Nil)),
        ArgumentMatchers.eq(partnersBlockKey)
      )(any())).thenReturn(Future.successful(PartnersSection(Nil)))

      val res = await(Service.deletePartner(testRegId, index = 1))

      res mustBe PartnersSection(Nil)
    }
  }

}
