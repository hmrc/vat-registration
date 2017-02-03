/*
 * Copyright 2017 HM Revenue & Customs
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

package controller

import common.exceptions.GenericServiceException
import controllers.VatRegistrationController
import helpers.VatRegSpec
import models.VatChoice
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.{Created, ServiceUnavailable}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future


class VatRegistrationControllerSpec extends VatRegSpec {

  val testId = "testId"
  val vatChoice: VatChoice = VatChoice.blank(new DateTime())


  class Setup {
    val controller = new VatRegistrationController(mockAuthConnector, mockRegistrationService)
  }

  "GET /" should {

    "return 403 if user not authenticated" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised
      val response: Future[Result] = controller.newVatRegistration(FakeRequest())
      status(response) shouldBe FORBIDDEN
    }

    "return 201 if a new VAT scheme is successfully created" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
      ServiceMocks.mockSuccessfulCreateNewRegistration(testId)
      val response: Future[Result] = controller.newVatRegistration()(FakeRequest())
      status(response) shouldBe CREATED
    }

    "return 503 if RegistrationService encounters any problems" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
      ServiceMocks.mockFailedCreateNewRegistration(testId)
      val response: Future[Result] = controller.newVatRegistration()(FakeRequest())
      status(response) shouldBe SERVICE_UNAVAILABLE
    }

    "call updateVatChoice return CREATED" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
      ServiceMocks.mockSuccessfulUpdateVatChoice(testId, vatChoice)
      val response: Future[Result] = controller.updateVatChoice(testId)(
        FakeRequest().withBody(
          Json.toJson[VatChoice](
            vatChoice
          )))
      await(response) shouldBe Created(Json.toJson(vatChoice))
    }

    "call updateVatChoice return ServiceUnavailable" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
      val exception = new Exception("Exception")
      ServiceMocks.mockServiceUnavailableUpdateVatChoice(testId, vatChoice, exception)
      val response: Future[Result] = controller.updateVatChoice(testId)(
        FakeRequest().withBody(
          Json.toJson[VatChoice](
            vatChoice
          )))
      await(response) shouldBe ServiceUnavailable
    }

  }


}
