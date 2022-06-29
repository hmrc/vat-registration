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

package services

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.VatScheme
import models.submission.RegSociety
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers, Mockito}
import play.api.libs.json.{JsArray, JsObject, JsResultException, Json}
import play.api.test.Helpers._

import java.time.LocalDate
import scala.concurrent.Future

class EligibilityServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service: EligibilityService = new EligibilityService(
      registrationRepository = mockRegistrationMongoRepository
    )
  }

  val json: JsObject = Json.obj("test" -> "value test")

  "getEligibilityData" should {
    "return an eligibility data json if found" in new Setup {
      when(mockRegistrationMongoRepository.fetchEligibilityData(any()))
        .thenReturn(Future.successful(Some(json)))

      val result: Option[JsObject] = await(service.getEligibilityData("regId"))
      result mustBe Some(json)
    }

    "return None if none found matching regId" in new Setup {
      when(mockRegistrationMongoRepository.fetchEligibilityData(any()))
        .thenReturn(Future.successful(None))

      val result: Option[JsObject] = await(service.getEligibilityData("regId"))
      result mustBe None
    }
  }

  "updateEligibilityData" should {
    val questions = Seq(
      Json.obj("questionId" -> "thresholdPreviousThirtyDays-optionalData", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> testEligibilitySubmissionData.threshold.thresholdPreviousThirtyDays),
      Json.obj("questionId" -> "thresholdInTwelveMonths-optionalData", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> testEligibilitySubmissionData.threshold.thresholdInTwelveMonths),
      Json.obj("questionId" -> "registeringBusiness", "question" -> "Some Question 23", "answer" -> "Some Answer 22", "answerValue" -> "own"),
      Json.obj("questionId" -> "businessEntity", "question" -> "Some Question 23", "answer" -> "Some Answer 22", "answerValue" -> "50")
    )
    val section = Json.obj("title" -> "test TITLE 2", "data" -> JsArray(questions))
    val sections = JsArray(Seq(section))
    val eligibilityData = Json.obj("sections" -> sections)

    "return the data that is being provided" in new Setup {
      when(mockRegistrationMongoRepository.updateEligibilitySubmissionData(any(), any()))
        .thenReturn(Future.successful(testEligibilitySubmissionData))
      when(mockRegistrationMongoRepository.updateEligibilityData(any(), any()))
        .thenReturn(Future.successful(eligibilityData))
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(None))

      val result: JsObject = await(service.updateEligibilityData("regId", eligibilityData))

      result mustBe eligibilityData
    }

    "return eligibility data and clear user's vat scheme if the partytype is changed" in new Setup {
      val testClearedVatScheme = testVatScheme.copy(
        confirmInformationDeclaration = Some(true)
      )
      when(mockRegistrationMongoRepository.updateEligibilitySubmissionData(any(), any()))
        .thenReturn(Future.successful(testEligibilitySubmissionData))
      when(mockRegistrationMongoRepository.updateEligibilityData(any(), any()))
        .thenReturn(Future.successful(eligibilityData))
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(partyType = RegSociety))))
      when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
        .thenReturn(Future.successful(Some(testFullVatScheme)))
      when(mockRegistrationMongoRepository.insertVatScheme(ArgumentMatchers.eq(testClearedVatScheme)))
        .thenReturn(Future.successful(testClearedVatScheme))

      val result: JsObject = await(service.updateEligibilityData("regId", eligibilityData))

      result mustBe eligibilityData
      verify(mockRegistrationMongoRepository, Mockito.times(1))
        .insertVatScheme(ArgumentMatchers.eq(testClearedVatScheme))
    }

    "return eligibility data and clear user's transactor details if the transactor answer is changed" in new Setup {
      val vatSchemeWithTransactor: VatScheme = testFullVatScheme.copy(transactorDetails = Some(validTransactorDetails))
      val vatSchemeWithoutTransactor: VatScheme = testFullVatScheme.copy(transactorDetails = None)

      when(mockRegistrationMongoRepository.updateEligibilitySubmissionData(any(), any()))
        .thenReturn(Future.successful(testEligibilitySubmissionData))
      when(mockRegistrationMongoRepository.updateEligibilityData(any(), any()))
        .thenReturn(Future.successful(eligibilityData))
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(isTransactor = true))))
      when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
        .thenReturn(Future.successful(Some(vatSchemeWithTransactor)))
      when(mockRegistrationMongoRepository.insertVatScheme(ArgumentMatchers.eq(vatSchemeWithoutTransactor)))
        .thenReturn(Future.successful(vatSchemeWithoutTransactor))

      val result: JsObject = await(service.updateEligibilityData("regId", eligibilityData))

      result mustBe eligibilityData
      verify(mockRegistrationMongoRepository, Mockito.times(1))
        .insertVatScheme(ArgumentMatchers.eq(vatSchemeWithoutTransactor))
    }

    "return eligibility data and clear user's exemption answer if the exception answer is true" in new Setup {
      val questions = Seq(
        Json.obj("questionId" -> "thresholdPreviousThirtyDays-optionalData", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> testEligibilitySubmissionData.threshold.thresholdPreviousThirtyDays),
        Json.obj("questionId" -> "thresholdInTwelveMonths-optionalData", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> testEligibilitySubmissionData.threshold.thresholdInTwelveMonths),
        Json.obj("questionId" -> "vatRegistrationException", "question" -> "Some Question", "answer" -> "Some Answer", "answerValue" -> true),
        Json.obj("questionId" -> "registeringBusiness", "question" -> "Some Question 23", "answer" -> "Some Answer 22", "answerValue" -> "own"),
        Json.obj("questionId" -> "businessEntity", "question" -> "Some Question 23", "answer" -> "Some Answer 22", "answerValue" -> "50")
      )
      val section = Json.obj("title" -> "test TITLE 2", "data" -> JsArray(questions))
      val sections = JsArray(Seq(section))
      val eligibilityData = Json.obj("sections" -> sections)

      val vatSchemeWithExemption: VatScheme = testFullVatScheme
        .copy(
          returns = Some(testReturns.copy(appliedForExemption = Some(true))),
          eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(appliedForException = Some(false)))
        )
      val vatSchemeWithoutExemption: VatScheme = vatSchemeWithExemption
        .copy(
          returns = Some(testReturns.copy(appliedForExemption = None))
        )
      when(mockRegistrationMongoRepository.updateEligibilitySubmissionData(any(), any()))
        .thenReturn(Future.successful(testEligibilitySubmissionData.copy(appliedForException = Some(true))))
      when(mockRegistrationMongoRepository.updateEligibilityData(any(), any()))
        .thenReturn(Future.successful(eligibilityData))
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(appliedForException = Some(false)))))
      when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
        .thenReturn(Future.successful(Some(vatSchemeWithExemption)))
      when(mockRegistrationMongoRepository.insertVatScheme(ArgumentMatchers.eq(vatSchemeWithoutExemption)))
        .thenReturn(Future.successful(vatSchemeWithoutExemption))

      val result: JsObject = await(service.updateEligibilityData("regId", eligibilityData))

      result mustBe eligibilityData
      verify(mockRegistrationMongoRepository, Mockito.times(1))
        .insertVatScheme(ArgumentMatchers.eq(vatSchemeWithoutExemption))
    }

    "return eligibility data and not clear any vatscheme fields where eligibility data is unchanged" in new Setup {
      when(mockRegistrationMongoRepository.updateEligibilitySubmissionData(any(), any()))
        .thenReturn(Future.successful(testEligibilitySubmissionData))
      when(mockRegistrationMongoRepository.updateEligibilityData(any(), any()))
        .thenReturn(Future.successful(eligibilityData))
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))
      when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
        .thenReturn(Future.successful(Some(testFullVatScheme.copy(eligibilitySubmissionData = Some(testEligibilitySubmissionData)))))

      val result: JsObject = await(service.updateEligibilityData("regId", eligibilityData))

      result mustBe eligibilityData
    }

    "encounter a JsResultException if json provided is incorrect" in new Setup {
      val incorrectQuestionValue: JsObject = Json.obj("sections" -> JsArray(Seq(Json.obj("title" -> "test TITLE 1", "data" -> JsArray(Seq(Json.obj(
        "questionId" -> "thresholdPreviousThirtyDays-optionalData", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> "234324"
      )))))))
      val incorrectEligibilityData: JsObject = eligibilityData.deepMerge(incorrectQuestionValue)
      intercept[JsResultException](await(service.updateEligibilityData("regId", incorrectEligibilityData)))
    }

    "encounter an exception if an error occurs during eligibility submission data update" in new Setup {
      when(mockRegistrationMongoRepository.updateEligibilitySubmissionData(any(), any()))
        .thenReturn(Future.failed(new Exception("")))

      intercept[Exception](await(service.updateEligibilityData("regId", eligibilityData)))
    }

    "encounter an exception if an error occurs" in new Setup {
      when(mockRegistrationMongoRepository.updateEligibilitySubmissionData(any(), any()))
        .thenReturn(Future.successful(testEligibilitySubmissionData))
      when(mockRegistrationMongoRepository.updateEligibilityData(any(), any()))
        .thenReturn(Future.failed(new Exception("")))

      intercept[Exception](await(service.updateEligibilityData("regId", eligibilityData)))
    }
  }
}
