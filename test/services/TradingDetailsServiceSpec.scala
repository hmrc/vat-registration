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


import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.TradingDetails
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.Helpers._
import repositories.VatSchemeRepository

import scala.concurrent.Future

class TradingDetailsServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service: TradingDetailsService = new TradingDetailsService(
      registrationRepository = mockRegistrationMongoRepository
    ) {
      override val registrationRepository: VatSchemeRepository = mockRegistrationMongoRepository
    }
  }

  "fetchTradingDetails" should {
    "return an Trading Details if found" in new Setup {
      when(mockRegistrationMongoRepository.retrieveTradingDetails(any()))
        .thenReturn(Future.successful(Some(validFullTradingDetails)))

      val result: Option[TradingDetails] = await(service.retrieveTradingDetails(testRegId))

      result mustBe Some(validFullTradingDetails)
    }

    "return None if none found matching regId" in new Setup {
      when(mockRegistrationMongoRepository.retrieveTradingDetails(any()))
        .thenReturn(Future.successful(None))

      val result: Option[TradingDetails] = await(service.retrieveTradingDetails(testRegId))

      result mustBe None
    }
  }

  "updateTradingDetails" should {
    "return the data that is being input" in new Setup {
      when(mockRegistrationMongoRepository.updateTradingDetails(any(),any()))
        .thenReturn(Future.successful(validFullTradingDetails))

      val result: TradingDetails = await(service.updateTradingDetails(testRegId, validFullTradingDetails))

      result mustBe validFullTradingDetails
    }

    "encounter an exception if an error occurs" in new Setup {
      when(mockRegistrationMongoRepository.updateTradingDetails(any(),any()))
        .thenReturn(Future.failed(new Exception))

      intercept[Exception](await(service.updateTradingDetails(testRegId, validFullTradingDetails)))
    }

    "encounter a MissingRegDocument if no document is found" in new Setup {
      when(mockRegistrationMongoRepository.updateTradingDetails(any(), any()))
        .thenReturn(Future.failed(MissingRegDocument(testRegId)))

      intercept[MissingRegDocument](await(service.updateTradingDetails(testRegId, validFullTradingDetails)))
    }
  }

}
