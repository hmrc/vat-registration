/*
 * Copyright 2018 HM Revenue & Customs
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

import common.{RegistrationId, TransactionId}

import scala.util.control.NoStackTrace

sealed trait DBExceptions {
  val id: RegistrationId
}

case class NoReturns() extends NoStackTrace
case class NoTradingDetails(msg: String) extends NoStackTrace
case class InvalidSubmissionStatus(msg: String) extends NoStackTrace
case class UnknownIncorpStatus(msg: String) extends NoStackTrace
case class NoTransactionId(msg: String) extends NoStackTrace
case class NoRegIdException(msg: String) extends NoStackTrace
case class NoCompanyName(msg: String) extends NoStackTrace
case class NoIncorpDate(msg: String) extends NoStackTrace
case class NoVatSchemeWithTransId(id: TransactionId) extends NoStackTrace
case class MissingRegDocument(id: RegistrationId) extends NoStackTrace with DBExceptions {
  override def getMessage: String = s"No Registration document found for regId: ${id.value}"
}

case class UpdateFailed(id: RegistrationId, attemptedModel: String) extends NoStackTrace with DBExceptions
case class InsertFailed(id: RegistrationId, attemptedModel: String) extends NoStackTrace with DBExceptions
