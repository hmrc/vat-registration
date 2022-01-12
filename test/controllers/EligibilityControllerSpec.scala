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

package controllers

import java.time.LocalDate

import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import repositories.VatSchemeRepository

import scala.concurrent.Future

class EligibilityControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import play.api.test.Helpers._

  class Setup {
    val controller: EligibilityController = new EligibilityController(mockEligibilityService, mockAuthConnector, stubControllerComponents()) {
      override val resourceConn: VatSchemeRepository = mockRegistrationMongoRepository
    }
  }

  val validEligibilityJson: JsObject = Json.parse(
    """
      |{
      | "version": 1,
      | "result": "thisIsAValidReason"
      |}
    """.stripMargin).as[JsObject]

  val upsertEligibilityJson: JsObject = Json.parse(
    """
      |{
      | "version": 1,
      | "result": "thisIsAnUpsert"
      |}
    """.stripMargin).as[JsObject]

  val invalidUpsertJson: JsObject = Json.parse(
    """
      |{
      | "result": "thisIsAnUpsert"
      |}
    """.stripMargin).as[JsObject]

  "getEligibilityData" should {
    val json = Json.obj("foo" -> "bar")
    "return 200 and a JsObject" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      when(mockEligibilityService.getEligibilityData(any())).thenReturn(Future.successful(Some(json)))

      val res: Future[Result] = controller.getEligibilityData(testRegId)(FakeRequest())
      status(res) mustBe 200
      contentAsJson(res) mustBe json
    }
    "return 204 when nothing exists" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      when(mockEligibilityService.getEligibilityData(any())).thenReturn(Future.successful(None))

      val res: Future[Result] = controller.getEligibilityData(testRegId)(FakeRequest())
      status(res) mustBe 204
    }
    "return 404 when no reg doc exists" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      when(mockEligibilityService.getEligibilityData(any())).thenReturn(Future.failed(new MissingRegDocument("foo")))

      val res: Future[Result] = controller.getEligibilityData(testRegId)(FakeRequest())
      status(res) mustBe 404
    }
  }
  "updateEligibilityData" should {
    val thresholdPreviousThirtyDays = LocalDate.of(2017, 5, 23)
    val thresholdInTwelveMonths = LocalDate.of(2017, 7, 16)
    val applicantDetails = Json.obj("role" -> "director", "name" -> Json.obj(
      "forename" -> "First Name Test",
      "other_forenames" -> "Middle Name Test",
      "surname" -> "Last Name Test"
    ))
    val json = Json.parse(
      s"""{
         |"sections": [
         |   {
         |     "title": "VAT details",
         |     "data": [
         |       {"questionId": "mandatoryRegistration", "question": "Some Question 11", "answer": "Some Answer 11", "answerValue": true},
         |       {"questionId": "voluntaryRegistration", "question": "Some Question 12", "answer": "Some Answer 12", "answerValue": false},
         |       {"questionId": "thresholdPreviousThirtyDays", "question": "Some Question 12", "answer": "Some Answer 12", "answerValue": "$thresholdPreviousThirtyDays"},
         |       {"questionId": "thresholdInTwelveMonths", "question": "Some Question 12", "answer": "Some Answer 12", "answerValue": "$thresholdInTwelveMonths"}
         |     ]
         |   },
         |   {
         |     "title": "Director details",
         |     "data": [
         |       {"questionId": "applicantUKNino", "question": "Some Question 11", "answer": "Some Answer 11", "answerValue": "SR123456C"},
         |       {"questionId": "turnoverEstimate", "question": "Some Question 11", "answer": "Some Answer 11", "answerValue": 2024},
         |       {"questionId": "completionCapacity", "question": "Some Question 11", "answer": "Some Answer 11", "answerValue": $applicantDetails},
         |       {"questionId":"fooDirectorDetails3","question": "Date of birth", "answer": "1 January 2000", "answerValue": true}
         |     ]
         |   }
         | ]
         |}
        """.stripMargin)

    "return 200 after a successful update" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      when(mockEligibilityService.updateEligibilityData(any(), any())(any())).thenReturn(Future.successful(json.as[JsObject]))
      val result: Future[Result] = controller.updateEligibilityData(testRegId)(FakeRequest().withBody[JsValue](json))
      status(result) mustBe 200
    }
    "returns 403 if user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(testRegId,testInternalId)

      val result: Future[Result] = controller.updateEligibilityData(testRegId)(FakeRequest().withBody[JsValue](json))
      status(result) mustBe 403
    }

    "returns 400 if json received is invalid" in new Setup {
      val invalidJson: JsValue = Json.parse(
        s"""
           |[
           |   {
           |     "title": "VAT details",
           |     "data": [
           |       {"questionId": "mandatoryRegistration", "question": "Some Question 11", "answer": "Some Answer 11", "answerValue": true},
           |       {"questionId": "voluntaryRegistration", "question": "Some Question 12", "answer": "Some Answer 12", "answerValue": false}
           |     ]
           |   },
           |   {
           |     "title": "Director details",
           |     "data": [
           |       {"questionId": "turnoverEstimate", "question": "Some Question 11", "answer": "Some Answer 11", "answerValue": 2024},
           |       {"questionId":"fooDirectorDetails3","question": "Date of birth", "answer": "1 January 2000", "answerValue": true}
           |     ]
           |   }
           |]
        """.stripMargin)
      AuthorisationMocks.mockAuthorised(testRegId,testInternalId)

      val result: Future[Result] = controller.updateEligibilityData(testRegId)(FakeRequest().withBody[JsValue](invalidJson))
      status(result) mustBe 400
    }

    "returns 404 if the registration is not found" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId,testInternalId)
      when(mockEligibilityService.updateEligibilityData(any(), any())(any()))
        .thenReturn(Future.failed(new MissingRegDocument(testRegId)))

      val result: Future[Result] = controller.updateEligibilityData(testRegId)(FakeRequest().withBody[JsValue](json))
      status(result) mustBe 404
    }

    "returns 500 if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId,testInternalId)
      when(mockEligibilityService.updateEligibilityData(any(), any())(any()))
        .thenReturn(Future.failed(new Exception))

      val result: Future[Result] = controller.updateEligibilityData(testRegId)(FakeRequest().withBody[JsValue](json))
      status(result) mustBe 500
    }
  }
}