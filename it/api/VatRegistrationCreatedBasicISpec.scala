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

package api

import connectors.stubs.BusinessRegConnectorStub._
import connectors.stubs.VatSubmissionStub._
import enums.VatRegStatus
import featureswitch.core.config.{FeatureSwitching, StubSubmission}
import itutil.IntegrationStubbing
import models.api.VatScheme
import models.external.CurrentProfile
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class VatRegistrationCreatedBasicISpec extends IntegrationStubbing with FeatureSwitching {

  class Setup extends SetupHelper

  val validBusinessRegistrationResponse: CurrentProfile = CurrentProfile(
    "12345",
    Some("director"),
    "ENG"
  )

  val testSubmissionResponse = Json.obj("formBundle" -> "123412341234")

  "/vatreg/new (create registration)" when {
    "the user is authorised" should {
      "Return CREATED" in new Setup {
        given.user.isAuthorised

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.newVatRegistration().url).post("test"))

        res.status mustBe CREATED
      }
      "Return NOT FOUND if the registration is missing" in new Setup {
        given.user.isAuthorised

        val res: WSResponse = await(client(s"/12345").post("test"))

        res.status mustBe NOT_FOUND
      }
    }
    "the user is not authorised" should {
      "Return FORBIDDEN" in new Setup {
        given.user.isNotAuthorised
        stubBusinessReg(OK)(Some(validBusinessRegistrationResponse))

        val res: WSResponse = await(client(controllers.routes.VatRegistrationController.newVatRegistration().url).post("test"))

        res.status mustBe FORBIDDEN
      }
    }
  }

  "/vatreg/:regId/submit-registration (submit registration)" should {
    "return an Ok if the submission is successful for the regID" in new Setup() {
      disable(StubSubmission)
      given.user.isAuthorised

      stubVatSubmission(OK)(testSubmissionResponse)

      await(repo.insert(testFullVatSchemeWithUnregisteredBusinessPartner))

      val result: WSResponse = await(client(
        controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url).put(Json.obj())
      )

      result.status mustBe OK

      val reg: Option[VatScheme] = await(repo.retrieveVatScheme(testRegId))
      reg.get.status mustBe VatRegStatus.submitted

      await(repo.remove("registrationId" -> testRegId))
    }

    "mock the return if the stub submission flag is on" in new Setup {
      enable(StubSubmission)
      given.user.isAuthorised
      stubVatSubmission(OK)(testSubmissionResponse)

      await(repo.insert(testFullVatSchemeWithUnregisteredBusinessPartner))

      val result: WSResponse = await(client(
        controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url).put(Json.obj())
      )

      result.status mustBe OK
    }
  }
}
