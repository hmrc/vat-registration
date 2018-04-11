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

package services

import javax.inject.Inject
import models.VatThreshold
import org.joda.time.DateTime
import play.api.libs.json._
import uk.gov.hmrc.play.config.ServicesConfig

import scala.io.Source._

class VatThresholdServiceImpl @Inject() extends VatThresholdService with ServicesConfig {
  lazy val thresholdString = fromFile(getConfString("ThresholdsJsonLocation", "../conf/thresholds.json")).mkString
  lazy val thresholds = Json.parse(thresholdString).as[List[VatThreshold]]
  implicit lazy val ThresholdReads = Json.reads[VatThreshold]
}

trait VatThresholdService {
  val thresholds: List[VatThreshold]

  def getThresholdForGivenTime(givenDate: DateTime): Option[VatThreshold] = {
    thresholds
      .sortWith(descendingDates)
      .find(model => givenDate.isAfter(model.date) || givenDate.isEqual(model.date))
  }

  private def descendingDates(vt1: VatThreshold, vt2: VatThreshold) = {
    vt1.date isAfter vt2.date
  }
}