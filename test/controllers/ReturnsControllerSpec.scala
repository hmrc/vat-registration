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
import models.api.returns.{AASDetails, Annual, BankGIRO, JanDecStagger, MonthlyPayment, PaymentFrequency, PaymentMethod, Returns, ReturnsFrequency, Stagger}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import repositories.RegistrationMongoRepository

import scala.concurrent.Future

class ReturnsControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import play.api.test.Helpers._

  class Setup {
    val controller: ReturnsController = new ReturnsController(mockAnnualAccountingSchemeService, mockAuthConnector, stubControllerComponents()) {
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
    }
  }

  val testZeroRatedSupplies = 10000.5

  val testAnnualReturns: Returns = Returns(
    Some(testZeroRatedSupplies),
    reclaimVatOnMostReturns = false,
    Annual,
    JanDecStagger,
    Some(testDate),
    Some(AASDetails(BankGIRO, MonthlyPayment)),
    None
  )

  val validAnnualReturnsJson: JsObject = Json.obj(
    "zeroRatedSupplies" -> testZeroRatedSupplies,
    "reclaimVatOnMostReturns" -> false,
    "returnsFrequency" -> Json.toJson[ReturnsFrequency](Annual),
    "staggerStart" -> Json.toJson[Stagger](JanDecStagger),
    "startDate" -> testDate,
    "annualAccountingDetails" -> Json.obj(
      "paymentMethod" -> Json.toJson[PaymentMethod](BankGIRO),
      "paymentFrequency" -> Json.toJson[PaymentFrequency](MonthlyPayment)
    )
  )

  val invalidReturnsJson: JsObject = Json.obj()

  "getReturns" should {
    "return an OK with a full valid returns json if the document contains it" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      when(mockAnnualAccountingSchemeService.retrieveReturns(any()))
        .thenReturn(Future.successful(Some(testAnnualReturns)))

      val result: Future[Result] = controller.getReturns(testRegId)(FakeRequest())
      status(result) mustBe 200
      contentAsJson(result) mustBe validAnnualReturnsJson
    }

    "return a NoContent if the returns block is not present in the document" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      when(mockAnnualAccountingSchemeService.retrieveReturns(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.getReturns(testRegId)(FakeRequest())
      status(result) mustBe 204
    }
  }

  "updateReturns" should {
    "returns Ok if successful with a full returns json" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      when(mockAnnualAccountingSchemeService.updateReturns(any(), any()))
        .thenReturn(Future.successful(testAnnualReturns))

      val result: Future[Result] = controller.updateReturns(testRegId)(FakeRequest().withBody[JsObject](validAnnualReturnsJson))
      status(result) mustBe 200
      contentAsJson(result) mustBe validAnnualReturnsJson
    }

    "returns InternalServerError if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      when(mockAnnualAccountingSchemeService.updateReturns(any(), any()))
        .thenReturn(Future.failed(new Exception))

      val result: Future[Result] = controller.updateReturns(testRegId)(FakeRequest().withBody[JsObject](validAnnualReturnsJson))
      status(result) mustBe 500
    }

    "returns Forbidden if the user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(testRegId, testInternalId)

      val result: Future[Result] = controller.updateReturns(testRegId)(FakeRequest().withBody[JsObject](validAnnualReturnsJson))
      status(result) mustBe 403
    }
  }
}