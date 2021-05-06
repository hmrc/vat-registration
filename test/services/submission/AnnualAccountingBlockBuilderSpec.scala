/*
 * Copyright 2021 HM Revenue & Customs
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

package services.submission

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import org.mockito.Mockito.when
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import scala.concurrent.Future

class AnnualAccountingBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service: AnnualAccountingBlockBuilder = new AnnualAccountingBlockBuilder(
      registrationMongoRepository = mockRegistrationMongoRepository
    )
  }

  val annualAccountingBlockJson: JsObject = Json.parse(
    """
      {"submissionType":"1",
      |"customerRequest":{
      |   "paymentMethod":"01",
      |   "annualStagger":"YA",
      |   "paymentFrequency":"M",
      |   "estimatedTurnover":123456,
      |   "reqStartDate":"2018-01-01"
      |   }
      |}
      |""".stripMargin).as[JsObject]

  "buildAnnualAccountingBlock" should {
    "return the correct json" when {
      "the applicant wants to join AAS and all data is provided" in new Setup {
        when(mockRegistrationMongoRepository.fetchAnnualAccountingScheme(testRegId))
          .thenReturn(Future.successful(Some(validFullAAS)))

        val result: Option[JsObject] = await(service.buildAnnualAccountingBlock(testRegId))
        result mustBe Some(annualAccountingBlockJson)
      }
    }
  }
}
