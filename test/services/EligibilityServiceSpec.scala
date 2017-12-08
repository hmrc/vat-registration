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

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.Eligibility
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.Json
import repositories.{RegistrationMongoRepository, RegistrationRepository}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class EligibilityServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service = new EligibilityService (
      registrationMongo = mockRegistrationMongo
    ) {
      override val registrationRepository: RegistrationMongoRepository = mockRegistrationMongoRepository
    }

    def upsertToMongo(): OngoingStubbing[Future[Eligibility]] = when(mockRegistrationMongoRepository.updateEligibility(any(),any())(any()))
      .thenReturn(Future.successful(upsertEligibility))

    def upsertToMongoFail(): OngoingStubbing[Future[Eligibility]] = when(mockRegistrationMongoRepository.updateEligibility(any(),any())(any()))
      .thenReturn(Future.failed(new Exception("")))

    def getsFromMongo(): OngoingStubbing[Future[Option[Eligibility]]] = when(mockRegistrationMongoRepository.getEligibility(any())(any()))
      .thenReturn(Future.successful(Some(validEligibility)))

    def getsNothingFromMongo(): OngoingStubbing[Future[Option[Eligibility]]] = when(mockRegistrationMongoRepository.getEligibility(any())(any()))
      .thenReturn(Future.successful(None))
  }

  val upsertEligibilityModel = Json.parse(
    """
      |{
      | "version": 1,
      | "result": "thisIsAnUpsert"
      |}
    """.stripMargin).as[Eligibility]

  val validEligibilityModel = Json.parse(
    """
      |{
      | "version": 1,
      | "result": "thisIsAValidReason"
      |}
    """.stripMargin).as[Eligibility]

  "upsertEligibility" should {
    "return the data that is being inputted" in new Setup {
      upsertToMongo()
      val result = await(service.upsertEligibility("regId", upsertEligibilityModel)(HeaderCarrier()))
      result shouldBe upsertEligibilityModel
    }

    "encounter an exception if an error occurs" in new Setup {
      upsertToMongoFail()
      intercept[Exception](await(service.upsertEligibility("regId", upsertEligibilityModel)(HeaderCarrier())))
    }
  }
  "getEligibility" should {
    "return an eligibility if found" in new Setup {
      getsFromMongo()
      val result = await(service.getEligibility("regId")(HeaderCarrier()))
      result shouldBe Some(validEligibilityModel)
    }

    "return None if none found matching regId" in new Setup {
      getsNothingFromMongo()
      val result = await(service.getEligibility("regId")(HeaderCarrier()))
      result shouldBe None
    }
  }
}
