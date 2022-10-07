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

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.submission.PartyType.toJsString
import models.submission.{Individual, Partnership, ScotPartnership, UkCompany}
import play.api.libs.json.Json

class EntitySpec extends VatRegSpec with VatRegistrationFixture {

  "parsing from JSON" must {
    "successfully parse a partner with valid Sole Trader details" in {
      val testJson = Json.obj(
        "details" -> Json.toJson(testSoleTraderEntity),
        "partyType" -> toJsString(Individual),
        "isLeadPartner" -> true,
        "address" -> Json.toJson(testAddress),
        "email" -> testEmail,
        "telephoneNumber" -> testTelephone
      )

      testJson.validate[Entity].asOpt mustBe Some(
        Entity(
          details = Some(testSoleTraderEntity),
          partyType = Individual,
          isLeadPartner = Some(true),
          address = Some(testAddress),
          email = Some(testEmail),
          telephoneNumber = Some(testTelephone)
        )
      )
    }
    "successfully parse a partner with valid Limited Company details" in {
      val testJson = Json.obj(
        "details" -> Json.toJson(testLtdCoEntity),
        "partyType" -> toJsString(UkCompany),
        "isLeadPartner" -> true,
        "address" -> Json.toJson(testAddress),
        "email" -> testEmail,
        "telephoneNumber" -> testTelephone
      )

      testJson.validate[Entity].asOpt mustBe Some(
        Entity(
          details = Some(testLtdCoEntity),
          partyType = UkCompany,
          isLeadPartner = Some(true),
          address = Some(testAddress),
          email = Some(testEmail),
          telephoneNumber = Some(testTelephone)
        )
      )
    }
    "successfully parse a partner with valid Partnership details" in {
      val testJson = Json.obj(
        "details" -> Json.toJson(testGeneralPartnershipEntity),
        "partyType" -> toJsString(Partnership),
        "isLeadPartner" -> false,
        "address" -> Json.toJson(testAddress),
        "email" -> testEmail,
        "telephoneNumber" -> testTelephone
      )

      testJson.validate[Entity].asOpt mustBe Some(
        Entity(
          details = Some(testGeneralPartnershipEntity),
          partyType = Partnership,
          isLeadPartner = Some(false),
          address = Some(testAddress),
          email = Some(testEmail),
          telephoneNumber = Some(testTelephone)
        )
      )
    }

    "successfully parse a partner with valid Scottish Partnership details" in {
      val testPartnershipName = "testPartnershipName"
      val testJson = Json.obj(
        "details" -> Json.toJson(testGeneralPartnershipEntity.copy(companyName = None)),
        "partyType" -> toJsString(ScotPartnership),
        "isLeadPartner" -> false,
        "optScottishPartnershipName" -> testPartnershipName,
        "address" -> Json.toJson(testAddress),
        "email" -> testEmail,
        "telephoneNumber" -> testTelephone
      )

      testJson.validate[Entity].asOpt mustBe Some(
        Entity(
          details = Some(testGeneralPartnershipEntity.copy(companyName = Some(testPartnershipName))),
          partyType = ScotPartnership,
          isLeadPartner = Some(false),
          optScottishPartnershipName = Some(testPartnershipName),
          address = Some(testAddress),
          email = Some(testEmail),
          telephoneNumber = Some(testTelephone)
        )
      )
    }

    "fail to parse parse a partner if the details json is incorrect for the party type" in {
      val testJson = Json.obj(
        "details" -> Json.toJson(testLtdCoEntity),
        "partyType" -> toJsString(Individual),
        "isLeadPartner" -> false
      )

      testJson.validate[Entity].asOpt mustBe None
    }
  }

  "writing to JSON" must {
    "successfully write a partner with valid Sole Trader details" in {
      val entity = Entity(
        details = Some(testSoleTraderEntity),
        partyType = Individual,
        isLeadPartner = Some(true),
        address = None,
        email = None,
        telephoneNumber = None
      )

      Json.toJson(entity) mustBe Json.obj(
        "details" -> Json.toJson(testSoleTraderEntity),
        "partyType" -> toJsString(Individual),
        "isLeadPartner" -> true
      )
    }
    "successfully write a partner with valid Limited Company details" in {
      val entity = Entity(
        details = Some(testLtdCoEntity),
        partyType = UkCompany,
        isLeadPartner = Some(true),
        address = None,
        email = None,
        telephoneNumber = None
      )

      Json.toJson(entity) mustBe Json.obj(
        "details" -> Json.toJson(testLtdCoEntity),
        "partyType" -> toJsString(UkCompany),
        "isLeadPartner" -> true
      )
    }
    "successfully write a partner with valid Partnership details" in {
      val entity = Entity(
        details = Some(testGeneralPartnershipEntity),
        partyType = Partnership,
        isLeadPartner = Some(false),
        address = None,
        email = None,
        telephoneNumber = None
      )

      Json.toJson(entity) mustBe Json.obj(
        "details" -> Json.toJson(testGeneralPartnershipEntity),
        "partyType" -> toJsString(Partnership),
        "isLeadPartner" -> false
      )
    }
  }

}
