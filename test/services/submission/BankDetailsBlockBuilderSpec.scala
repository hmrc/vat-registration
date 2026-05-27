/*
 * Copyright 2026 HM Revenue & Customs
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

import enums.VatRegStatus
import featureswitch.core.config.{FeatureSwitching, SubmitBarsInvalidBankDetailsToAPI}
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api._
import models.submission.Individual
import models.{BuildFailure, Voluntary}
import play.api.libs.json.{JsObject, Json}
import services.submission.BankDetailsBlockBuilder.buildBankDetailsBlock
import utils.JsonUtils.jsonObject

import java.time.LocalDate

class BankDetailsBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture with FeatureSwitching {

  private val baseVatScheme = VatScheme("regId", "internalId", LocalDate.of(2020, 2, 2), VatRegStatus.draft)

  trait SwitchOn  { enable(SubmitBarsInvalidBankDetailsToAPI)  }
  trait SwitchOff { disable(SubmitBarsInvalidBankDetailsToAPI) }

  "buildBankDetailsBlock" when {
    "SubmitBarsInvalidBankDetailsToAPI switch it ON" should {
      def bankAccountDetailsWithStatus(status: Option[BankAccountDetailsStatus]) =
        BankAccountDetails("name", "sort-Code", "accountNumber", Some("rollNumber"), status)

      "return a Right with Json containing bank details and no reason or invalid flag" when {
        "user is submitting valid bank details" in new SwitchOn {
          val bankAccountDetails: BankAccount =
            BankAccount(isProvided = true, Some(bankAccountDetailsWithStatus(Some(ValidStatus))), reason = None, None)
          val vatSchemeWithValidBankDetails: VatScheme = baseVatScheme.copy(bankAccount = Some(bankAccountDetails))
          val expectedJson: JsObject = jsonObject(
            "UK" -> jsonObject(
              "accountName"   -> "name",
              "sortCode"      -> "sortCode",
              "accountNumber" -> "accountNumber",
              "rollNumber"    -> "rollNumber"
            )
          )

          buildBankDetailsBlock(vatSchemeWithValidBankDetails) mustBe Right(expectedJson)
        }
      }

      "return a Right with Json containing bank details with an invalid flag, and the 'DontWantToProvide' (ID: 7) reason" when {
        "user fails bars 3 times and has been locked out" in new SwitchOn {
          val invalidBankDetailsWithLockoutReason: BankAccount =
            BankAccount(isProvided = true, Some(bankAccountDetailsWithStatus(Some(InvalidStatus))), reason = Some(DontWantToProvide), None)
          val vatSchemeWithInvalidBankDetailsAndLockoutReason: VatScheme = baseVatScheme.copy(bankAccount = Some(invalidBankDetailsWithLockoutReason))
          val expectedJson: JsObject = jsonObject(
            "UK" -> jsonObject(
              "accountName"              -> "name",
              "sortCode"                 -> "sortCode",
              "accountNumber"            -> "accountNumber",
              "rollNumber"               -> "rollNumber",
              "bankDetailsNotValid"      -> true,
              "reasonBankAccNotProvided" -> "7"
            )
          )

          buildBankDetailsBlock(vatSchemeWithInvalidBankDetailsAndLockoutReason) mustBe Right(expectedJson)
        }
      }

      "return a Right with Json containing bank details with an invalid flag, and their chosen reason" when {
        "user fails bars and then changes their mind to give a reason" in new SwitchOn {
          val invalidBankDetailsWithReasonToNotProvide: BankAccount =
            BankAccount(isProvided = false, Some(bankAccountDetailsWithStatus(Some(InvalidStatus))), reason = Some(BeingSetup), None)
          val vatSchemeWithInvalidBankDetailsAndReason: VatScheme = baseVatScheme.copy(bankAccount = Some(invalidBankDetailsWithReasonToNotProvide))
          val indeterminateBankDetailsWithReasonToNotProvide: BankAccount =
            BankAccount(isProvided = false, Some(bankAccountDetailsWithStatus(Some(IndeterminateStatus))), reason = Some(BeingSetup), None)
          val vatSchemeWithIndeterminateBankDetailsAndReason: VatScheme =
            baseVatScheme.copy(bankAccount = Some(indeterminateBankDetailsWithReasonToNotProvide))
          val expectedJson: JsObject = jsonObject(
            "UK" -> jsonObject(
              "accountName"              -> "name",
              "sortCode"                 -> "sortCode",
              "accountNumber"            -> "accountNumber",
              "rollNumber"               -> "rollNumber",
              "bankDetailsNotValid"      -> true,
              "reasonBankAccNotProvided" -> "1"
            )
          )

          buildBankDetailsBlock(vatSchemeWithInvalidBankDetailsAndReason) mustBe Right(expectedJson)
          buildBankDetailsBlock(vatSchemeWithIndeterminateBankDetailsAndReason) mustBe Right(expectedJson)
        }
      }

      "return a Right with Json containing only their chosen reason" when {
        "user chooses to submit a reason and has not failed any BARS checks" in new SwitchOn {
          val invalidBankDetailsWithLockoutReason: BankAccount =
            BankAccount(isProvided = false, details = None, reason = Some(AccountNotInBusinessName), None)
          val vatSchemeWithInvalidBankDetailsAndLockoutReason: VatScheme = baseVatScheme.copy(bankAccount = Some(invalidBankDetailsWithLockoutReason))
          val expectedJson: JsObject                                     = jsonObject("UK" -> jsonObject("reasonBankAccNotProvided" -> "6"))

          buildBankDetailsBlock(vatSchemeWithInvalidBankDetailsAndLockoutReason) mustBe Right(expectedJson)
        }
      }

      "return a Right with Json containing only the 'OverseasAccount' (ID: 3) reason" when {
        "the partyType is 'Individual'" in new SwitchOn {
          val eligibilitySubmissionData: EligibilitySubmissionData =
            EligibilitySubmissionData(Threshold(true), None, partyType = Individual, Voluntary, None, true, None, true)
          val vatSchemeForIndividual: VatScheme = baseVatScheme.copy(eligibilitySubmissionData = Some(eligibilitySubmissionData))
          val expectedJson: JsObject            = jsonObject("UK" -> jsonObject("reasonBankAccNotProvided" -> "3"))

          buildBankDetailsBlock(vatSchemeForIndividual) mustBe Right(expectedJson)
        }
      }

      "return a Left with a BuildFailure and issue description" when {
        "there is no BankAccount data" in new SwitchOn {
          val vatSchemeWithMissingBankAccountData: VatScheme = baseVatScheme.copy(bankAccount = None)
          val buildFailure: BuildFailure = BuildFailure("[BankDetailsBlockBuilder] Unable to build submission model: No BankAccount model")

          buildBankDetailsBlock(vatSchemeWithMissingBankAccountData) mustBe Left(buildFailure)
        }
        "'isProvided' = true but there are no bank details" in new SwitchOn {
          val missingBankDetails: BankAccount            = BankAccount(isProvided = true, details = None, None, None)
          val vatSchemeWithMissingBankDetails: VatScheme = baseVatScheme.copy(bankAccount = Some(missingBankDetails))
          val buildFailure: BuildFailure =
            BuildFailure("[BankDetailsBlockBuilder] Unable to build submission model: isProvided = true but details are not defined")

          buildBankDetailsBlock(vatSchemeWithMissingBankDetails) mustBe Left(buildFailure)
        }
        "'isProvided' = false but there is no reason given" in new SwitchOn {
          val missingReason: BankAccount            = BankAccount(isProvided = false, details = None, None, None)
          val vatSchemeWithMissingReason: VatScheme = baseVatScheme.copy(bankAccount = Some(missingReason))
          val buildFailure: BuildFailure =
            BuildFailure("[BankDetailsBlockBuilder] Unable to build submission model: isProvided = false but reason is not defined")

          buildBankDetailsBlock(vatSchemeWithMissingReason) mustBe Left(buildFailure)
        }
        "bank details are provided but there is no status" in new SwitchOn {
          val missingStatus: BankAccount            = BankAccount(isProvided = false, details = Some(bankAccountDetailsWithStatus(None)), None, None)
          val vatSchemeWithMissingStatus: VatScheme = baseVatScheme.copy(bankAccount = Some(missingStatus))
          val buildFailure: BuildFailure =
            BuildFailure("[BankDetailsBlockBuilder] Unable to build submission model: bank details have not yet been BARS checked")

          buildBankDetailsBlock(vatSchemeWithMissingStatus) mustBe Left(buildFailure)
        }
      }
    }

    "SubmitBarsInvalidBankDetailsToAPI switch it OFF" should {

      val bankDetailsBlockJson: JsObject = Json.obj(
        "UK" -> Json.obj(
          "accountName"   -> testBankName,
          "sortCode"      -> testSortCode,
          "accountNumber" -> testBankNumber
        )
      )

      val bankDetailsWithRollNumberBlockJson: JsObject = Json.obj(
        "UK" -> Json.obj(
          "accountName"   -> testBankName,
          "sortCode"      -> testSortCode,
          "accountNumber" -> testBankNumber,
          "rollNumber"    -> testRollNumber
        )
      )

      val notValidBankDetailsBlockJson: JsObject = Json.obj(
        "UK" -> Json.obj(
          "accountName"         -> testBankName,
          "sortCode"            -> testSortCode,
          "accountNumber"       -> testBankNumber,
          "bankDetailsNotValid" -> true
        )
      )

      val bankDetailsNotProvidedBlockJson: JsObject = Json.obj(
        "UK" -> Json.obj(
          "reasonBankAccNotProvided" -> NoUKBankAccount.reasonId(BeingSetup)
        )
      )

      val bankDetailsOverseasNotProvidedBlockJson: JsObject = Json.obj(
        "UK" -> Json.obj(
          "reasonBankAccNotProvided" -> NoUKBankAccount.reasonId(OverseasAccount)
        )
      )
      "return the correct json" when {
        "the applicant has a bank account" in new SwitchOff {
          val vatScheme: VatScheme = testVatScheme.copy(
            bankAccount = Some(testBankAccount),
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )

          val result: Either[BuildFailure, JsObject] = buildBankDetailsBlock(vatScheme)
          result mustBe Right(bankDetailsBlockJson)
        }

        "the applicant has a bank account with roll number" in new SwitchOff {
          val vatScheme: VatScheme = testVatScheme.copy(
            bankAccount = Some(testBankAccount.copy(details = Some(testBankDetailsWithRollNumber))),
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )

          val result: Either[BuildFailure, JsObject] = buildBankDetailsBlock(vatScheme)
          result mustBe Right(bankDetailsWithRollNumberBlockJson)
        }

        "the applicant has an indeterminate bank account" in new SwitchOff {
          val vatScheme: VatScheme = testVatScheme.copy(
            bankAccount = Some(testBankAccount.copy(details = Some(testBankDetails.copy(status = Some(IndeterminateStatus))))),
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )

          val result: Either[BuildFailure, JsObject] = buildBankDetailsBlock(vatScheme)
          result mustBe Right(notValidBankDetailsBlockJson)
        }

        "the applicant has an invalid bank account" in new SwitchOff {
          val vatScheme: VatScheme = testVatScheme.copy(
            bankAccount = Some(testBankAccount.copy(details = Some(testBankDetails.copy(status = Some(InvalidStatus))))),
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )

          val result: Either[BuildFailure, JsObject] = buildBankDetailsBlock(vatScheme)
          result mustBe Right(notValidBankDetailsBlockJson)
        }

        "the applicant has a bank account with a sortcode containing hyphens" in new SwitchOff {
          val vatScheme: VatScheme = testVatScheme.copy(
            bankAccount = Some(testBankAccount.copy(details = Some(testBankDetails.copy(sortCode = "01-02-03")))),
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )

          val result: Either[BuildFailure, JsObject] = buildBankDetailsBlock(vatScheme)
          result mustBe Right(bankDetailsBlockJson)
        }

        "the applicant does not have a bank account" in new SwitchOff {
          val vatScheme: VatScheme = testVatScheme.copy(
            bankAccount = Some(testBankAccountNotProvided),
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )

          val result: Either[BuildFailure, JsObject] = buildBankDetailsBlock(vatScheme)
          result mustBe Right(bankDetailsNotProvidedBlockJson)
        }

        "the bank account is missing and user is a NETP" in new SwitchOff {
          val vatScheme: VatScheme = testVatScheme.copy(
            bankAccount = None,
            eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = Individual))
          )

          val result: Either[BuildFailure, JsObject] = buildBankDetailsBlock(vatScheme)
          result mustBe Right(bankDetailsOverseasNotProvidedBlockJson)
        }
      }

      "return a BuildFailure in a Left" when {
        "the bank account details are missing" in new SwitchOff {
          val vatScheme: VatScheme = testVatScheme.copy(
            bankAccount = Some(testBankAccount.copy(details = None)),
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )
          val buildFailure: BuildFailure = BuildFailure(
            "[BankDetailsBlockBuilder] Unable to build submission model as user has not given bank details, nor bank details reason, nor is a NonUK/NonEstablished user")

          buildBankDetailsBlock(vatScheme) mustBe Left(buildFailure)
        }

        "the bank account is missing" in new SwitchOff {
          val vatScheme: VatScheme = testVatScheme.copy(
            bankAccount = None,
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )
          val buildFailure: BuildFailure = BuildFailure(
            "[BankDetailsBlockBuilder] Unable to build submission model as user has not given bank details, nor bank details reason, nor is a NonUK/NonEstablished user")

          buildBankDetailsBlock(vatScheme) mustBe Left(buildFailure)
        }
      }
    }
  }

}
