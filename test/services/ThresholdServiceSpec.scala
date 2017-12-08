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

package services

import java.time.LocalDate

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.Threshold
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.Json
import repositories.{RegistrationMongoRepository, RegistrationRepository}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ThresholdServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service = new ThresholdService (
      registrationMongo = mockRegistrationMongo
    ) {
      override val registrationRepository: RegistrationMongoRepository = mockRegistrationMongoRepository
    }

    def upsertToMongo(): OngoingStubbing[Future[Threshold]] = when(mockRegistrationMongoRepository.updateThreshold(any(),any())(any()))
      .thenReturn(Future.successful(upsertThreshold))

    def upsertToMongoFail(): OngoingStubbing[Future[Threshold]] = when(mockRegistrationMongoRepository.updateThreshold(any(),any())(any()))
      .thenReturn(Future.failed(new Exception("")))

    def getsFromMongo(): OngoingStubbing[Future[Option[Threshold]]] = when(mockRegistrationMongoRepository.getThreshold(any())(any()))
      .thenReturn(Future.successful(Some(validThreshold)))

    def getsNothingFromMongo(): OngoingStubbing[Future[Option[Threshold]]] = when(mockRegistrationMongoRepository.getThreshold(any())(any()))
      .thenReturn(Future.successful(None))
  }

  val validThresholdModel = Json.parse(
    s"""
       |{
       | "mandatoryRegistration": false,
       | "voluntaryReason": "voluntaryReason",
       | "overThresholdDate": "${LocalDate.now()}",
       | "expectedOverThresholdDate": "${LocalDate.now()}"
       |}
    """.stripMargin).as[Threshold]

  val upsertTresholdModel = Json.parse(
    s"""
       |{
       | "mandatoryRegistration": true,
       | "overThresholdDate": "${LocalDate.now()}",
       | "expectedOverThresholdDate": "${LocalDate.now()}"
       |}
    """.stripMargin).as[Threshold]

  "upsertThreshold" should {
    "return the data that is being inputted" in new Setup {
      upsertToMongo()
      val result = await(service.upsertThreshold("regId", upsertTresholdModel)(HeaderCarrier()))
      result shouldBe upsertTresholdModel
    }
    "encounter an exception if an error occurs" in new Setup {
      upsertToMongoFail()
      intercept[Exception](await(service.upsertThreshold("regId", upsertTresholdModel)(HeaderCarrier())))
    }
  }

  "getThreshold" should {
    "return an eligibility if found" in new Setup {
      getsFromMongo()
      val result = await(service.getThreshold("regId")(HeaderCarrier()))
      result shouldBe Some(validThresholdModel)
    }

    "return None if none found matching regId" in new Setup {
      getsNothingFromMongo()
      val result = await(service.getThreshold("regId")(HeaderCarrier()))
      result shouldBe None
    }
  }
}
