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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlMatching}
import models.api.VatScheme
import play.api.test.Helpers.OK
import reactivemongo.api.commands.WriteResult
import uk.gov.hmrc.auth.core.AuthenticateHeaderParser

import scala.concurrent.Future

trait IntegrationStubbing extends IntegrationSpecBase {

  class PreconditionBuilder {
    implicit val builder: PreconditionBuilder = this

    def user: User = User()
    def regRepo:RegRepo = RegRepo()
  }

  def given: PreconditionBuilder = new PreconditionBuilder

  case class RegRepo()(implicit builder: PreconditionBuilder) {
    def insertIntoDb(v:VatScheme,f:VatScheme => Future[WriteResult]): PreconditionBuilder = {
     await(f(v))
      builder
    }
  }
  case class User()(implicit builder: PreconditionBuilder) {
    val authoriseData =
      s"""{
         | "internalId": "Int-xxx",
         | "externalId": "Ext-xxx",
         | "credentials": {
         |   "providerId": "xxx2",
         |   "providerType": "some-provider-type"
         | }
         |}""".stripMargin

    def isAuthorised: PreconditionBuilder = {
      stubPost("/write/audit", OK, """{"x":2}""")
      stubPost("/write/audit/merged", OK, """{"x":2}""")
      stubPost("/auth/authorise", OK, authoriseData)
      builder
    }
// def exceptionHeaders(value: String) = Map(AuthenticateHeaderParser.WWW_AUTHENTICATE -> s"""MDTP detail="$value"""")
    def isNotAuthorised: PreconditionBuilder = {
      stubFor(post(urlMatching("/auth/authorise")).willReturn(aResponse().withStatus(401).withBody("""{"internalId": "Int-xxx"}""").withHeader(AuthenticateHeaderParser.WWW_AUTHENTICATE,s"""MDTP detail="InvalidBearerToken"""")))

      stubPost("/write/audit", OK, """{"x":2}""")
      stubPost("/write/audit/merged", OK, """{"x":2}""")
      builder
    }
  }
}
