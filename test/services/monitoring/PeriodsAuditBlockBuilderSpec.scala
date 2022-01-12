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
import models.api.returns._
import play.api.libs.json.Json
import uk.gov.hmrc.http.InternalServerException

class PeriodsAuditBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  object TestBuilder extends PeriodsAuditBlockBuilder

  "the periods block builder" should {
    "write the correct json for the monthly stagger" in {
      val testScheme = testVatScheme.copy(returns = Some(testReturns).map(_.copy(returnsFrequency = Monthly, staggerStart = MonthlyStagger))
      )

      val res = TestBuilder.buildPeriodsBlock(testScheme)

      res mustBe Json.obj(
        "customerPreferredPeriodicity" -> "MM"
      )
    }
    "write the correct json for stagger 1" in {
      val testScheme = testVatScheme.copy(returns = Some(testReturns).map(_.copy(staggerStart = JanuaryStagger)))

      val res = TestBuilder.buildPeriodsBlock(testScheme)

      res mustBe Json.obj(
        "customerPreferredPeriodicity" -> "MA"
      )
    }
    "write the correct json for stagger 2" in {
      val testScheme = testVatScheme.copy(returns = Some(testReturns).map(_.copy(staggerStart = FebruaryStagger)))

      val res = TestBuilder.buildPeriodsBlock(testScheme)

      res mustBe Json.obj(
        "customerPreferredPeriodicity" -> "MB"
      )
    }
    "write the correct json for stagger 3" in {
      val testScheme = testVatScheme.copy(returns = Some(testReturns).map(_.copy(staggerStart = MarchStagger)))

      val res = TestBuilder.buildPeriodsBlock(testScheme)

      res mustBe Json.obj(
        "customerPreferredPeriodicity" -> "MC"
      )
    }
    "throw an exception if the returns section is missing" in {
      val testScheme = testVatScheme.copy(returns = None)

      intercept[InternalServerException] {
        TestBuilder.buildPeriodsBlock(testScheme)
      }
    }
  }

}
