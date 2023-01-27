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

package controllers.registrations

import itutil.IntegrationStubbing
import models.registration.OtherBusinessInvolvementsSectionId
import play.api.libs.json.Json
import play.api.test.Helpers._

class RegistrationListSectionControllerISpec extends IntegrationStubbing {

  def url(section: String, index: Int) = s"/registrations/$testRegId/sections/$section/$index"

  val testSectionId: String = OtherBusinessInvolvementsSectionId.key
  val testValidIndex = 1
  val testValidIndex2 = 2
  val testInvalidIndex = 99

  "GET /registrations/:regId/sections/:sectionId/:index" when {
    "the section exists in the registration and the index is 1" must {
      "return OK with the json for the first section" in new SetupHelper {
        given.user.isAuthorised

        insertIntoDb(testVatScheme.copy(otherBusinessInvolvements = Some(List(testOtherBusinessInvolvement))))

        val res = await(client(url(testSectionId, testValidIndex)).get())

        res.status mustBe OK
        res.json mustBe Json.toJson(testOtherBusinessInvolvement)
      }
    }
    "the section doesn't exist in the registration" must {
      "return NOT FOUND" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res = await(client(url(testSectionId, testValidIndex)).get())

        res.status mustBe NOT_FOUND
      }
    }
    "the index in the call is out of bounds for section" must {
      "return OK with the json for the first section" in new SetupHelper {
        given.user.isAuthorised

        insertIntoDb(testVatScheme.copy(otherBusinessInvolvements = Some(List(testOtherBusinessInvolvement))))

        val res = await(client(url(testSectionId, testInvalidIndex)).get())

        res.status mustBe BAD_REQUEST
        res.body mustBe s"[RegistrationListSectionController] Index out of bounds for ${OtherBusinessInvolvementsSectionId.toString}"
      }
    }
  }

  "PUT /registrations/:regId/sections/:sectionId/:index" when {
    "the section exists in the registration" must {
      "replace the first existing index with a full model and return OK with the updated JSON" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(otherBusinessInvolvements = Some(List(testOtherBusinessInvolvement))))

        val res = await(client(url(testSectionId, testValidIndex)).put(
          Json.toJson(testOtherBusinessInvolvement.copy(stillTrading = Some(false)))
        ))

        res.status mustBe OK
        res.json mustBe Json.toJson(testOtherBusinessInvolvement.copy(stillTrading = Some(false)))
      }
      "add a new index to the section using a full model and return OK with the JSON" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(otherBusinessInvolvements = Some(List(testOtherBusinessInvolvement))))

        val res = await(client(url(testSectionId, testValidIndex2)).put(
          Json.toJson(testOtherBusinessInvolvement)
        ))

        res.status mustBe OK
        res.json mustBe Json.toJson(testOtherBusinessInvolvement)
      }
    }
    "the section doesn't exist in the registration" must {
      "return OK with JSON for the new section" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res = await(client(url(testSectionId, testValidIndex)).put(Json.toJson(testOtherBusinessInvolvement)))

        res.status mustBe OK
        res.json mustBe Json.toJson(testOtherBusinessInvolvement)
      }

      "return BAD_REQUEST with JSON for the new section for an invalid answer" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res = await(client(url(testSectionId, testValidIndex)).put(Json.obj(
          "hasVrn" -> "not a boolean"
        )))

        res.status mustBe BAD_REQUEST
      }
    }
    "the registration doesn't exist" must {
      "return an exception" in new SetupHelper {
        given.user.isAuthorised

        val res = await(client(url(testSectionId, testValidIndex)).put(Json.toJson(testOtherBusinessInvolvement)))

        res.status mustBe INTERNAL_SERVER_ERROR
        res.body mustBe s"[RegistrationListSectionController] Unable to upsert section '${OtherBusinessInvolvementsSectionId.key}' for regId '$testRegId'"
      }
    }
  }

  "DELETE /registrations/:regId/sections/:sectionId/:index" when {
    "the section index exists in the registration" must {
      "return NO CONTENT" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(otherBusinessInvolvements = Some(List(testOtherBusinessInvolvement))))

        val res = await(client(url(testSectionId, testValidIndex)).delete())

        res.status mustBe NO_CONTENT
        await(repo.getRegistration(testInternalid, testRegId)) mustBe Some(testVatScheme)
      }
    }
    "multiple section indexes exist in the registration" must {
      "return NO CONTENT" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(otherBusinessInvolvements = Some(List(
          testOtherBusinessInvolvement.copy(stillTrading = Some(false)), testOtherBusinessInvolvement
        ))))

        val res = await(client(url(testSectionId, testValidIndex)).delete())

        res.status mustBe NO_CONTENT
        await(repo.getRegistration(testInternalid, testRegId)) mustBe Some(testVatScheme.copy(otherBusinessInvolvements = Some(List(testOtherBusinessInvolvement))))
      }
    }
    "the section doesn't exist in the registration" must {
      "return NO CONTENT" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res = await(client(url(testSectionId, testValidIndex)).delete())

        res.status mustBe NO_CONTENT
        await(repo.getRegistration(testInternalid, testRegId)) mustBe Some(testVatScheme)
      }
    }
  }

}
