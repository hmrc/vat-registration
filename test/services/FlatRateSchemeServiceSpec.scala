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
import models.api.FlatRateScheme
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.Helpers._

import scala.concurrent.Future

class FlatRateSchemeServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service: FlatRateSchemeService = new FlatRateSchemeService(
      registrationRepository = mockRegistrationMongoRepository
    )
  }

  "retrieveFlatRateScheme" should {
    "return a Flat Rate Scheme if found" in new Setup {
      when(mockRegistrationMongoRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(Some(validFullFlatRateScheme)))

      val result: Option[FlatRateScheme] = await(service.retrieveFlatRateScheme(testRegId))

      result mustBe Some(validFullFlatRateScheme)
    }

    "return None if none found matching regId" in new Setup {
      when(mockRegistrationMongoRepository.fetchFlatRateScheme(any()))
        .thenReturn(Future.successful(None))

      val result: Option[FlatRateScheme] = await(service.retrieveFlatRateScheme(testRegId))

      result mustBe None
    }
  }

  "updateFlatRateScheme" should {
    "return the data that is being input" in new Setup {
      when(mockRegistrationMongoRepository.updateFlatRateScheme(any(),any()))
        .thenReturn(Future.successful(validFullFlatRateScheme))

      val result: FlatRateScheme = await(service.updateFlatRateScheme(testRegId, validFullFlatRateScheme))

      result mustBe validFullFlatRateScheme
    }

    "encounter an exception if an error occurs" in new Setup {
      when(mockRegistrationMongoRepository.updateFlatRateScheme(any(),any()))
        .thenReturn(Future.failed(new Exception))

      intercept[Exception](await(service.updateFlatRateScheme(testRegId, validFullFlatRateScheme)))
    }

    "encounter a MissingRegDocument if no document is found" in new Setup {
      when(mockRegistrationMongoRepository.updateFlatRateScheme(any(), any()))
        .thenReturn(Future.failed(MissingRegDocument(testRegId)))

      intercept[MissingRegDocument](await(service.updateFlatRateScheme(testRegId, validFullFlatRateScheme)))
    }
  }

  "removeFlatRateScheme" should {
    "return true when the block has been removed" in new Setup {
      when(mockRegistrationMongoRepository.removeFlatRateScheme(any()))
        .thenReturn(Future.successful(true))

      val result: Boolean = await(service.removeFlatRateScheme(testRegId))

      result mustBe true
    }

    "encounter an exception if an error occurs" in new Setup {
      when(mockRegistrationMongoRepository.removeFlatRateScheme(any()))
        .thenReturn(Future.failed(new Exception))

      intercept[Exception](await(service.removeFlatRateScheme(testRegId)))
    }

    "encounter a MissingRegDocument if no document is found" in new Setup {
      when(mockRegistrationMongoRepository.removeFlatRateScheme(any()))
        .thenReturn(Future.failed(MissingRegDocument(testRegId)))

      intercept[MissingRegDocument](await(service.removeFlatRateScheme(testRegId)))
    }
  }
}