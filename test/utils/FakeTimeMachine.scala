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

package utils

import fixtures.VatRegistrationFixture

import java.time.{LocalDate, LocalDateTime, LocalTime}

class FakeTimeMachine extends TimeMachine with VatRegistrationFixture {
  override def today: LocalDate = LocalDate.parse("2020-01-01")
  override def timestamp: LocalDateTime = LocalDateTime.of(testDate, LocalTime.of(FakeTimeMachine.hour, 0))
}

object FakeTimeMachine {
  var hour: Int = 9
}
