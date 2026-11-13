/*
 * Copyright 2026 HM Revenue & Customs
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

package models.hip

import play.api.libs.json.{Json, Reads}

case class HipInner422Err(code: String, processingDate: String, text: String)

object HipInner422Err {
  implicit val reads: Reads[HipInner422Err] = Json.reads[HipInner422Err]
}

case class Hip422Error(error: HipInner422Err)

object Hip422Error {
  implicit val reads: Reads[Hip422Error] = Json.reads[Hip422Error]
}
