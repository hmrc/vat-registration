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
import mocks.MockVatSchemeRepository
import models.api.vatapplication._
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.InternalServerException

class PeriodsBlockBuilderSpec extends VatRegSpec with MockVatSchemeRepository with VatRegistrationFixture {

  object TestBuilder extends PeriodsBlockBuilder

  implicit val request: Request[_] = FakeRequest()

  "the periods block builder" should {
    "write the correct json for the monthly stagger" in {
      val monthlyReturns =
        testVatApplicationDetails.copy(returnsFrequency = Some(Monthly), staggerStart = Some(MonthlyStagger))
      val vatScheme      = testVatScheme.copy(vatApplication = Some(monthlyReturns))

      val res = TestBuilder.buildPeriodsBlock(vatScheme)

      res mustBe Json.obj(
        "customerPreferredPeriodicity" -> "MM"
      )
    }
    "write the correct json for stagger 1" in {
      val stagger1ApplicationDetails = testVatApplicationDetails.copy(staggerStart = Some(JanuaryStagger))
      val vatScheme                  = testVatScheme.copy(vatApplication = Some(stagger1ApplicationDetails))

      val res = TestBuilder.buildPeriodsBlock(vatScheme)

      res mustBe Json.obj(
        "customerPreferredPeriodicity" -> "MA"
      )
    }
    "write the correct json for stagger 2" in {
      val stagger2ApplicationDetails = testVatApplicationDetails.copy(staggerStart = Some(FebruaryStagger))
      val vatScheme                  = testVatScheme.copy(vatApplication = Some(stagger2ApplicationDetails))

      val res = TestBuilder.buildPeriodsBlock(vatScheme)

      res mustBe Json.obj(
        "customerPreferredPeriodicity" -> "MB"
      )
    }
    "write the correct json for stagger 3" in {
      val stagger3ApplicationDetails = testVatApplicationDetails.copy(staggerStart = Some(MarchStagger))
      val vatScheme                  = testVatScheme.copy(vatApplication = Some(stagger3ApplicationDetails))

      val res = TestBuilder.buildPeriodsBlock(vatScheme)

      res mustBe Json.obj(
        "customerPreferredPeriodicity" -> "MC"
      )
    }
    "throw an exception if the returns section is missing" in {
      intercept[InternalServerException] {
        TestBuilder.buildPeriodsBlock(testVatScheme)
      }
    }
  }

}
