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

package services

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.VatThreshold

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class VatThresholdServiceSpec extends VatRegSpec with VatRegistrationFixture {

  def date(s: String): LocalDate = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE)

  class Setup {
    val service: VatThresholdService = new VatThresholdService(backendConfig) {
      override lazy val thresholds: List[VatThreshold] =
        List[VatThreshold](
          VatThreshold(date("2001-01-01"), "1"),
          VatThreshold(date("2002-02-02"), "2"),
          VatThreshold(date("2003-03-03"), "3"),
          VatThreshold(date("2004-04-04"), "4")
        )
    }
  }

  "return most recent threshold for given date" in new Setup {
    val result: Option[VatThreshold] = service.getThresholdForGivenDate(date("2001-05-15"))
    result mustBe Some(VatThreshold(date("2001-01-01"), "1"))
  }

  "return most updated threshold for same date edge case" in new Setup {
    val result: Option[VatThreshold] = service.getThresholdForGivenDate(date("2002-02-02"))
    result mustBe Some(VatThreshold(date("2002-02-02"), "2"))
  }

  "return most recent threshold for future dates" in new Setup {
    val result: Option[VatThreshold] = service.getThresholdForGivenDate(date("2010-01-01"))
    result mustBe Some(VatThreshold(date("2004-04-04"), "4"))
  }

  "return none for date before known thresholds" in new Setup {
    val result: Option[VatThreshold] = service.getThresholdForGivenDate(date("2000-01-01"))
    result mustBe None
  }
}