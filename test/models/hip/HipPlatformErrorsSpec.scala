/*
 * Copyright 2026 HM Revenue & Customs
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

package models.hip

import models.hip.HipPlatformErrors._
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, Json}

class HipPlatformErrorsSpec extends PlaySpec {

  "HipErrorWrapper reads" should {
    "read a HoD system error envelope" in {
      val json = Json.parse(
        """{
          |  "origin": "HoD",
          |  "response": {
          |    "error": {
          |      "code": "500",
          |      "logID": "D82EBAB67AC6D7565C0682CA91BDC577",
          |      "message": "string"
          |    }
          |  }
          |}""".stripMargin
      )

      json.asOpt[HipErrorWrapper] mustBe Some(
        HipErrorWrapper(
          Origin.HoD,
          HipSystemErrorObject(HipError("500", "D82EBAB67AC6D7565C0682CA91BDC577", "string"))
        )
      )
    }

    "read a HIP failures envelope" in {
      val json = Json.parse(
        """{
          |  "origin": "HIP",
          |  "response": {
          |    "failures": [
          |      { "type": "Type of Failure", "reason": "Reason for Failure" }
          |    ]
          |  }
          |}""".stripMargin
      )

      val result = json.validate[HipErrorWrapper].get
      result.origin mustBe Origin.HIP
      result.response match {
        case HipFailuresErrorArray(failures) => failures.toSeq mustBe Seq(HipFailure("Type of Failure", "Reason for Failure"))
        case other                           => fail(s"Expected HipFailuresErrorArray, got $other")
      }
    }

    "fail on an unknown origin" in {
      Json.parse("""{"origin":"Other","response":{"failures":[]}}""").validate[HipErrorWrapper] mustBe a[JsError]
    }

    "fail on an unrecognised response" in {
      Json.parse("""{"origin":"HIP","response":{"something":"else"}}""").validate[HipErrorWrapper] mustBe a[JsError]
    }
  }

  "Hip422Error reads" should {
    "read the schema example" in {
      val json = Json.parse(
        """{
          |  "error": {
          |    "code": "003",
          |    "processingDate": "2026-03-09T12:34:46Z",
          |    "text": "Request could not be processed"
          |  }
          |}""".stripMargin
      )

      json.asOpt[Hip422Error] mustBe Some(
        Hip422Error(HipInner422Err("003", "2026-03-09T12:34:46Z", "Request could not be processed"))
      )
    }
  }

}
