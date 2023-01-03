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

package controllers

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.VatThreshold
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.concurrent.Future

class VatThresholdControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import play.api.test.Helpers._

  class SetupMocks(mockSubmission: Boolean) {
    val controller: VatThresholdController = new VatThresholdController(mockVatThresholdService, stubControllerComponents()) {

    }
  }

  class Setup extends SetupMocks(false)
  class SetupWithMockSubmission extends SetupMocks(true)

  def date(s: String): LocalDate = LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd"))


  "getThresholdForTime" should {
    "returns the correct threshold and since date" in new Setup {
      AuthorisationMocks.mockAuthenticatedLoggedInNoCorrespondingData()
      val returnObj: JsObject = Json.obj("taxable-threshold" -> "73000", "since" -> "2011-04-01")
      when(mockVatThresholdService.getThresholdForGivenDate(any())).thenReturn(Some(VatThreshold(date("2011-04-01"), "73000")))
      val result: Future[Result] = controller.getThresholdForDate("2012-03-20")(FakeRequest())
      status(result) mustBe OK
      contentAsJson(result) mustBe returnObj
    }

    "return 404 if requested date is before any known thresholds" in new Setup {
      AuthorisationMocks.mockAuthenticatedLoggedInNoCorrespondingData()
      when(mockVatThresholdService.getThresholdForGivenDate(any())).thenReturn(None)
      val result: Future[Result] = controller.getThresholdForDate("2012-03-20")(FakeRequest())
      status(result) mustBe NOT_FOUND
    }
  }
}