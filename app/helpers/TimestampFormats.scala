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

package helpers

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json.{Format, JsString, Reads, Writes}

object TimestampFormats {

  // $COVERAGE-OFF$ this object is currently not used but might be required if timestamp/date format changes
  val timestampFormat: Format[DateTime] = {
    val isoFormatter = ISODateTimeFormat.dateTimeParser().withOffsetParsed()

    Format[DateTime](
      Reads[DateTime](js => js.validate[String].map(isoFormatter.parseDateTime)),
      Writes[DateTime](d => JsString(isoFormatter.print(d))))

  }
  val dateFormat: Format[DateTime] = {
    val isoFormatter = ISODateTimeFormat.dateParser().withOffsetParsed()

    Format[DateTime](
      Reads[DateTime](js => js.validate[String].map(isoFormatter.parseDateTime)),
      Writes[DateTime](d => JsString(isoFormatter.print(d)))
    )
  }
  // $COVERAGE-ON$

}
