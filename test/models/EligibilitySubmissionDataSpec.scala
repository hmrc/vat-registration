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
import play.api.libs.json.{JsObject, JsSuccess, Json}

import java.time.LocalDate

class EligibilitySubmissionDataSpec extends JsonFormatValidation {

  val testName = "testName"
  val testVrn = "testVrn"

  "eligibilityReads" when {
    "return EligibilitySubmissionData from a valid eligibility json" in {
      val testEligibilityJson = Json.obj(
        "fixedEstablishment" -> true,
        "businessEntity" -> "50",
        "agriculturalFlatRateScheme" -> false,
        "internationalActivities" -> false,
        "registeringBusiness" -> "own",
        "registrationReason" -> "selling-goods-and-services",
        "thresholdPreviousThirtyDays" -> Json.obj(
          "value" -> true,
          "optionalData" -> LocalDate.now()
        ),
        "thresholdInTwelveMonths" -> Json.obj(
          "value" -> true,
          "optionalData" -> LocalDate.now()
        ),
        "thresholdNextThirtyDays" -> Json.obj(
          "value" -> true,
          "optionalData" -> LocalDate.now()
        ),
        "vatRegistrationException" -> false
      )

      val result = Json.fromJson(testEligibilityJson)(EligibilitySubmissionData.eligibilityReads)

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

      result mustBe expected
    }
    "registration reason is TogcCole" must {
      "set calculatedDate as dateOfTransfer in EligibilitySubmissionData" in {
        val testEligibilityJson: JsObject = Json.obj(
          "fixedEstablishment" -> true,
          "businessEntity" -> "50",
          "agriculturalFlatRateScheme" -> false,
          "internationalActivities" -> false,
          "registeringBusiness" -> "own",
          "registrationReason" -> EligibilitySubmissionData.takingOverBusiness,
          "dateOfBusinessTransfer" -> Json.obj(
            "date" -> LocalDate.now()
          ),
          "previousBusinessName" -> testName,
          "vatNumber" -> testVrn,
          "keepOldVrn" -> true,
          "termsAndConditions" -> true,
          "vatRegistrationException" -> false
        )

        val result = Json.fromJson(testEligibilityJson)(EligibilitySubmissionData.eligibilityReads)

        val expected = JsSuccess(EligibilitySubmissionData(
          threshold = Threshold(
            mandatoryRegistration = false
          ),
          appliedForException = Some(false),
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
        val testEligibilityJson: JsObject = Json.obj(
          "fixedEstablishment" -> false,
          "businessEntity" -> "50",
          "agriculturalFlatRateScheme" -> false,
          "internationalActivities" -> false,
          "registeringBusiness" -> "own",
          "registrationReason" -> EligibilitySubmissionData.sellingGoodsAndServices,
          "thresholdTaxableSupplies" -> Json.obj(
            "date" -> LocalDate.now()
          ),
          "vatRegistrationException" -> false
        )

        val result = Json.fromJson(testEligibilityJson)(EligibilitySubmissionData.eligibilityReads)

        val expected = JsSuccess(EligibilitySubmissionData(
          threshold = Threshold(
            mandatoryRegistration = true,
            thresholdOverseas = Some(LocalDate.now)
          ),
          appliedForException = Some(false),
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
        val testEligibilityJson: JsObject = Json.obj(
          "fixedEstablishment" -> true,
          "businessEntity" -> "50",
          "agriculturalFlatRateScheme" -> false,
          "internationalActivities" -> false,
          "registeringBusiness" -> "own",
          "registrationReason" -> EligibilitySubmissionData.sellingGoodsAndServices,
          "thresholdPreviousThirtyDays" -> Json.obj(
            "value" -> true,
            "optionalData" -> LocalDate.now()
          ),
          "vatRegistrationException" -> false
        )

        val result = Json.fromJson(testEligibilityJson)(EligibilitySubmissionData.eligibilityReads)

        val expected = JsSuccess(EligibilitySubmissionData(
          threshold = Threshold(
            mandatoryRegistration = true,
            thresholdPreviousThirtyDays = Some(LocalDate.now)
          ),
          appliedForException = Some(false),
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
        val testEligibilityJson: JsObject = Json.obj(
          "fixedEstablishment" -> true,
          "businessEntity" -> "50",
          "agriculturalFlatRateScheme" -> false,
          "internationalActivities" -> false,
          "registeringBusiness" -> "own",
          "registrationReason" -> EligibilitySubmissionData.sellingGoodsAndServices,
          "thresholdNextThirtyDays" -> Json.obj(
            "value" -> true,
            "optionalData" -> LocalDate.now()
          ),
          "vatRegistrationException" -> false
        )

        val result = Json.fromJson(testEligibilityJson)(EligibilitySubmissionData.eligibilityReads)

        val expected = JsSuccess(EligibilitySubmissionData(
          threshold = Threshold(
            mandatoryRegistration = true,
            thresholdNextThirtyDays = Some(LocalDate.now)
          ),
          appliedForException = Some(false),
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
      "set calculatedDate as thresholdInTwelveMonths in EligibilitySubmissionData" in {
        val testEligibilityJson: JsObject = Json.obj(
          "fixedEstablishment" -> true,
          "businessEntity" -> "50",
          "agriculturalFlatRateScheme" -> false,
          "internationalActivities" -> false,
          "registeringBusiness" -> "own",
          "registrationReason" -> EligibilitySubmissionData.sellingGoodsAndServices,
          "thresholdInTwelveMonths" -> Json.obj(
            "value" -> true,
            "optionalData" -> LocalDate.now()
          ),
          "vatRegistrationException" -> false
        )

        val result = Json.fromJson(testEligibilityJson)(EligibilitySubmissionData.eligibilityReads)

        val expected = JsSuccess(EligibilitySubmissionData(
          threshold = Threshold(
            mandatoryRegistration = true,
            thresholdInTwelveMonths = Some(LocalDate.now)
          ),
          appliedForException = Some(false),
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
