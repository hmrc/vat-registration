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

package services

import helpers.VatRegSpec
import models.api.schemas.ApiSchema
import play.api.libs.json.Json

class SchemaValidationServiceSpec extends VatRegSpec {

  val unknownErrorsKey    = "unknownErrors"
  val suppressedErrorsKey = "suppressedErrors"

  object Service extends SchemaValidationService

  "validate" when {
    "there are no errors" must {
      "return an empty map" in {
        object FakeSchema extends ApiSchema("/test-schema.yaml", "request") {
          val suppressedErrors = Nil
        }

        val validBody = Json.obj(
          "name"   -> "Test",
          "date"   -> "2023-01-16",
          "hasCar" -> true
        )

        val result = Service.validate(FakeSchema, validBody.toString())

        result mustBe Map()
      }
    }
    "there are errors" when {
      "the schema has no suppressed errors" must {
        "return a map with all errors under the 'unknownErrors' key" in {
          object FakeSchema extends ApiSchema("/test-schema.yaml", "request") {
            val suppressedErrors = Nil
          }

          val invalidBody = Json.obj(
            "name"   -> "Test",
            "date"   -> "ABCD-EF-GH",
            "hasCar" -> 1
          )

          val result = Service.validate(FakeSchema, invalidBody.toString())

          result mustBe Map(unknownErrorsKey -> Seq("/date", "/hasCar"))
        }
      }
      "the schema has suppressed errors"    must {
        "return a map with the suppressed errors under the 'suppressedErrors' key and the rest under the 'unknownErrors' key" in {
          object FakeSchema extends ApiSchema("/test-schema.yaml", "request") {
            val suppressedErrors = List("/hasCar")
          }

          val invalidBody = Json.obj(
            "name"   -> "Test",
            "date"   -> "ABCD-EF-GH",
            "hasCar" -> 1
          )

          val result = Service.validate(FakeSchema, invalidBody.toString())

          result mustBe Map(
            unknownErrorsKey    -> Seq("/date"),
            suppressedErrorsKey -> Seq("/hasCar")
          )
        }
        "return a map with the suppressed errors (with regex) under the 'suppressedErrors' key and the rest under the 'unknownErrors' key" in {
          object FakeSchema extends ApiSchema("/test-schema.yaml", "request") {
            val suppressedErrors = List("^(.*)Car$")
          }

          val invalidBody = Json.obj(
            "name"   -> "Test",
            "date"   -> "ABCD-EF-GH",
            "hasCar" -> 1
          )

          val result = Service.validate(FakeSchema, invalidBody.toString())

          result mustBe Map(
            unknownErrorsKey    -> Seq("/date"),
            suppressedErrorsKey -> Seq("/hasCar")
          )
        }
        "return a map with multiple suppressed errors under the 'suppressedErrors' key and the rest under the 'unknownErrors' key" in {
          object FakeSchema extends ApiSchema("/test-schema.yaml", "request") {
            val suppressedErrors = List("/date", "/hasCar")
          }

          val invalidBody = Json.obj(
            "name"   -> true,
            "date"   -> "ABCD-EF-GH",
            "hasCar" -> 1
          )

          val result = Service.validate(FakeSchema, invalidBody.toString())

          result mustBe Map(
            unknownErrorsKey    -> Seq("/name"),
            suppressedErrorsKey -> Seq("/date", "/hasCar")
          )
        }
      }
    }
  }

}
