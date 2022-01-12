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

import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.JsObject
import play.api.mvc.Result
import play.api.test.FakeRequest
import repositories.VatSchemeRepository

import scala.concurrent.Future

class FlatRateSchemeControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import play.api.test.Helpers._

  class Setup {
    val controller: FlatRateSchemeController = new FlatRateSchemeController(mockFlatRateSchemeService, mockAuthConnector, stubControllerComponents()){
      override val resourceConn: VatSchemeRepository = mockRegistrationMongoRepository
    }
  }

  "fetchFlatRateScheme" should {
    "return an OK with a full valid flat rate scheme json if the document contains it" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId,testInternalId)
      when(mockFlatRateSchemeService.retrieveFlatRateScheme(any()))
        .thenReturn(Future.successful(Some(validFullFlatRateScheme)))

      val result: Future[Result] = controller.fetchFlatRateScheme(testRegId)(FakeRequest())
      status(result) mustBe 200
      contentAsJson(result) mustBe validFullFlatRateSchemeJson
    }

    "return an OK with a valid flat rate scheme json where the frsDetails is not present" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId,testInternalId)
      when(mockFlatRateSchemeService.retrieveFlatRateScheme(any()))
        .thenReturn(Future.successful(Some(validEmptyFlatRateScheme)))

      val result: Future[Result] = controller.fetchFlatRateScheme(testRegId)(FakeRequest())
      status(result) mustBe 200
      contentAsJson(result) mustBe validEmptyFlatRateSchemeJson
    }

    "return a NoContent if the flat rate scheme block is not present in the document" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId,testInternalId)
      when(mockFlatRateSchemeService.retrieveFlatRateScheme(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.fetchFlatRateScheme(testRegId)(FakeRequest())
      status(result) mustBe 204
    }

    "return NotFound if the registration document was not found for the regId provided" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId,testInternalId)
      when(mockFlatRateSchemeService.retrieveFlatRateScheme(any()))
        .thenReturn(Future.failed(MissingRegDocument(testRegId)))

      val result: Future[Result] = controller.fetchFlatRateScheme(testRegId)(FakeRequest())
      status(result) mustBe 404
    }

    "return Forbidden if the registration document was not found for the regId provided" in new Setup {
      AuthorisationMocks.mockNotAuthorised(testRegId,testInternalId)

      val result: Future[Result] = controller.fetchFlatRateScheme(testRegId)(FakeRequest())
      status(result) mustBe 403
    }
  }

  "updateFlatRateScheme" should {
    "returns Ok if successful with a full flat rate scheme" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId,testInternalId)
      when(mockFlatRateSchemeService.updateFlatRateScheme(any(), any()))
        .thenReturn(Future.successful(validFullFlatRateScheme))

      val result: Future[Result] = controller.updateFlatRateScheme(testRegId)(FakeRequest().withBody[JsObject](validFullFlatRateSchemeJson))
      status(result) mustBe 200
      contentAsJson(result) mustBe validFullFlatRateSchemeJson
    }

    "returns Ok if successful with a missing frsDetails" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId,testInternalId)
      when(mockFlatRateSchemeService.updateFlatRateScheme(any(), any()))
        .thenReturn(Future.successful(validEmptyFlatRateScheme))

      val result: Future[Result] = controller.updateFlatRateScheme(testRegId)(FakeRequest().withBody[JsObject](validEmptyFlatRateSchemeJson))
      status(result) mustBe 200
      contentAsJson(result) mustBe validEmptyFlatRateSchemeJson
    }

    "returns NotFound if the registration is not found" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId,testInternalId)
      when(mockFlatRateSchemeService.updateFlatRateScheme(any(), any()))
        .thenReturn(Future.failed(MissingRegDocument(testRegId)))

      val result: Future[Result] = controller.updateFlatRateScheme(testRegId)(FakeRequest().withBody[JsObject](validFullFlatRateSchemeJson))
      status(result) mustBe 404
    }

    "returns InternalServerError if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId,testInternalId)
      when(mockFlatRateSchemeService.updateFlatRateScheme(any(), any()))
        .thenReturn(Future.failed(new Exception))

      val result: Future[Result] = controller.updateFlatRateScheme(testRegId)(FakeRequest().withBody[JsObject](validFullFlatRateSchemeJson))
      status(result) mustBe 500
    }

    "returns Forbidden if the user is not authoirised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(testRegId,testInternalId)

      val result: Future[Result] = controller.updateFlatRateScheme(testRegId)(FakeRequest().withBody[JsObject](validFullFlatRateSchemeJson))
      status(result) mustBe 403
    }
  }

  "removeFlatRateScheme" should {
    "returns Ok if successful" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId,testInternalId)
      when(mockFlatRateSchemeService.removeFlatRateScheme(any()))
        .thenReturn(Future.successful(true))

      val result: Future[Result] = controller.removeFlatRateScheme(testRegId)(FakeRequest())
      status(result) mustBe 200
    }

    "returns NotFound if the registration is not found" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId,testInternalId)
      when(mockFlatRateSchemeService.removeFlatRateScheme(any()))
        .thenReturn(Future.failed(MissingRegDocument(testRegId)))

      val result: Future[Result] = controller.removeFlatRateScheme(testRegId)(FakeRequest())
      status(result) mustBe 404
    }

    "returns InternalServerError if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId,testInternalId)
      when(mockFlatRateSchemeService.removeFlatRateScheme(any()))
        .thenReturn(Future.failed(new Exception))

      val result: Future[Result] = controller.removeFlatRateScheme(testRegId)(FakeRequest())
      status(result) mustBe 500
    }

    "returns Forbidden if the user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(testRegId,testInternalId)

      val result: Future[Result] = controller.removeFlatRateScheme(testRegId)(FakeRequest())
      status(result) mustBe 403
    }
  }

}