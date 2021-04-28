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

package controllers

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.JsObject
import play.api.mvc.Result
import play.api.test.FakeRequest
import repositories.RegistrationMongoRepository

import scala.concurrent.Future

class AnnualAccountingSchemeControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import play.api.test.Helpers._

  class Setup {
    val controller: AnnualAccountingSchemeController = new AnnualAccountingSchemeController(mockAnnualAccountingSchemeService, mockAuthConnector, stubControllerComponents()) {
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
    }
  }

  "retrieveAnnualAccountingScheme" should {
    "return an OK with a full valid annual accounting scheme json if the document contains it" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockAnnualAccountingSchemeService.retrieveAnnualAccountingScheme(any()))
        .thenReturn(Future.successful(Some(validFullAAS)))

      val result: Future[Result] = controller.getAnnualAccountingScheme(testRegId)(FakeRequest())
      status(result) mustBe 200
      contentAsJson(result) mustBe validFullAnnualAccountingSchemeJson
    }

    "return a NoContent if the annual accounting scheme block is not present in the document" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockAnnualAccountingSchemeService.retrieveAnnualAccountingScheme(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getAnnualAccountingScheme(testRegId)(FakeRequest())
      status(result) mustBe 204
    }
  }

  "updateAnnualAccountingScheme" should {
    "returns Ok if successful with a full annual accounting scheme" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockAnnualAccountingSchemeService.updateAnnualAccountingScheme(any(), any()))
        .thenReturn(Future.successful(validFullAAS))

      val result: Future[Result] = controller.updateAnnualAccountingScheme(testRegId)(FakeRequest().withBody[JsObject](validFullAnnualAccountingSchemeJson))
      status(result) mustBe 200
      contentAsJson(result) mustBe validFullAnnualAccountingSchemeJson
    }

    "returns InternalServerError if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalid)
      when(mockAnnualAccountingSchemeService.updateAnnualAccountingScheme(any(), any()))
        .thenReturn(Future.failed(new Exception))

      val result: Future[Result] = controller.updateAnnualAccountingScheme(testRegId)(FakeRequest().withBody[JsObject](validFullAnnualAccountingSchemeJson))
      status(result) mustBe 500
    }

    "returns Forbidden if the user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(testRegId, testInternalid)

      val result: Future[Result] = controller.updateAnnualAccountingScheme(testRegId)(FakeRequest().withBody[JsObject](validFullAnnualAccountingSchemeJson))
      status(result) mustBe 403
    }
  }
}