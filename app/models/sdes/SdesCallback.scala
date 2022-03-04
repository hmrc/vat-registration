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

package models.sdes

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

case class SdesCallback(notification: String,
                        filename: String,
                        correlationID: String,
                        dateTime: LocalDateTime,
                        checksumAlgorithm: Option[String] = None,
                        checksum: Option[String] = None,
                        availableUntil: Option[LocalDateTime] = None,
                        properties: List[Property] = Nil,
                        failureReason: Option[String] = None) {
  def getPropertyValue(key: String): Option[String] = this.properties.find(_.name.equals(key)).map(_.value)
}

object SdesCallback {
  implicit val format: OFormat[SdesCallback] = Json.format[SdesCallback]
}
