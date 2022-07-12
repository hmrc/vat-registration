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

package services.monitoring

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import play.api.libs.json.{JsObject, Json}

class AnnualAccountingAuditBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  val annualAccountingAuditBlockJson: JsObject = Json.parse(
    s"""
      {"submissionType":"1",
       |"customerRequest":{
       |   "paymentMethod":"01",
       |   "annualStagger":"YA",
       |   "paymentFrequency":"M",
       |   "estimatedTurnover":$testTurnover,
       |   "reqStartDate":"2020-10-07"
       |   }
       |}
       |""".stripMargin).as[JsObject]

  object TestBuilder extends AnnualAccountingAuditBlockBuilder

  "buildAnnualAccountingBlock" should {
    "return the correct json" when {
      "the applicant wants to join AAS and all data is provided" in {
        val testScheme = testVatScheme.copy(vatApplication = Some(validAASApplicationDeatils), eligibilitySubmissionData = Some(testEligibilitySubmissionData))

        val res = TestBuilder.buildAnnualAccountingAuditBlock(testScheme)

        res mustBe Some(annualAccountingAuditBlockJson)
      }
    }

    "return None" when {
      "the returns block is quarterly" in {
        val testScheme = testVatScheme.copy(returns = Some(testReturns))

        val res = TestBuilder.buildAnnualAccountingAuditBlock(testScheme)

        res mustBe None
      }

      "the returns block is missing" in {
        val testScheme = testVatScheme.copy(returns = None)

        val res = TestBuilder.buildAnnualAccountingAuditBlock(testScheme)

        res mustBe None
      }
    }
  }
}
