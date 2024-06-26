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

package models.api

import models.submission.{IdType, UtrIdType, VrnIdType}
import play.api.libs.json.{Json, OFormat}

case class OtherBusinessInvolvement(
  businessName: Option[String],
  hasVrn: Option[Boolean],
  vrn: Option[String],
  hasUtr: Option[Boolean],
  utr: Option[String],
  stillTrading: Option[Boolean]
) {
  val optIdType: Option[IdType] = if (hasVrn.contains(true)) {
    Some(VrnIdType)
  } else if (hasUtr.contains(true)) {
    Some(UtrIdType)
  } else {
    None
  }

  val optIdValue: Option[String] = optIdType.flatMap {
    case VrnIdType => vrn
    case UtrIdType => utr
  }
}

object OtherBusinessInvolvement {
  implicit val format: OFormat[OtherBusinessInvolvement] = Json.format[OtherBusinessInvolvement]
}
