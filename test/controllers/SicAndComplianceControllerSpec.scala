/*
 * Copyright 2020 HM Revenue & Customs
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

import common.RegistrationId
import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.SicAndCompliance
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import repositories.RegistrationMongoRepository

import scala.concurrent.Future
class SicAndComplianceControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import play.api.test.Helpers._

  class Setup {
    val controller = new SicAndComplianceControllerImpl(sicAndComplianceService = mockSicAndComplianceService, authConnector = mockAuthConnector){
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
    }
  }

  def mockGetSicAndComplianceFromService(res:Future[Option[SicAndCompliance]]):OngoingStubbing[Future[Option[SicAndCompliance]]] = when(mockSicAndComplianceService.getSicAndCompliance(any())(any())).thenReturn(res)

  def mockUpdateSicAndComplianceFromService(res:Future[SicAndCompliance]) :OngoingStubbing[Future[SicAndCompliance]] = when(mockSicAndComplianceService.updateSicAndCompliance(any(),any())(any())).thenReturn(res)

  "getSicAndCompliance" should {
    "return valid Json if record returned from service" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      mockGetSicAndComplianceFromService(Future.successful(validSicAndCompliance))

      val result = controller.getSicAndCompliance(regId.value)(FakeRequest())
      status(result) shouldBe 200
      contentAsJson(result) shouldBe validSicAndComplianceJson

    }
    "return 204 when nothing is returned but document exists" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      mockGetSicAndComplianceFromService(Future.successful(None))

      val result = controller.getSicAndCompliance(regId.value)(FakeRequest())
      status(result) shouldBe 204
    }
    "returns 404 if none found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      mockGetSicAndComplianceFromService(Future.failed(MissingRegDocument(RegistrationId("foo"))))

      val result = controller.getSicAndCompliance(regId.value)(FakeRequest())
      status(result) shouldBe 404
    }
    "returns 403 if not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(regId.value,internalid)

      val result = controller.getSicAndCompliance(regId.value)(FakeRequest())
      status(result) shouldBe 403
    }
  }
  "updateSicAndCompliance" should {
    "return 200 and the updated model as json when a record exists and the update is successful" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      mockUpdateSicAndComplianceFromService(Future.successful(validSicAndCompliance.get))

      val result = controller.updateSicAndCompliance(regId.value)(FakeRequest().withBody[JsObject](validSicAndComplianceJson))
      contentAsJson(result) shouldBe validSicAndComplianceJson
    }
    "returns 404 if regId not found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      mockUpdateSicAndComplianceFromService(Future.failed(MissingRegDocument(RegistrationId("testId"))))

      val result = controller.updateSicAndCompliance(regId.value)(FakeRequest().withBody[JsObject](validSicAndComplianceJson))
      status(result) shouldBe 404
    }
    "returns 500 if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      mockUpdateSicAndComplianceFromService(Future.failed(new Exception))

      val result = controller.updateSicAndCompliance(regId.value)(FakeRequest().withBody[JsObject](validSicAndComplianceJson))
      status(result) shouldBe 500
    }
    "returns 403 if the user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(regId.value,internalid)

      val result = controller.updateSicAndCompliance(regId.value)(FakeRequest().withBody[JsObject](validSicAndComplianceJson))
      status(result) shouldBe 403
    }
  }
}