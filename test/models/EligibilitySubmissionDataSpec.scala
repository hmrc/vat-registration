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

package models

import models.api.{EligibilitySubmissionData, Threshold}
import models.submission.UkCompany
import play.api.libs.json.{JsArray, JsObject, JsSuccess, Json}
import utils.EligibilityDataJsonUtils

import java.time.LocalDate

class EligibilitySubmissionDataSpec extends JsonFormatValidation {

  val testName = "testName"
  val testVrn = "testVrn"

  "eligibilityReads" when {
    "return EligibilitySubmissionData from a valid eligibility json" in {
      val questions = Seq(
        Json.obj("questionId" -> "voluntaryRegistration", "question" -> "testQuestion", "answer" -> "testAnswer",
          "answerValue" -> false),
        Json.obj("questionId" -> "thresholdPreviousThirtyDays-optionalData", "question" -> "testQuestion", "answer" -> "testAnswer",
          "answerValue" -> LocalDate.now().toString),
        Json.obj("questionId" -> "thresholdInTwelveMonths-optionalData", "question" -> "testQuestion", "answer" -> "testQuestion",
          "answerValue" -> LocalDate.now().toString),
        Json.obj("questionId" -> "thresholdNextThirtyDays-optionalData", "question" -> "testQuestion", "answer" -> "testQuestion",
          "answerValue" -> LocalDate.now().toString),
        Json.obj("questionId" -> "registeringBusiness", "question" -> "testQuestion", "answer" -> "testQuestion",
          "answerValue" -> "own"),
        Json.obj("questionId" -> "businessEntity", "question" -> "testQuestion", "answer" -> "testQuestion",
          "answerValue" -> "50"),
        Json.obj("questionId" -> "dateOfBusinessTransfer", "question" -> "testQuestion", "answer" -> "testQuestion",
          "answerValue" -> LocalDate.now()),
        Json.obj("questionId" -> "previousBusinessName", "question" -> "testQuestion", "answer" -> "testQuestion",
          "answerValue" -> testName),
        Json.obj("questionId" -> "vatNumber", "question" -> "testQuestion", "answer" -> "testQuestion",
          "answerValue" -> testVrn),
        Json.obj("questionId" -> "keepOldVrn", "question" -> "testQuestion", "answer" -> "testQuestion",
          "answerValue" -> true),
        Json.obj("questionId" -> "termsAndConditions", "question" -> "testQuestion", "answer" -> "testQuestion",
          "answerValue" -> true),
        Json.obj("questionId" -> "fixedEstablishment", "question" -> "testQuestion", "answer" -> "testAnswer",
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
        appliedForException = None,
        partyType = UkCompany,
        registrationReason = ForwardLook,
        togcCole = Some(TogcCole(
          dateOfTransfer = LocalDate.now(),
          previousBusinessName = testName,
          vatRegistrationNumber = testVrn,
          wantToKeepVatNumber = true,
          agreedWithTermsForKeepingVat = Some(true)
        )),
        isTransactor = false,
        calculatedDate = Some(LocalDate.now()),
        fixedEstablishmentInManOrUk = true
      ))

      result mustBe expected
    }
    "registration reason is TogcCole" must {
      "set calculatedDate as dateOfTransfer in EligibilitySubmissionData" in {
        val togcColeBlock = Seq(
          Json.obj("questionId" -> "registeringBusiness", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> "own"),
          Json.obj("questionId" -> "businessEntity", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> "50"),
          Json.obj("questionId" -> "registrationReason", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> EligibilitySubmissionData.takingOverBusiness),
          Json.obj("questionId" -> "dateOfBusinessTransfer", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> LocalDate.now()),
          Json.obj("questionId" -> "previousBusinessName", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> testName),
          Json.obj("questionId" -> "vatNumber", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> testVrn),
          Json.obj("questionId" -> "keepOldVrn", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> true),
          Json.obj("questionId" -> "termsAndConditions", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> true),
          Json.obj("questionId" -> "fixedEstablishment", "question" -> "testQuestion", "answer" -> "testAnswer",
            "answerValue" -> true)
        )
        val section: JsObject = Json.obj("title" -> "testTitle", "data" -> JsArray(togcColeBlock))
        val testEligibilityJson: JsObject = Json.obj("sections" -> section)

        val result = Json.fromJson(testEligibilityJson)(
          EligibilityDataJsonUtils.mongoReads[EligibilitySubmissionData](EligibilitySubmissionData.eligibilityReads)
        )

        val expected = JsSuccess(EligibilitySubmissionData(
          threshold = Threshold(
            mandatoryRegistration = false
          ),
          appliedForException = None,
          partyType = UkCompany,
          registrationReason = TransferOfAGoingConcern,
          togcCole = Some(TogcCole(
            dateOfTransfer = LocalDate.now(),
            previousBusinessName = testName,
            vatRegistrationNumber = testVrn,
            wantToKeepVatNumber = true,
            agreedWithTermsForKeepingVat = Some(true)
          )),
          isTransactor = false,
          calculatedDate = Some(LocalDate.now()),
          fixedEstablishmentInManOrUk = true
        ))

        result mustBe expected
      }
    }
    "registration reason is sellingGoodsAndServices with threshold overseas" must {
      "set calculatedDate as thresholdOverseas in EligibilitySubmissionData" in {
        val thresholdOverseasBlock = Seq(
          Json.obj("questionId" -> "thresholdTaxableSupplies", "question" -> "testQuestion", "answer" -> "testAnswer",
            "answerValue" -> LocalDate.now().toString),
          Json.obj("questionId" -> "registeringBusiness", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> "own"),
          Json.obj("questionId" -> "businessEntity", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> "50"),
          Json.obj("questionId" -> "fixedEstablishment", "question" -> "testQuestion", "answer" -> "testAnswer",
            "answerValue" -> false)
        )
        val section: JsObject = Json.obj("title" -> "testTitle", "data" -> JsArray(thresholdOverseasBlock))
        val testEligibilityJson: JsObject = Json.obj("sections" -> section)

        val result = Json.fromJson(testEligibilityJson)(
          EligibilityDataJsonUtils.mongoReads[EligibilitySubmissionData](EligibilitySubmissionData.eligibilityReads)
        )

        val expected = JsSuccess(EligibilitySubmissionData(
          threshold = Threshold(
            mandatoryRegistration = true,
            thresholdOverseas = Some(LocalDate.now)
          ),
          appliedForException = None,
          partyType = UkCompany,
          registrationReason = NonUk,
          isTransactor = false,
          calculatedDate = Some(LocalDate.now()),
          fixedEstablishmentInManOrUk = false
        ))

        result mustBe expected
      }
    }
    "registration reason is sellingGoodsAndServices with thresholdPreviousThirtyDays" must {
      "set calculatedDate as thresholdPreviousThirtyDays in EligibilitySubmissionData" in {
        val thresholdPrevThirtyDaysBlock = Seq(
          Json.obj("questionId" -> "thresholdPreviousThirtyDays-optionalData", "question" -> "testQuestion", "answer" -> "testAnswer",
            "answerValue" -> LocalDate.now().toString),
          Json.obj("questionId" -> "registeringBusiness", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> "own"),
          Json.obj("questionId" -> "businessEntity", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> "50"),
          Json.obj("questionId" -> "fixedEstablishment", "question" -> "testQuestion", "answer" -> "testAnswer",
            "answerValue" -> true)
        )
        val section: JsObject = Json.obj("title" -> "testTitle", "data" -> JsArray(thresholdPrevThirtyDaysBlock))
        val testEligibilityJson: JsObject = Json.obj("sections" -> section)

        val result = Json.fromJson(testEligibilityJson)(
          EligibilityDataJsonUtils.mongoReads[EligibilitySubmissionData](EligibilitySubmissionData.eligibilityReads)
        )

        val expected = JsSuccess(EligibilitySubmissionData(
          threshold = Threshold(
            mandatoryRegistration = true,
            thresholdPreviousThirtyDays = Some(LocalDate.now)
          ),
          appliedForException = None,
          partyType = UkCompany,
          registrationReason = ForwardLook,
          isTransactor = false,
          calculatedDate = Some(LocalDate.now()),
          fixedEstablishmentInManOrUk = true
        ))

        result mustBe expected
      }
    }
    "registration reason is sellingGoodsAndServices with thresholdNextThirtyDays" must {
      "set calculatedDate as thresholdNextThirtyDays in EligibilitySubmissionData" in {
        val thresholdNextThirtyDaysBlock = Seq(
          Json.obj("questionId" -> "thresholdNextThirtyDays-optionalData", "question" -> "testQuestion", "answer" -> "testAnswer",
            "answerValue" -> LocalDate.now().toString),
          Json.obj("questionId" -> "registeringBusiness", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> "own"),
          Json.obj("questionId" -> "businessEntity", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> "50"),
          Json.obj("questionId" -> "fixedEstablishment", "question" -> "testQuestion", "answer" -> "testAnswer",
            "answerValue" -> true)
        )
        val section: JsObject = Json.obj("title" -> "testTitle", "data" -> JsArray(thresholdNextThirtyDaysBlock))
        val testEligibilityJson: JsObject = Json.obj("sections" -> section)

        val result = Json.fromJson(testEligibilityJson)(
          EligibilityDataJsonUtils.mongoReads[EligibilitySubmissionData](EligibilitySubmissionData.eligibilityReads)
        )

        val expected = JsSuccess(EligibilitySubmissionData(
          threshold = Threshold(
            mandatoryRegistration = true,
            thresholdNextThirtyDays = Some(LocalDate.now)
          ),
          appliedForException = None,
          partyType = UkCompany,
          registrationReason = ForwardLook,
          isTransactor = false,
          calculatedDate = Some(LocalDate.now()),
          fixedEstablishmentInManOrUk = true
        ))

        result mustBe expected
      }
    }
    "registration reason is sellingGoodsAndServices with thresholdNextTwelveMonths" must {
      "set calculatedDate as thresholdNextTwelveMonths in EligibilitySubmissionData" in {
        val thresholdNextTwelveMonthsBlock = Seq(
          Json.obj("questionId" -> "thresholdInTwelveMonths-optionalData", "question" -> "testQuestion", "answer" -> "testAnswer",
            "answerValue" -> LocalDate.now().toString),
          Json.obj("questionId" -> "registeringBusiness", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> "own"),
          Json.obj("questionId" -> "businessEntity", "question" -> "testQuestion", "answer" -> "testQuestion",
            "answerValue" -> "50"),
          Json.obj("questionId" -> "fixedEstablishment", "question" -> "testQuestion", "answer" -> "testAnswer",
            "answerValue" -> true)
        )
        val section: JsObject = Json.obj("title" -> "testTitle", "data" -> JsArray(thresholdNextTwelveMonthsBlock))
        val testEligibilityJson: JsObject = Json.obj("sections" -> section)

        val result = Json.fromJson(testEligibilityJson)(
          EligibilityDataJsonUtils.mongoReads[EligibilitySubmissionData](EligibilitySubmissionData.eligibilityReads)
        )

        val expected = JsSuccess(EligibilitySubmissionData(
          threshold = Threshold(
            mandatoryRegistration = true,
            thresholdInTwelveMonths = Some(LocalDate.now)
          ),
          appliedForException = None,
          partyType = UkCompany,
          registrationReason = BackwardLook,
          isTransactor = false,
          calculatedDate = Some(LocalDate.now.plusMonths(2).withDayOfMonth(1)),
          fixedEstablishmentInManOrUk = true
        ))

        result mustBe expected
      }
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
        "appliedForException" -> false,
        "partyType" -> "50",
        "registrationReason" -> Json.toJson[RegistrationReason](ForwardLook),
        "calculatedDate" -> LocalDate.now(),
        "fixedEstablishmentInManOrUk" -> true
      )

      val expected = JsSuccess(EligibilitySubmissionData(
        threshold = Threshold(
          mandatoryRegistration = true,
          thresholdInTwelveMonths = Some(LocalDate.now()),
          thresholdNextThirtyDays = Some(LocalDate.now()),
          thresholdPreviousThirtyDays = Some(LocalDate.now())
        ),
        appliedForException = Some(false),
        partyType = UkCompany,
        registrationReason = ForwardLook,
        isTransactor = false,
        calculatedDate = Some(LocalDate.now()),
        fixedEstablishmentInManOrUk = true
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
        appliedForException = Some(false),
        partyType = UkCompany,
        registrationReason = ForwardLook,
        isTransactor = false,
        calculatedDate = Some(LocalDate.now()),
        fixedEstablishmentInManOrUk = true
      )

      val expected = Json.obj(
        "threshold" -> Json.obj(
          "mandatoryRegistration" -> true,
          "thresholdInTwelveMonths" -> LocalDate.now().toString,
          "thresholdNextThirtyDays" -> LocalDate.now().toString,
          "thresholdPreviousThirtyDays" -> LocalDate.now().toString
        ),
        "appliedForException" -> false,
        "partyType" -> "50",
        "registrationReason" -> Json.toJson[RegistrationReason](ForwardLook),
        "isTransactor" -> false,
        "calculatedDate" -> LocalDate.now(),
        "fixedEstablishmentInManOrUk" -> true
      )

      EligibilitySubmissionData.format.writes(model) mustBe expected
    }
  }
}
