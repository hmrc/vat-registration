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

package auth

import connectors.{AuthConnector, Authority, UserIds}
import helpers.VatRegSpec
import org.mockito.Matchers
import org.mockito.Mockito._
import org.scalatest._
import play.api.mvc.Results
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class AuthenticationSpec extends VatRegSpec with BeforeAndAfter {

  implicit val hc = HeaderCarrier()

  val mockAuth = mock[AuthConnector]

  object Authenticated extends Authenticated {
    val auth = mockAuth
  }

  before {
    reset(mockAuth)
  }

  "The authentication helper" should {

    "provided a logged in auth result when there is a valid bearer token" in {

      val a = Authority("x", "y", "z", UserIds("tiid", "teid"))

      when(mockAuth.getCurrentAuthority()(Matchers.any())).thenReturn(Future.successful(Some(a)))

      status(Authenticated.authenticated { authResult => {
        authResult shouldBe LoggedIn(a)
        Future.successful(Results.Ok)
      }
      }) shouldBe OK
    }

    "indicate there's no logged in user where there isn't a valid bearer token" in {

      when(mockAuth.getCurrentAuthority()(Matchers.any())).
        thenReturn(Future.successful(None))

      status(Authenticated.authenticated { authResult => {
        authResult shouldBe NotLoggedIn
        Future.successful(Results.Forbidden)
      }
      }) shouldBe FORBIDDEN
    }
  }
}
