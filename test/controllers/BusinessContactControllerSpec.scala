/*
 * Copyright 2019 HM Revenue & Customs
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
import models.api.BusinessContact
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import repositories.RegistrationMongoRepository
import services.BusinessContactService
import uk.gov.hmrc.auth.core.AuthConnector

import scala.concurrent.Future

class BusinessContactControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import play.api.test.Helpers._

  class Setup {
    val controller = new BusinessContactController {
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
      override lazy val authConnector: AuthConnector = mockAuthConnector
      override val businessContactService: BusinessContactService = mockBusinessContactService
    }
  }
    def mockGetBusinessContactFromService(res:Future[Option[BusinessContact]]):OngoingStubbing[Future[Option[BusinessContact]]] = when(mockBusinessContactService.getBusinessContact(any())(any())).thenReturn(res)

    def mockUpdateBusinessContactToSoService(res:Future[BusinessContact]) :OngoingStubbing[Future[BusinessContact]] = when(mockBusinessContactService.updateBusinessContact(any(),any())(any())).thenReturn(res)


  "getBusinessContact" should {
    "return valid Json if record returned from service" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      mockGetBusinessContactFromService(Future.successful(validBusinessContact))

      val result = controller.getBusinessContact(regId.value)(FakeRequest())
      status(result) shouldBe 200
      await(contentAsJson(result)) shouldBe validBusinessContactJson

    }
    "return 204 when nothing is returned but document exists" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      mockGetBusinessContactFromService(Future.successful(None))

      val result = controller.getBusinessContact(regId.value)(FakeRequest())
      status(result) shouldBe 204
    }
    "returns 404 if none found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      mockGetBusinessContactFromService(Future.failed(MissingRegDocument(RegistrationId("foo"))))

      val result = controller.getBusinessContact(regId.value)(FakeRequest())
      status(result) shouldBe 404
    }
    "returns 403 if not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(regId.value,internalid)

      val result = controller.getBusinessContact(regId.value)(FakeRequest())
      status(result) shouldBe 403
    }
  }
  "updateBusinessContact" should {
    "return 200 and the updated model as json when a record exists and the update is successful" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      mockUpdateBusinessContactToSoService(Future.successful(validBusinessContact.get))

      val result = controller.updateBusinessContact(regId.value)(FakeRequest().withBody[JsObject](validBusinessContactJson))
      status(result) shouldBe 200
      await(contentAsJson(result)) shouldBe validBusinessContactJson
    }
    "returns 404 if regId not found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      mockUpdateBusinessContactToSoService(Future.failed(MissingRegDocument(RegistrationId("testId"))))

      val result = controller.updateBusinessContact(regId.value)(FakeRequest().withBody[JsObject](validBusinessContactJson))
      status(result) shouldBe 404
    }
    "returns 500 if an error occurs" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      mockUpdateBusinessContactToSoService(Future.failed(new Exception))

      val result = controller.updateBusinessContact(regId.value)(FakeRequest().withBody[JsObject](validBusinessContactJson))
      status(result) shouldBe 500
    }
    "returns 403 if the user is not authorised" in new Setup {
      AuthorisationMocks.mockNotAuthorised(regId.value,internalid)

      val result = controller.updateBusinessContact(regId.value)(FakeRequest().withBody[JsObject](validBusinessContactJson))
      status(result) shouldBe 403
    }
  }
}