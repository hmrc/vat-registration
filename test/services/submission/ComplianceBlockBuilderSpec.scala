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
import models.api.ComplianceLabour
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest
import uk.gov.hmrc.http.InternalServerException

class ComplianceBlockBuilderSpec extends VatRegSpec with MockVatSchemeRepository with VatRegistrationFixture {

  object TestBuilder extends ComplianceBlockBuilder

  implicit val request: Request[_] = FakeRequest()

  val emptyLabourCompliance = ComplianceLabour(
    numOfWorkersSupplied = None,
    intermediaryArrangement = None,
    supplyWorkers = Some(true)
  )

  "The compliance block builder" must {
    "build the correct json when only the supplyWorkers flag is set" in {
      val vatScheme =
        testVatScheme.copy(business = Some(testBusiness.copy(labourCompliance = Some(emptyLabourCompliance))))

      val res = TestBuilder.buildComplianceBlock(vatScheme)

      res mustBe Some(
        Json.obj(
          "supplyWorkers" -> true
        )
      )
    }
    "build the correct json when the both the optional answers are provided" in {
      val fullLabourCompliance = emptyLabourCompliance.copy(
        numOfWorkersSupplied = Some(1),
        intermediaryArrangement = Some(true)
      )
      val vatScheme            =
        testVatScheme.copy(business = Some(testBusiness.copy(labourCompliance = Some(fullLabourCompliance))))

      val res = TestBuilder.buildComplianceBlock(vatScheme)

      res mustBe Some(
        Json.obj(
          "numOfWorkersSupplied"    -> 1,
          "intermediaryArrangement" -> true,
          "supplyWorkers"           -> true
        )
      )
    }
    "return None when the labourCompliance section is not defined" in {
      val noCompliance = testBusiness.copy(labourCompliance = None)
      val vatScheme    = testVatScheme.copy(business = Some(noCompliance))

      val res = TestBuilder.buildComplianceBlock(vatScheme)

      res mustBe None
    }
    "throw an exception when the business section is not defined" in {
      intercept[InternalServerException] {
        TestBuilder.buildComplianceBlock(testVatScheme)
      }
    }
  }

}
