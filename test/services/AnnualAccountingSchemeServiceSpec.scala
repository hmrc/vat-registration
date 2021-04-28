/*
 * Copyright 2021 HM Revenue & Customs
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
import models.api.AnnualAccountingScheme
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.Helpers._

import scala.concurrent.Future

class AnnualAccountingSchemeServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service: AnnualAccountingService = new AnnualAccountingService(
      registrationRepository = mockRegistrationMongoRepository
    )
  }

  "getAnnualAccountingScheme" should {
    "return a Annual Accounting Scheme if found" in new Setup {
      when(mockRegistrationMongoRepository.fetchAnnualAccountingScheme(any()))
        .thenReturn(Future.successful(Some(validFullAAS)))

      val result: Option[AnnualAccountingScheme] = await(service.retrieveAnnualAccountingScheme(testRegId))

      result mustBe Some(validFullAAS)
    }

    "return None if none found matching regId" in new Setup {
      when(mockRegistrationMongoRepository.fetchAnnualAccountingScheme(any()))
        .thenReturn(Future.successful(None))

      val result: Option[AnnualAccountingScheme] = await(service.retrieveAnnualAccountingScheme(testRegId))

      result mustBe None
    }
  }

  "updateAnnualAccountingScheme" should {
    "return the data that is being input" in new Setup {
      when(mockRegistrationMongoRepository.updateAnnualAccountingScheme(any(), any()))
        .thenReturn(Future.successful(validFullAAS))

      val result: AnnualAccountingScheme = await(service.updateAnnualAccountingScheme(testRegId, validFullAAS))

      result mustBe validFullAAS
    }

    "encounter an exception if an error occurs" in new Setup {
      when(mockRegistrationMongoRepository.updateAnnualAccountingScheme(any(), any()))
        .thenReturn(Future.failed(new Exception))

      intercept[Exception](await(service.updateAnnualAccountingScheme(testRegId, validFullAAS)))
    }
  }
}