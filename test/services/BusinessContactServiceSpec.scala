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
import models.api.BusinessContact
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.test.Helpers._

import scala.concurrent.Future

class BusinessContactServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service: BusinessContactService = new BusinessContactService(
      registrationRepository = mockRegistrationMongoRepository
    )
  }

  def getFromMongo(res: Future[Option[BusinessContact]]): OngoingStubbing[Future[Option[BusinessContact]]] =
    when(mockRegistrationMongoRepository.fetchBusinessContact(any())).thenReturn(res)

  def updateMongo(res: Future[BusinessContact]): OngoingStubbing[Future[BusinessContact]] =
    when(mockRegistrationMongoRepository.updateBusinessContact(any(), any())).thenReturn(res)

  "getBusinessContact" should {
    "return a BusinessContact Model when an entry exists in mongo for the specified regId" in new Setup {
      getFromMongo(Future.successful(testBusinessContact))
      await(service.getBusinessContact("fooBarAndWizz")) mustBe testBusinessContact
    }
    "return None when no entry exists in the dataBase for the specified regId" in new Setup {
      getFromMongo(Future.successful(None))
      await(service.getBusinessContact("bangBuzzFang")) mustBe None
    }
  }
  "updateBusinessContact" should {
    "return an updated BusinessContact Model when an update successfully takes place in mongo" in new Setup {
      updateMongo(Future.successful(testBusinessContact.get))
      await(service.updateBusinessContact("ImARegId",testBusinessContact.get)) mustBe testBusinessContact.get
    }
    "return a missingRegDocument when no reg Document exists for the reg id when an update takes place" in new Setup {
      updateMongo(Future.failed(MissingRegDocument(testRegId)))
      intercept[MissingRegDocument](await(service.updateBusinessContact(testRegId, testBusinessContact.get))) mustBe MissingRegDocument(testRegId)
    }
    "return new Exception when an exception is returned from the repo during an update" in new Setup {
      updateMongo(Future.failed(new Exception("foo Bar Wizz Bang")))
      intercept[Exception](await(service.updateBusinessContact(testRegId, testBusinessContact.get)))
    }
  }
}
