/*
 * Copyright 2023 HM Revenue & Customs
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

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import play.api.libs.json.{JsObject, Json}

class AnnualAccountingBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service: AnnualAccountingBlockBuilder = new AnnualAccountingBlockBuilder
  }

  val annualAccountingBlockJson: JsObject = Json.parse(
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

  "buildAnnualAccountingBlock" should {
    "return the correct json" when {
      "the applicant wants to join AAS and all data is provided" in new Setup {
        val vatScheme = testVatScheme.copy(
          vatApplication = Some(validAASApplicationDeatils),
          eligibilitySubmissionData = Some(testEligibilitySubmissionData)
        )

        val result: Option[JsObject] = service.buildAnnualAccountingBlock(vatScheme)
        result mustBe Some(annualAccountingBlockJson)
      }
    }
  }
}
