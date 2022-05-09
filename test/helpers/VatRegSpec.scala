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

package helpers

import cats.instances.FutureInstances
import config.BackendConfig
import mocks.VatMocks
import org.mockito.Mockito.reset
import org.scalatest.{BeforeAndAfterEach, Inside}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import scala.concurrent.ExecutionContext

trait VatRegSpec extends PlaySpec with Inside with MockitoSugar with VatMocks
  with BeforeAndAfterEach with FutureInstances with GuiceOneAppPerSuite with FutureAssertions {

  val backendConfig: BackendConfig = app.injector.instanceOf[BackendConfig]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  override def beforeEach() {
    reset(mockAuthConnector)
    reset(mockHttpClient)
    reset(mockAuthorisationResource)
    reset(mockRegistrationMongoRepository)
    reset(mockHttpClient)
    reset(mockSubmissionService)
    reset(mockVatRegistrationService)
    reset(mockNonRepudiationConnector)
  }
}

