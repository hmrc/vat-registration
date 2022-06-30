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

package models.api

import fixtures.VatRegistrationFixture
import helpers.BaseSpec
import play.api.libs.json.{JsObject, JsSuccess, Json}

class BusinessSpec extends BaseSpec with VatRegistrationFixture {

  lazy val businessJson: JsObject = Json.obj(
    "ppobAddress" -> Json.obj(
      "line1" -> testAddress.line1,
      "line2" -> testAddress.line2,
      "postcode" -> testAddress.postcode,
      "country" -> Json.obj(
        "code" -> testAddress.country.flatMap(_.code),
        "name" -> testAddress.country.flatMap(_.name)
      ),
      "addressValidated" -> true
    ),
    "email" -> testEmail,
    "telephoneNumber" -> testTelephone,
    "hasWebsite" -> true,
    "website" -> testWebsite,
    "contactPreference" -> ContactPreference.email,
    "hasLandAndProperty" -> false,
    "businessDescription" -> testBusinessDescription,
    "businessActivities" -> Json.arr(
      Json.obj(
        "code" -> testSicCode1,
        "desc" -> testSicDesc1,
        "indexes" -> testSicDisplay1
      ), Json.obj(
        "code" -> testSicCode2,
        "desc" -> testSicDesc2,
        "indexes" -> testSicDisplay2
      ), Json.obj(
        "code" -> testSicCode3,
        "desc" -> testSicDesc3,
        "indexes" -> testSicDisplay3
      )
    ),
    "mainBusinessActivity" -> Json.obj(
      "code" -> testSicCode1,
      "desc" -> testSicDesc1,
      "indexes" -> testSicDisplay1
    ),
    "labourCompliance" -> Json.obj(
      "numOfWorkersSupplied" -> 1000,
      "supplyWorkers" -> true
    ),
    "otherBusinessInvolvement" -> false
  )

  "format" must {
    "read the json back into a model" in {
      Business.format.reads(businessJson) mustBe JsSuccess(testBusiness)
    }

    "write the model into a json" in {
      Business.format.writes(testBusiness) mustBe businessJson
    }
  }

  "tempReads" must {
    "read the VatScheme json into a model" in {
      Business.tempReads.reads(Json.toJson(testFullVatScheme.copy(business = None))(VatScheme.writes())) mustBe JsSuccess(Business(
        email = testBusinessContact.email,
        telephoneNumber = testBusinessContact.telephoneNumber,
        website = testBusinessContact.website,
        ppobAddress = Some(testBusinessContact.ppob),
        contactPreference = Some(testBusinessContact.commsPreference),
        hasWebsite = testBusinessContact.hasWebsite,
        businessDescription = Some(testSicAndCompliance.businessDescription),
        labourCompliance = testSicAndCompliance.labourCompliance,
        mainBusinessActivity = Some(testSicAndCompliance.mainBusinessActivity),
        businessActivities = Some(testSicAndCompliance.businessActivities),
        otherBusinessInvolvement = None,
        hasLandAndProperty = None
      ))
    }
  }
}
