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

package models.monitoring

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec

class SubmissionFailureErrorsAuditModelSpec extends VatRegSpec with VatRegistrationFixture {

  val testCorrelationId = "testCorrelationId"
  val regIdKey = "registrationId"
  val correlationIdKey = "correlationId"
  val suppressedKey = "suppressedErrors"
  val unknownKey = "unknownErrors"

  val testField1 = "/test1"
  val testField2 = "/test2"
  val testField3 = "/test3"

  val testErrorMap = Map(
     suppressedKey -> List(testField1),
     unknownKey -> List(testField2, testField3)
  )

  "the Submission Failure audit model" must {
    "create the correct JSON" in {
      val model = SubmissionFailureErrorsAuditModel(testRegId, testCorrelationId, testErrorMap)

      (model.detail \ regIdKey).asOpt[String] mustBe Some(testRegId)
      (model.detail \ correlationIdKey).asOpt[String] mustBe Some(testCorrelationId)
      (model.detail \ suppressedKey).asOpt[List[String]] mustBe Some(List(testField1))
      (model.detail \ unknownKey).asOpt[List[String]] mustBe Some(List(testField2, testField3))
    }
  }

}
