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

package models.api

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import play.api.libs.json.{JsString, JsSuccess, Json}

class AttachmentTypeSpec extends VatRegSpec with VatRegistrationFixture {

  "AttachmentType format" must {
    AttachmentType.map.keySet.foreach { attachmentType =>
      val attachmentTypeString = AttachmentType.map(attachmentType)
      s"parse the $attachmentType object into '$attachmentTypeString' and back" in {
        Json.toJson[AttachmentType](attachmentType) mustBe JsString(attachmentTypeString)
        Json.fromJson[AttachmentType](JsString(attachmentTypeString)) mustBe JsSuccess(attachmentType)
      }
    }
  }

  "AttachmentType submissionWrites" must {
    "parse a list of AttachmentTypes to the correct Json" in {
      Json.toJson[List[AttachmentType]](List(LetterOfAuthority, VAT51))(AttachmentType.submissionWrites(Post)) mustBe Json.obj(
        "letterOfAuthority" -> "3",
        "VAT51" -> "3"
      )
    }
  }
}