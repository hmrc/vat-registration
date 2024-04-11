/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.registrations

import enums.VatRegStatus
import itutil.{FakeRegistrationIdService, FakeTimeMachine, IntegrationStubbing}
import models.VatSchemeHeader
import models.api.VatScheme
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, OFormat}
import play.api.test.Helpers._
import services.RegistrationIdService
import utils.TimeMachine

import scala.concurrent.ExecutionContext

class RegistrationControllerISpec extends IntegrationStubbing {

  implicit val fmt: OFormat[VatScheme] = VatScheme.format()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .overrides(bind[RegistrationIdService].to[FakeRegistrationIdService])
    .overrides(bind[TimeMachine].to[FakeTimeMachine])
    .build()

  val registrationsUrl = "/registrations"
  val testApplicationReference = "Application Reference"

  def registrationUrl(regID: String) = s"$registrationsUrl/$regID"

  val testRegId2: String = testRegId + "2"

  val testVatScheme2: VatScheme = testVatScheme.copy(registrationId = testRegId2)
  val testVatSchemeHeader: VatSchemeHeader = VatSchemeHeader(
    registrationId = testVatScheme.registrationId,
    status = testVatScheme.status,
    applicationReference = testVatScheme.applicationReference,
    createdDate = testVatScheme.createdDate,
    requiresAttachments = false
  )
  val testVatSchemeHeader2: VatSchemeHeader = VatSchemeHeader(
    registrationId = testVatScheme2.registrationId,
    status = testVatScheme2.status,
    applicationReference = testVatScheme2.applicationReference,
    createdDate = testVatScheme2.createdDate,
    requiresAttachments = false
  )

  "GET /registrations" when {
    "registrations exist for the user" must {
      "return OK with a list of registrations" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)
        insertIntoDb(testVatScheme2)

        val res = await(client(registrationsUrl).get())

        res.status mustBe OK
        res.json mustBe Json.toJson(Seq(testVatSchemeHeader, testVatSchemeHeader2))
      }
    }
    "registrations do not exist for the user" must {
      "return OK with an empty list" in new SetupHelper {
        given.user.isAuthorised

        val res = await(client(registrationsUrl).get())

        res.status mustBe OK
        res.json mustBe Json.toJson(Nil)
      }
    }
  }

  "POST /registrations" when {
    "the user has no existing registrations" must {
      "return CREATED with the json of the new registration" in new SetupHelper {
        given.user.isAuthorised
        FakeRegistrationIdService.id = testRegId

        val res = await(client(registrationsUrl).post(Json.obj()))

        res.status mustBe CREATED
        res.json mustBe Json.toJson(testVatScheme)
      }
    }
    "the user has existing registrations" must {
      "return CREATED with the json of the new registration without affecting other registrations" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)
        FakeRegistrationIdService.id = testRegId2

        val res = await(client(registrationsUrl).post(Json.obj()))

        res.status mustBe CREATED
        res.json mustBe Json.toJson(testVatScheme2)
        await(repo.getRegistration(testInternalid, testRegId2)) mustBe Some(testVatScheme2)
      }
    }
  }

  "GET /registrations/:regId" when {
    "the registration exists" must {
      "return OK with the registration JSON" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res = await(client(registrationUrl(testRegId)).get())

        res.status mustBe OK
        res.json mustBe Json.toJson(testVatScheme)
      }
      "return OK with the registration JSON when the registration has an optional application reference" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(applicationReference = Some(testApplicationReference)))

        val res = await(client(registrationUrl(testRegId)).get())

        res.status mustBe OK
        res.json mustBe Json.toJson(testVatScheme.copy(applicationReference = Some(testApplicationReference)))
      }
    }
    "the registration donesn't exists" must {
      "return NOT FOUND" in new SetupHelper {
        given.user.isAuthorised

        val res = await(client(registrationUrl(testRegId)).get())

        res.status mustBe NOT_FOUND
      }
    }
  }

  "PUT /registrations/:regID" when {
    "the registration exists" must {
      "update the registration and return OK with the updated json" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val updatedRegistration = testVatScheme.copy(status = VatRegStatus.locked)

        val res = await(client(registrationUrl(testRegId)).put(Json.toJson(updatedRegistration)))

        res.status mustBe OK
        res.json mustBe Json.toJson(updatedRegistration)
      }
    }
    "the registration doesn't exist" must {
      "create a record and return OK with the json" in new SetupHelper {
        given.user.isAuthorised

        val res = await(client(registrationUrl(testRegId)).put(Json.toJson(testVatScheme)))

        res.status mustBe OK
        res.json mustBe Json.toJson(testVatScheme)
      }
    }
    "the JSON is invalid" must {
      "Return BAD_REQUEST" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res = await(client(registrationUrl(testRegId)).put(Json.obj()))

        res.status mustBe BAD_REQUEST
      }
    }
  }

  "DELETE /registrations/:regId" when {
    "the registration exists" must {
      "return NO CONTENT and remove the record" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res = await(client(registrationUrl(testRegId)).delete())

        res.status mustBe NO_CONTENT
        await(repo.getRegistration(testRegId, testRegId)) mustBe None
      }
    }
    "the regustrtion doesn't exist" must {
      "return NO CONTENT" in new SetupHelper {
        given.user.isAuthorised

        val res = await(client(registrationUrl(testRegId)).delete())

        res.status mustBe NO_CONTENT
      }
    }
  }

}
