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

package controllers

import itutil.IntegrationStubbing
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class VatThresholdControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper()

  "VatThresholds" should {
    "return valid threshold amount and change date for given date" in new Setup {
      val response: WSResponse = await(client(routes.VatThresholdController.getThresholdForDate("2001-06-04").url).get())

      response.status mustBe OK
      response.body mustBe """{"since":"2001-04-01","taxable-threshold":"54000"}"""
    }
  }
}
