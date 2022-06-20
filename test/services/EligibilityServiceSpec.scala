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
import models.api.{TurnoverEstimates, VatScheme}
import models.submission.RegSociety
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers, Mockito}
import play.api.libs.json.{JsArray, JsObject, JsResultException, Json}
import play.api.test.Helpers._

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
    val completionCapacity = Json.obj("role" -> "director", "name" -> Json.obj(
      "forename" -> "First Name Test",
      "other_forenames" -> "Middle Name Test",
      "surname" -> "Last Name Test"
    ))
    val questions1 = Seq(
      Json.obj("questionId" -> "completionCapacity", "question" -> "Some Question 11", "answer" -> "Some Answer 11", "answerValue" -> completionCapacity),
      Json.obj("questionId" -> "testQId12", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> "val12")
    )
    val questions2 = Seq(
      Json.obj("questionId" -> "voluntaryRegistration", "question" -> "Some Question", "answer" -> "Some Answer", "answerValue" -> true),
      Json.obj("questionId" -> "voluntaryInformation", "question" -> "Some Question", "answer" -> "Some Answer", "answerValue" -> true),
      Json.obj("questionId" -> "applicantUKNino-optionalData", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "JW778877A"),
      Json.obj("questionId" -> "turnoverEstimate", "question" -> "Some Question 21", "answer" -> "Some Answer 21", "answerValue" -> 123456),
      Json.obj("questionId" -> "testQId22", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "val22"),
      Json.obj("questionId" -> "registeringBusiness", "question" -> "Some Question 23", "answer" -> "Some Answer 22", "answerValue" -> "own"),
      Json.obj("questionId" -> "businessEntity", "question" -> "Some Question 23", "answer" -> "Some Answer 22", "answerValue" -> "50"),
      Json.obj("questionId" -> "currentlyTrading", "question" -> "Some Question 24", "answer" -> "Some Answer 24", "answerValue" -> false)
    )
    val section1 = Json.obj("title" -> "test TITLE 1", "data" -> JsArray(questions1))
    val section2 = Json.obj("title" -> "test TITLE 2", "data" -> JsArray(questions2))
    val sections = JsArray(Seq(section1, section2))
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

    "return eligibility data and clear user's frs and aas scheme if the turnover is changed part the thresholds" in new Setup {
      val vatSchemeWithFRSandAAS: VatScheme = testFullVatScheme.copy(returns = Some(validAASReturns))
      val vatSchemeWithoutFRSorAAS: VatScheme = testFullVatScheme.copy(flatRateScheme = None, returns = None)

      when(mockRegistrationMongoRepository.updateEligibilitySubmissionData(any(), any()))
        .thenReturn(Future.successful(testEligibilitySubmissionData))
      when(mockRegistrationMongoRepository.updateEligibilityData(any(), any()))
        .thenReturn(Future.successful(eligibilityData))
      when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(any()))
        .thenReturn(Future.successful(Some(testEligibilitySubmissionData.copy(estimates = Some(TurnoverEstimates(1500000L))))))
      when(mockRegistrationMongoRepository.retrieveVatScheme(any()))
        .thenReturn(Future.successful(Some(vatSchemeWithFRSandAAS)))
      when(mockRegistrationMongoRepository.insertVatScheme(ArgumentMatchers.eq(vatSchemeWithoutFRSorAAS)))
        .thenReturn(Future.successful(vatSchemeWithoutFRSorAAS))

      val result: JsObject = await(service.updateEligibilityData("regId", eligibilityData))

      result mustBe eligibilityData
      verify(mockRegistrationMongoRepository, Mockito.times(1))
        .insertVatScheme(ArgumentMatchers.eq(vatSchemeWithoutFRSorAAS))
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
