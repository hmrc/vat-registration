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
package itutil

import play.api.test.Helpers.OK

trait IntegrationStubbing extends IntegrationSpecBase {

  class PreconditionBuilder {
    implicit val builder: PreconditionBuilder = this

    def user: User = User()
  }

  def given: PreconditionBuilder = new PreconditionBuilder

  case class User()(implicit builder: PreconditionBuilder) {
    def isAuthorised: PreconditionBuilder = {
      stubPost("/write/audit", OK, """{"x":2}""")
      stubPost("/write/audit/merged", OK, """{"x":2}""")
      stubGet("/auth/authority", OK, """{"uri":"xxx","credentials":{"gatewayId":"xxx2"},"userDetailsLink":"xxx3","ids":"/auth/ids"}""")
      stubGet("/auth/ids", OK, """{"internalId":"Int-xxx","externalId":"Ext-xxx"}""")
      builder
    }

    def isNotAuthorised: PreconditionBuilder = {
      stubGet("/auth/authority", 403, "")
      stubPost("/write/audit/merged", OK, """{"x":2}""")
      builder
    }
  }
}
