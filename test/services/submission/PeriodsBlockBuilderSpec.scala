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

package services.submission

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.MockRegistrationRepository
import models.api.returns._
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException

class PeriodsBlockBuilderSpec extends VatRegSpec with MockRegistrationRepository with VatRegistrationFixture {

  object TestBuilder extends PeriodsBlockBuilder(mockRegistrationRepository)

  val emptyReturns: Returns = Returns(
    zeroRatedSupplies = None,
    reclaimVatOnMostReturns = false,
    returnsFrequency = Quarterly,
    staggerStart = JanuaryStagger,
    startDate = Some(testDate),
    annualAccountingDetails = None
  )

  "the periods block builder" should {
    "write the correct json for the monthly stagger" in {
      val monthlyReturns = testReturns.copy(returnsFrequency = Monthly, staggerStart = MonthlyStagger)
      mockGetVatScheme(testRegId)(Some(testVatScheme.copy(returns = Some(monthlyReturns))))

      val res = await(TestBuilder.buildPeriodsBlock(testRegId))

      res mustBe Json.obj(
        "customerPreferredPeriodicity" -> "MM"
      )
    }
    "write the correct json for stagger 1" in {
      val stagger1Returns = testReturns.copy(staggerStart = JanuaryStagger)
      mockGetVatScheme(testRegId)(Some(testVatScheme.copy(returns = Some(stagger1Returns))))

      val res = await(TestBuilder.buildPeriodsBlock(testRegId))

      res mustBe Json.obj(
        "customerPreferredPeriodicity" -> "MA"
      )
    }
    "write the correct json for stagger 2" in {
      val stagger2Returns = testReturns.copy(staggerStart = FebruaryStagger)
      mockGetVatScheme(testRegId)(Some(testVatScheme.copy(returns = Some(stagger2Returns))))

      val res = await(TestBuilder.buildPeriodsBlock(testRegId))

      res mustBe Json.obj(
        "customerPreferredPeriodicity" -> "MB"
      )
    }
    "write the correct json for stagger 3" in {
      val stagger3Returns = testReturns.copy(staggerStart = MarchStagger)
      mockGetVatScheme(testRegId)(Some(testVatScheme.copy(returns = Some(stagger3Returns))))

      val res = await(TestBuilder.buildPeriodsBlock(testRegId))

      res mustBe Json.obj(
        "customerPreferredPeriodicity" -> "MC"
      )
    }
    "throw an exception if the returns section is missing" in {
      mockGetVatScheme(testRegId)(Some(testVatScheme.copy(returns = None)))

      intercept[InternalServerException] {
        await(TestBuilder.buildPeriodsBlock(testRegId))
      }
    }
  }

}
