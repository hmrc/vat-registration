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

package common.exceptions

import common.DBResponse

object InternalExceptions extends InternalExceptions

trait InternalExceptions {

  class IncorrectDBSuccessResponseException(expected: Any, actual: Any) extends Exception(
    s"Success Response of type [${actual.getClass.toString}] did not match expected type [${expected.getClass.toString}]"
  )

  class UnexpextedDBResponseException(action: String, resp: DBResponse) extends Exception(
    s"Unexpected DB Response of type [${resp.getClass.toString}] returned in action $action"
  )

}
