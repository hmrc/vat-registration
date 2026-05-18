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
import models.api.BankAccountType.Personal
import models.api._
import models.submission.Individual
import models.{BuildFailure, Voluntary}
import play.api.libs.json.{JsObject, Json}
import services.submission.BankDetailsBlockBuilder.buildBankDetailsBlock
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils.jsonObject

import java.time.LocalDate

class BankDetailsBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture with FeatureSwitching {

  "buildBankDetailsBlock" when {
    "SubmitBarsInvalidBankDetailsToAPI switch it ON" should {
      enable(SubmitBarsInvalidBankDetailsToAPI)
      val baseVatScheme = VatScheme("regId", "internalId", LocalDate.of(2020, 2, 2), VatRegStatus.draft)
      val buildFailure = BuildFailure(
        "Unable to build submission model as user has not give bank details, nor bank details reason, nor is a NonUK/NonEstablished user")
      def bankAccountDetailsWithStatus(status: BankAccountDetailsStatus) =
        BankAccountDetails("name", "sort-Code", "number", Some("rollNumber"), status)

      "return a Right with Json containing bank details and no reason or invalid flag" when {
        "user is submitting valid bank details" in {
          val bankAccountDetails = BankAccount(isProvided = true, Some(bankAccountDetailsWithStatus(ValidStatus)), reason = None, Some(Personal))
          val vatSchemeWithValidBankDetails = baseVatScheme.copy(bankAccount = Some(bankAccountDetails))
          val expectedJson = jsonObject(
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
        "user fails bars 3 times and has been locked out" in {
          val invalidBankDetailsWithLockoutReason =
            BankAccount(isProvided = true, Some(bankAccountDetailsWithStatus(InvalidStatus)), reason = Some(DontWantToProvide), Some(Personal))
          val vatSchemeWithInvalidBankDetailsAndLockoutReason = baseVatScheme.copy(bankAccount = Some(invalidBankDetailsWithLockoutReason))
          val expectedJson = jsonObject(
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
        "user fails bars and then changes their mind to give a reason" in {
          val invalidBankDetailsWithReasonToNotProvide =
            BankAccount(isProvided = false, Some(bankAccountDetailsWithStatus(InvalidStatus)), reason = Some(BeingSetup), Some(Personal))
          val vatSchemeWithInvalidBankDetailsAndReason = baseVatScheme.copy(bankAccount = Some(invalidBankDetailsWithReasonToNotProvide))
          val indeterminateBankDetailsWithReasonToNotProvide =
            BankAccount(isProvided = false, Some(bankAccountDetailsWithStatus(IndeterminateStatus)), reason = Some(BeingSetup), Some(Personal))
          val vatSchemeWithIndeterminateBankDetailsAndReason = baseVatScheme.copy(bankAccount = Some(indeterminateBankDetailsWithReasonToNotProvide))
          val expectedJson = jsonObject(
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
        "user chooses to submit a reason and has not failed any BARS checks" in {
          val invalidBankDetailsWithLockoutReason =
            BankAccount(isProvided = false, details = None, reason = Some(AccountNotInBusinessName), Some(Personal))
          val vatSchemeWithInvalidBankDetailsAndLockoutReason = baseVatScheme.copy(bankAccount = Some(invalidBankDetailsWithLockoutReason))
          val expectedJson                                    = jsonObject("UK" -> jsonObject("reasonBankAccNotProvided" -> "6"))

          buildBankDetailsBlock(vatSchemeWithInvalidBankDetailsAndLockoutReason) mustBe Right(expectedJson)
        }
      }

      "return a Right with Json containing only the 'OverseasAccount' (ID: 3) reason" when {
        "the partyType is 'Individual'" in {
          val eligibilitySubmissionData = EligibilitySubmissionData(Threshold(true), None, partyType = Individual, Voluntary, None, true, None, true)
          val vatSchemeForIndividual    = baseVatScheme.copy(eligibilitySubmissionData = Some(eligibilitySubmissionData))
          val expectedJson              = jsonObject("UK" -> jsonObject("reasonBankAccNotProvided" -> "3"))

          buildBankDetailsBlock(vatSchemeForIndividual) mustBe Right(expectedJson)
        }
      }

      "return a Left with a BuildFailure and issue description" when {
        "there is no BankAccount data" in {
          val vatSchemeWithMissingBankAccountData = baseVatScheme.copy(bankAccount = None)

          buildBankDetailsBlock(vatSchemeWithMissingBankAccountData) mustBe Left(buildFailure)
        }
        "'isProvided' = true but there are no bank details" in {
          val missingBankDetails              = BankAccount(isProvided = true, details = None, None, None)
          val vatSchemeWithMissingBankDetails = baseVatScheme.copy(bankAccount = Some(missingBankDetails))

          buildBankDetailsBlock(vatSchemeWithMissingBankDetails) mustBe Left(buildFailure)
        }
        "'isProvided' = false but there is no reason given" in {
          val missingReason              = BankAccount(isProvided = false, details = None, None, None)
          val vatSchemeWithMissingReason = baseVatScheme.copy(bankAccount = Some(missingReason))

          buildBankDetailsBlock(vatSchemeWithMissingReason) mustBe Left(buildFailure)
        }
      }
    }

    "SubmitBarsInvalidBankDetailsToAPI switch it OFF" should {
      disable(SubmitBarsInvalidBankDetailsToAPI)

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
        "the applicant has a bank account" in {
          val vatScheme = testVatScheme.copy(
            bankAccount = Some(testBankAccount),
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )

          val result = buildBankDetailsBlock(vatScheme)
          result mustBe Right(bankDetailsBlockJson)
        }

        "the applicant has a bank account with roll number" in {
          val vatScheme = testVatScheme.copy(
            bankAccount = Some(testBankAccount.copy(details = Some(testBankDetailsWithRollNumber))),
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )

          val result = buildBankDetailsBlock(vatScheme)
          result mustBe Right(bankDetailsWithRollNumberBlockJson)
        }

        "the applicant has an indeterminate bank account" in {
          val vatScheme = testVatScheme.copy(
            bankAccount = Some(testBankAccount.copy(details = Some(testBankDetails.copy(status = IndeterminateStatus)))),
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )

          val result = buildBankDetailsBlock(vatScheme)
          result mustBe Right(notValidBankDetailsBlockJson)
        }

        "the applicant has an invalid bank account" in {
          val vatScheme = testVatScheme.copy(
            bankAccount = Some(testBankAccount.copy(details = Some(testBankDetails.copy(status = InvalidStatus)))),
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )

          val result = buildBankDetailsBlock(vatScheme)
          result mustBe Right(notValidBankDetailsBlockJson)
        }

        "the applicant has a bank account with a sortcode containing hyphens" in {
          val vatScheme = testVatScheme.copy(
            bankAccount = Some(testBankAccount.copy(details = Some(testBankDetails.copy(sortCode = "01-02-03")))),
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )

          val result = buildBankDetailsBlock(vatScheme)
          result mustBe Right(bankDetailsBlockJson)
        }

        "the applicant does not have a bank account" in {
          val vatScheme = testVatScheme.copy(
            bankAccount = Some(testBankAccountNotProvided),
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )

          val result = buildBankDetailsBlock(vatScheme)
          result mustBe Right(bankDetailsNotProvidedBlockJson)
        }

        "the bank account is missing and user is a NETP" in {
          val vatScheme = testVatScheme.copy(
            bankAccount = None,
            eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(partyType = Individual))
          )

          val result = buildBankDetailsBlock(vatScheme)
          result mustBe Right(bankDetailsOverseasNotProvidedBlockJson)
        }
      }
      "throw an Interval Server Exception" when {
        "the bank account details are missing" in {
          val vatScheme = testVatScheme.copy(
            bankAccount = Some(testBankAccount.copy(details = None)),
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )

          intercept[InternalServerException](buildBankDetailsBlock(vatScheme))
        }

        "the bank account is missing" in {
          val vatScheme = testVatScheme.copy(
            bankAccount = None,
            eligibilitySubmissionData = Some(testEligibilitySubmissionData)
          )
          intercept[InternalServerException](buildBankDetailsBlock(vatScheme))
        }
      }
    }
  }
}
