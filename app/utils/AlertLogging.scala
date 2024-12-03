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

package utils

import java.time.format.{DateTimeFormatter, TextStyle}
import java.time.{LocalDate, LocalTime, ZoneOffset}
import java.util.Locale

object PagerDutyKeys extends Enumeration {
  val NOTIFY_SDES_FAILED: PagerDutyKeys.Value = Value
  val SDES_CALLBACK_FAILED: PagerDutyKeys.Value = Value
  val INVALID_SDES_PAYLOAD_RECEIVED: PagerDutyKeys.Value = Value
  val UNEXPECTED_SDES_CALLBACK_STATUS: PagerDutyKeys.Value = Value
  val NRS_NOTIFICATION_FAILED: PagerDutyKeys.Value = Value
  val SDES_NRS_SUBMISSION_ID_MISSING: PagerDutyKeys.Value = Value
  val INVALID_UPSCAN_DETAILS_RECEIVED: PagerDutyKeys.Value = Value
  val NO_DATA_FOUND_IN_UPSCAN_MONGO: PagerDutyKeys.Value = Value
  val NRS_SUBMISSION_FAILED: PagerDutyKeys.Value = Value
  val NRS_ATTACHMENT_NOTIFICATION_FAILED: PagerDutyKeys.Value = Value
}

trait AlertLogging extends LoggingUtils {

  protected val loggingDays: String = "MON,TUE,WED,THU,FRI"
  protected val loggingTimes: String = "08:00:00_17:00:00"

  def pagerduty(key: PagerDutyKeys.Value, message: Option[String] = None): Unit = {
    val log = s"${key.toString}${message.fold("")(msg => s" - $msg")}"
    if (inWorkingHours) logger.error(log) else logger.info(log)
  }

  private[utils] def today: String = LocalDate.now(ZoneOffset.UTC).getDayOfWeek.getDisplayName(TextStyle.SHORT, Locale.UK).toUpperCase

  private[utils] def now: LocalTime = LocalTime.now

  private[utils] def isLoggingDay = loggingDays.split(",").contains(today)

  private[utils] def isBetweenLoggingTimes: Boolean = {
    val stringToDate = LocalTime.parse(_: String, DateTimeFormatter.ofPattern("HH:mm:ss"))
    val Array(start, end) = loggingTimes.split("_") map stringToDate
    ((start isBefore now) || (now equals start)) && (now isBefore end)
  }

  def inWorkingHours: Boolean = isLoggingDay && isBetweenLoggingTimes

}
