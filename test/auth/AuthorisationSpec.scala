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

package auth

import helpers.VatRegSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.mvc.Results
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AuthorisationSpec extends VatRegSpec {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val authorisation: Authorisation = new Authorisation {
    implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    val authConnector: AuthConnector = mockAuthConnector
  }

  val regId = "xxx"
  val testInternalId = "foo"

  "isAuthenticated" should {
    "provided a logged in auth result when there is a valid bearer token" in {
      AuthorisationMocks.mockAuthenticated(testInternalId)

      val result = authorisation.isAuthenticated {
        _ => Future.successful(Results.Ok)
      }
      val response = await(result)
      response.header.status mustBe OK
    }

    "indicate there's no logged in user where there isn't a valid bearer token" in {
      AuthorisationMocks.mockNotLoggedInOrAuthenticated()

      val result = authorisation.isAuthenticated {
        _ => Future.successful(Results.Ok)
      }

      val response = await(result)
      response.header.status mustBe FORBIDDEN
    }
    "throw an exception if an exception occurs whilst calling auth" in {
      when(mockAuthConnector.authorise[Option[String]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(),
        ArgumentMatchers.any())).thenReturn(Future.failed(new Exception))

      val result = authorisation.isAuthenticated {
        _ => Future.successful(Results.Ok)
      }
      an[Exception] mustBe thrownBy(await(result))
    }
  }
}
