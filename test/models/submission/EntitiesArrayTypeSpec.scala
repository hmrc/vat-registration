/*
 * Copyright 2023 HM Revenue & Customs
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

package models.submission

import helpers.VatRegSpec
import play.api.libs.json.Json

class EntitiesArrayTypeSpec extends VatRegSpec {

  val fieldName = "entityType"

  "reads" must {
    "parse 1 to GroupMemberEntity" in {
      val testJson = Json.obj(fieldName -> "1")
      val res      = (testJson \ fieldName).validate[EntitiesArrayType].asOpt

      res mustBe Some(GroupMemberEntity)
    }
    "parse 2 to GroupRepMemberEntity" in {
      val testJson = Json.obj(fieldName -> "2")
      val res      = (testJson \ fieldName).validate[EntitiesArrayType].asOpt

      res mustBe Some(GroupRepMemberEntity)
    }
    "parse 3 to PartnerEntity" in {
      val testJson = Json.obj(fieldName -> "3")
      val res      = (testJson \ fieldName).validate[EntitiesArrayType].asOpt

      res mustBe Some(PartnerEntity)
    }
  }

  "writes" must {
    "write GroupMemberEntity to 1" in {
      val testObj = Json.obj(fieldName -> Json.toJson[EntitiesArrayType](GroupMemberEntity))
      Json.toJson(testObj) mustBe Json.obj(fieldName -> "1")
    }
    "write GroupRepMemberEntity to 2" in {
      val testObj = Json.obj(fieldName -> Json.toJson[EntitiesArrayType](GroupRepMemberEntity))
      Json.toJson(testObj) mustBe Json.obj(fieldName -> "2")
    }
    "write PartnerEntity to 3" in {
      val testObj = Json.obj(fieldName -> Json.toJson[EntitiesArrayType](PartnerEntity))
      Json.toJson(testObj) mustBe Json.obj(fieldName -> "3")
    }
  }

}
