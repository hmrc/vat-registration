/*
 * Copyright 2024 HM Revenue & Customs
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

package itutil

import utils.TimeMachine

import java.time.{LocalDate, LocalDateTime, LocalTime}
import javax.inject.Singleton

@Singleton
class FakeTimeMachine extends TimeMachine with ITFixtures {
  override def today: LocalDate = testDate
  override def timestamp: LocalDateTime = LocalDateTime.of(testDate, LocalTime.of(FakeTimeMachine.hour, 0))
}

object FakeTimeMachine {
  var hour: Int = 9
}
