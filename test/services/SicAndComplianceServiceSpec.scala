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

package services

import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.SicAndCompliance
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.test.Helpers._

import scala.concurrent.Future

class SicAndComplianceServiceSpec extends VatRegSpec with VatRegistrationFixture  {

  class Setup {
    val service: SicAndComplianceService = new SicAndComplianceService(
      registrationRepository = mockRegistrationMongoRepository
    )
  }
  def getFromMongo(res:Future[Option[SicAndCompliance]]): OngoingStubbing[Future[Option[SicAndCompliance]]] =
    when(mockRegistrationMongoRepository.fetchSicAndCompliance(any()))
    .thenReturn(res)

  def updateMongo(res:Future[SicAndCompliance]): OngoingStubbing[Future[SicAndCompliance]] =
    when(mockRegistrationMongoRepository.updateSicAndCompliance(any(),any()))
    .thenReturn(res)

  "getSicAndCompliance" should {
    "return a SicAndCompliance Model when an entry exists in mongo for the specified regId" in new Setup {
      getFromMongo(Future.successful(Some(testSicAndCompliance)))
      await(service.getSicAndCompliance("fooBarAndWizz")) mustBe Some(testSicAndCompliance)
    }
    "return None when no entry exists in the dataBase for the specified regId" in new Setup {
      getFromMongo(Future.successful(None))
      await(service.getSicAndCompliance("bangBuzzFang")) mustBe None
    }
  }
  "updateSicAndCompliance" should {
    "return an updated SicAndCompliance Model when an update successfully takes place in mongo" in new Setup {
      updateMongo(Future.successful(testSicAndCompliance))
      await(service.updateSicAndCompliance("ImARegId",testSicAndCompliance)) mustBe testSicAndCompliance
    }
    "return a missingRegDocument when no reg Document exists for the reg id when an update takes place" in new Setup {
      updateMongo(Future.failed(MissingRegDocument("testId")))
      intercept[MissingRegDocument](await(
        service.updateSicAndCompliance("testId",testSicAndCompliance))) mustBe MissingRegDocument("testId")
    }
    "return new Exception when an exception is returned from the repo during an update" in new Setup {
      updateMongo(Future.failed(new Exception("foo Bar Wizz Bang")))
      intercept[Exception](await(service.updateSicAndCompliance("testId",testSicAndCompliance)))
    }
  }

}
