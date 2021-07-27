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

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import play.api.libs.json.Json
import uk.gov.hmrc.http.InternalServerException

class CustomerIdentificationBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  "buildCustomerIdentificationBlock" when {
    "all required sections are present in the VAT scheme" when {
      "a trading name is present" must {
        "return the correct JSON" in new CustomerIdentificationBlockBuilder {
          val res = buildCustomerIdentificationBlock(testFullVatScheme)

          res mustBe Json.obj(
            "tradersPartyType" -> "50",
            "identifiers" -> Json.obj(
              "companyRegistrationNumber" -> testCrn,
              "ctUTR" -> testUtr
            ),
            "shortOrgName" -> testCompanyName,
            "dateOfBirth" -> testDateOfBirth,
            "tradingName" -> testTradingName
          )
        }
      }
      "a trading name is not present" must {
        "return the correct JSON" in new CustomerIdentificationBlockBuilder {
          val tradingDetails = validFullTradingDetails.copy(tradingName = None)
          val res = buildCustomerIdentificationBlock(testFullVatScheme.copy(tradingDetails = Some(tradingDetails)))

          res mustBe Json.obj(
            "tradersPartyType" -> "50",
            "identifiers" -> Json.obj(
              "companyRegistrationNumber" -> testCrn,
              "ctUTR" -> testUtr
            ),
            "shortOrgName" -> testCompanyName,
            "dateOfBirth" -> testDateOfBirth
          )
        }
      }
    }
    "the ApplicantDetails section is missing from the VAT scheme" must {
      "throw an InternalServerException" in new CustomerIdentificationBlockBuilder {
        intercept[InternalServerException] {
          buildCustomerIdentificationBlock(testFullVatScheme.copy(applicantDetails = None))
        }
      }
    }
    "the TradingDetails section is missing from the VAT scheme" must {
      "throw an InternalServerException" in new CustomerIdentificationBlockBuilder {
        intercept[InternalServerException] {
          buildCustomerIdentificationBlock(testFullVatScheme.copy(applicantDetails = None))
        }
      }
    }
  }

}
