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
import uk.gov.hmrc.http.InternalServerException

class VatSchemeSpec extends VatRegSpec with VatRegistrationFixture {

  "exceptionOrExemption" must {
    "return exceptionKey" when {
      "eligibilitySubmissionData has appliedForException flag true" in {
        val result = VatScheme.exceptionOrExemption(testEligibilitySubmissionData.copy(appliedForException = Some(true)), testReturns)
        result mustBe VatScheme.exceptionKey
      }
    }
    "return exemptionKey" when {
      "Returns has appliedForExemption flag true" in {
        val result = VatScheme.exceptionOrExemption(testEligibilitySubmissionData, testReturns.copy(appliedForExemption = Some(true)))
        result mustBe VatScheme.exemptionKey
      }
    }
    "return nonExceptionOrExemptionKey" when {
      "both exception and exemption flags are false" in {
        val result = VatScheme.exceptionOrExemption(
          testEligibilitySubmissionData.copy(appliedForException = Some(false)),
          testReturns.copy(appliedForExemption = Some(false)))
        result mustBe VatScheme.nonExceptionOrExemptionKey
      }
      "both exception and exemption flags are not present" in {
        val result = VatScheme.exceptionOrExemption(
          testEligibilitySubmissionData.copy(appliedForException = None),
          testReturns.copy(appliedForExemption = None))
        result mustBe VatScheme.nonExceptionOrExemptionKey
      }
    }
    "returns error" when {
      "both exception and exemption flags are true" in {
        intercept[InternalServerException] {
          VatScheme.exceptionOrExemption(
            testEligibilitySubmissionData.copy(appliedForException = Some(true)),
            testReturns.copy(appliedForExemption = Some(true)))
        }
      }
    }
  }
}
