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

package models.api

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.submission.{Individual, Partnership, UkCompany}
import models.submission.PartyType.toJsString
import play.api.libs.json.Json

class PartnerSpec extends VatRegSpec with VatRegistrationFixture {

  "parsing from JSON" must {
    "successfully parse a partner with valid Sole Trader details" in {
      val testJson = Json.obj(
        "details" -> Json.toJson(testSoleTraderEntity),
        "partyType" -> toJsString(Individual),
        "isLeadPartner" -> true
      )

      testJson.validate[Partner].asOpt mustBe Some(
        Partner(
          details = testSoleTraderEntity,
          partyType = Individual,
          isLeadPartner = true
        )
      )
    }
    "successfully parse a partner with valid Limited Company details" in {
      val testJson = Json.obj(
        "details" -> Json.toJson(testLtdCoEntity),
        "partyType" -> toJsString(UkCompany),
        "isLeadPartner" -> true
      )

      testJson.validate[Partner].asOpt mustBe Some(
        Partner(
          details = testLtdCoEntity,
          partyType = UkCompany,
          isLeadPartner = true
        )
      )
    }
    "successfully parse a partner with valid Partnership details" in {
      val testJson = Json.obj(
        "details" -> Json.toJson(testGeneralPartnershipEntity),
        "partyType" -> toJsString(Partnership),
        "isLeadPartner" -> false
      )

      testJson.validate[Partner].asOpt mustBe Some(
        Partner(
          details = testGeneralPartnershipEntity,
          partyType = Partnership,
          isLeadPartner = false
        )
      )
    }
    "successfully parse a partner without an index" in {
      val testJson = Json.obj(
        "details" -> Json.toJson(testGeneralPartnershipEntity),
        "partyType" -> toJsString(Partnership),
        "isLeadPartner" -> false
      )

      testJson.validate[Partner].asOpt mustBe Some(
        Partner(
          details = testGeneralPartnershipEntity,
          partyType = Partnership,
          isLeadPartner = false
        )
      )
    }
    "fail to parse parse a partner if the details json is incorrect for the party type" in {
      val testJson = Json.obj(
        "details" -> Json.toJson(testLtdCoEntity),
        "partyType" -> toJsString(Individual),
        "isLeadPartner" -> false
      )

      testJson.validate[Partner].asOpt mustBe None
    }
  }

  "writing to JSON" must {
    "successfully write a partner with valid Sole Trader details" in {
      val entity = Partner(
        details = testSoleTraderEntity,
        partyType = Individual,
        isLeadPartner = true
      )

      Json.toJson(entity) mustBe Json.obj(
        "details" -> Json.toJson(testSoleTraderEntity),
        "partyType" -> toJsString(Individual),
        "isLeadPartner" -> true
      )
    }
    "successfully write a partner with valid Limited Company details" in {
      val entity = Partner(
        details = testLtdCoEntity,
        partyType = UkCompany,
        isLeadPartner = true
      )

      Json.toJson(entity) mustBe Json.obj(
        "details" -> Json.toJson(testLtdCoEntity),
        "partyType" -> toJsString(UkCompany),
        "isLeadPartner" -> true
      )
    }
    "successfully write a partner with valid Partnership details" in {
      val entity = Partner(
        details = testGeneralPartnershipEntity,
        partyType = Partnership,
        isLeadPartner = false
      )

      Json.toJson(entity) mustBe Json.obj(
        "details" -> Json.toJson(testGeneralPartnershipEntity),
        "partyType" -> toJsString(Partnership),
        "isLeadPartner" -> false
      )
    }
    "successfully write a partner without an index" in {
      val entity = Partner(
        details = testGeneralPartnershipEntity,
        partyType = Partnership,
        isLeadPartner = false
      )

      Json.toJson(entity) mustBe Json.obj(
        "details" -> Json.toJson(testGeneralPartnershipEntity),
        "partyType" -> toJsString(Partnership),
        "isLeadPartner" -> false
      )
    }
  }

}
