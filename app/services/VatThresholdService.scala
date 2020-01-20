/*
 * Copyright 2020 HM Revenue & Customs
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

package services

import javax.inject.Inject
import com.typesafe.config.ConfigRenderOptions
import models.VatThreshold
import org.joda.time.DateTime
import play.api.Configuration
import play.api.libs.json._

class VatThresholdServiceImpl @Inject()(config: Configuration) extends VatThresholdService {
  lazy val thresholdString = config.underlying.getList("thresholds").render(ConfigRenderOptions.concise())
  lazy val thresholds = Json.parse(thresholdString).as[List[VatThreshold]]
}

trait VatThresholdService {
  val thresholds: List[VatThreshold]

  def getThresholdForGivenDate(givenDate: DateTime): Option[VatThreshold] = {
    thresholds
      .sortWith(_.date isAfter _.date)
      .find(model => givenDate.isAfter(model.date) || givenDate.isEqual(model.date))
  }
}