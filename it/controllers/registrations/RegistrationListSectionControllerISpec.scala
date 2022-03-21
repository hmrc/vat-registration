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
  }

  "PUT /registrations/:regId/sections/:sectionId/:index" when {
    "the section exists in the registration" must {
      "replace the first existing index with a full model and return OK with the updated JSON" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(otherBusinessInvolvements = Some(List(testOtherBusinessInvolvement))))

        val res = await(client(url(testSectionId, testValidIndex)).put(
          Json.toJson(testOtherBusinessInvolvement.copy(stillTrading = false))
        ))

        res.status mustBe OK
        res.json mustBe Json.toJson(testOtherBusinessInvolvement.copy(stillTrading = false))
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
    }
  }

  "DELETE /registrations/:regId/sections/:sectionId/:index" when {
    "the section index exists in the registration" must {
      "return NO CONTENT" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(otherBusinessInvolvements = Some(List(testOtherBusinessInvolvement))))

        val res = await(client(url(testSectionId, testValidIndex)).delete())

        res.status mustBe NO_CONTENT
        await(repo.retrieveVatScheme(testRegId)) mustBe Some(testVatScheme)
      }
    }
    "multiple section indexes exist in the registration" must {
      "return NO CONTENT" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(otherBusinessInvolvements = Some(List(
          testOtherBusinessInvolvement.copy(stillTrading = false), testOtherBusinessInvolvement
        ))))

        val res = await(client(url(testSectionId, testValidIndex)).delete())

        res.status mustBe NO_CONTENT
        await(repo.retrieveVatScheme(testRegId)) mustBe Some(testVatScheme.copy(otherBusinessInvolvements = Some(List(testOtherBusinessInvolvement))))
      }
    }
    "the section doesn't exist in the registration" must {
      "return NO CONTENT" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res = await(client(url(testSectionId, testValidIndex)).delete())

        res.status mustBe NO_CONTENT
        await(repo.retrieveVatScheme(testRegId)) mustBe Some(testVatScheme)
      }
    }
  }

}
