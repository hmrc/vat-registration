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

package models

import models.api.{EligibilitySubmissionData, MTDfB, Threshold, TurnoverEstimates}
import play.api.libs.json.{JsArray, JsObject, JsSuccess, Json}
import utils.EligibilityDataJsonUtils

import java.time.LocalDate

class EligibilitySubmissionDataSpec extends JsonFormatValidation {

  "eligibilityReads" must {
    "return EligibilitySubmissionData from a valid eligibility json" in {
      val questions = Seq(
        Json.obj("questionId" -> "voluntaryInformation", "question" -> "testQuestion", "answer" -> "testAnswer",
          "answerValue" -> true),
        Json.obj("questionId" -> "voluntaryRegistration", "question" -> "testQuestion", "answer" -> "testAnswer",
          "answerValue" -> false),
        Json.obj("questionId" -> "thresholdPreviousThirtyDays-optionalData", "question" -> "testQuestion", "answer" -> "testAnswer",
          "answerValue" -> LocalDate.now().toString),
        Json.obj("questionId" -> "thresholdInTwelveMonths-optionalData", "question" -> "testQuestion", "answer" -> "testQuestion",
          "answerValue" -> LocalDate.now().toString),
        Json.obj("questionId" -> "thresholdNextThirtyDays-optionalData", "question" -> "testQuestion", "answer" -> "testQuestion",
          "answerValue" -> LocalDate.now().toString),
        Json.obj("questionId" -> "turnoverEstimate-value", "question" -> "testQuestion", "answer" -> "testQuestion",
          "answerValue" -> 123456),
        Json.obj("questionId" -> "customerStatus-value", "question" -> "testQuestion", "answer" -> "testQuestion",
          "answerValue" -> "2"),
        Json.obj("questionId" -> "registeringBusiness", "question" -> "testQuestion", "answer" -> "testQuestion",
          "answerValue" -> true)
      )
      val section: JsObject = Json.obj("title" -> "testTitle", "data" -> JsArray(questions))
      val testEligibilityJson: JsObject = Json.obj("sections" -> section)

      val result = Json.fromJson(testEligibilityJson)(
        EligibilityDataJsonUtils.mongoReads[EligibilitySubmissionData](EligibilitySubmissionData.eligibilityReads)
      )

      val expected = JsSuccess(EligibilitySubmissionData(
        threshold = Threshold(
          mandatoryRegistration = true,
          thresholdInTwelveMonths = Some(LocalDate.now()),
          thresholdNextThirtyDays = Some(LocalDate.now()),
          thresholdPreviousThirtyDays = Some(LocalDate.now())
        ),
        exceptionOrExemption = "0",
        estimates = TurnoverEstimates(123456),
        customerStatus = MTDfB
      ))

      result mustBe expected
    }
  }

  "reads" must {
    "return EligibilitySubmissionData from a valid json" in {
      val json = Json.obj(
        "threshold" -> Json.obj(
          "mandatoryRegistration" -> true,
          "thresholdInTwelveMonths" -> LocalDate.now().toString,
          "thresholdNextThirtyDays" -> LocalDate.now().toString,
          "thresholdPreviousThirtyDays" -> LocalDate.now().toString
        ),
        "exceptionOrExemption" -> "0",
        "estimates" -> Json.obj(
          "turnoverEstimate" -> 123456
        ),
        "customerStatus" -> "2"
      )

      val expected = JsSuccess(EligibilitySubmissionData(
        threshold = Threshold(
          mandatoryRegistration = true,
          thresholdInTwelveMonths = Some(LocalDate.now()),
          thresholdNextThirtyDays = Some(LocalDate.now()),
          thresholdPreviousThirtyDays = Some(LocalDate.now())
        ),
        exceptionOrExemption = "0",
        estimates = TurnoverEstimates(123456),
        customerStatus = MTDfB
      ))

      EligibilitySubmissionData.format.reads(json) mustBe expected
    }
  }

  "writes" must {
    "return a json from EligibilitySubmissionData" in {
      val model = EligibilitySubmissionData(
        threshold = Threshold(
          mandatoryRegistration = true,
          thresholdInTwelveMonths = Some(LocalDate.now()),
          thresholdNextThirtyDays = Some(LocalDate.now()),
          thresholdPreviousThirtyDays = Some(LocalDate.now())
        ),
        exceptionOrExemption = "0",
        estimates = TurnoverEstimates(123456),
        customerStatus = MTDfB
      )

      val expected = Json.obj(
        "threshold" -> Json.obj(
          "mandatoryRegistration" -> true,
          "thresholdInTwelveMonths" -> LocalDate.now().toString,
          "thresholdNextThirtyDays" -> LocalDate.now().toString,
          "thresholdPreviousThirtyDays" -> LocalDate.now().toString
        ),
        "exceptionOrExemption" -> "0",
        "estimates" -> Json.obj(
          "turnoverEstimate" -> 123456
        ),
        "customerStatus" -> "2"
      )

      EligibilitySubmissionData.format.writes(model) mustBe expected
    }
  }
}
