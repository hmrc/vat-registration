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

package common.exceptions

import scala.util.control.NoStackTrace

sealed trait DBExceptions {
  val id: String
}

case class InvalidSubmissionStatus(msg: String) extends NoStackTrace {
  override def getMessage: String = msg
}
case class MissingRegDocument(id: String) extends NoStackTrace with DBExceptions {
  override def getMessage: String = s"No Registration document found for regId: ${id}"
}

case class UpdateFailed(id: String, attemptedModel: String) extends NoStackTrace with DBExceptions
case class InsertFailed(id: String, attemptedModel: String) extends NoStackTrace with DBExceptions
