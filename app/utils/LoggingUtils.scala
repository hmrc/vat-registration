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

package utils

import org.slf4j.{Logger, LoggerFactory}
import play.api.LoggerLike
import play.api.mvc.Request
import uk.gov.hmrc.http.{HeaderNames, SessionKeys}

trait LoggingUtils extends LoggerLike {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  lazy val sessionId: Request[_] => Option[String] =
    request => request.session.get(SessionKeys.sessionId).map(sessionId => s"sessionId: $sessionId ")

  lazy val trueClientIp: Request[_] => Option[String] =
    request => request.headers.get(HeaderNames.trueClientIp).map(trueClientIp => s"trueClientIp: $trueClientIp ")

  lazy val identifiers: Request[_] => String =
    request => Seq(trueClientIp(request), sessionId(request)).flatten.foldLeft("")(_ + _)

  lazy val handleRegId: String => String =
    regId => if (regId.nonEmpty) s"(regId: $regId)" else ""

  def infoLog(message: => String, regId: String = "")(implicit request: Request[_]): Unit =
    logger.info(s"$message ${handleRegId(regId)} (${identifiers(request)})")

  def warnLog(message: => String, regId: String = "")(implicit request: Request[_]): Unit =
    logger.warn(s"$message ${handleRegId(regId)} (${identifiers(request)})")

  def errorLog(message: => String, regId: String = "")(implicit request: Request[_]): Unit =
    logger.error(s"$message ${handleRegId(regId)} (${identifiers(request)})")

}
