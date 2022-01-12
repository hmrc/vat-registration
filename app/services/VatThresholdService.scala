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

package services

import com.typesafe.config.{ConfigList, ConfigRenderOptions}
import config.BackendConfig
import javax.inject.{Inject, Singleton}
import models.VatThreshold
import org.joda.time.DateTime
import play.api.libs.json._

@Singleton
class VatThresholdService @Inject()(backendConfig: BackendConfig) {

  lazy val thresholdString: String = backendConfig.runModeConfiguration.get[ConfigList]("thresholds").render(ConfigRenderOptions.concise())
  lazy val thresholds: Seq[VatThreshold] = Json.parse(thresholdString).as[List[VatThreshold]]

  def getThresholdForGivenDate(givenDate: DateTime): Option[VatThreshold] = {
    thresholds
      .sortWith(_.date isAfter _.date)
      .find(model => givenDate.isAfter(model.date) || givenDate.isEqual(model.date))
  }
}