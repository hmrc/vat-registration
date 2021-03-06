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
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future

class AdminBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  object TestBuilder extends AdminBlockBuilder(mockRegistrationMongoRepository)

  val expectedJson: JsObject =
    Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> "2"
      ),
      "attachments" -> Json.obj(
        "EORIrequested" -> true
      )
    )

  "buildAdminBlock" should {
    "return an admin block json object" when {
      "both eligibility and trading details data are in the database" in {
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))

        when(mockRegistrationMongoRepository.retrieveTradingDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(validFullTradingDetails)))

        val result = await(TestBuilder.buildAdminBlock(testRegId))

        result mustBe expectedJson

      }
    }

    "throw an exception" when {
      "the eligibility data is missing from the database" in {
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(None))

        when(mockRegistrationMongoRepository.retrieveTradingDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(validFullTradingDetails)))

        intercept[InternalServerException] {
          await(TestBuilder.buildAdminBlock(testRegId))
        }

      }

      "the trading details data is missing from the database" in {
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(Some(testEligibilitySubmissionData)))

        when(mockRegistrationMongoRepository.retrieveTradingDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(None))

        intercept[InternalServerException] {
          await(TestBuilder.buildAdminBlock(testRegId))
        }
      }

      "there is no eligibility data or trading details data in the database" in {
        when(mockRegistrationMongoRepository.fetchEligibilitySubmissionData(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(None))

        when(mockRegistrationMongoRepository.retrieveTradingDetails(ArgumentMatchers.eq(testRegId)))
          .thenReturn(Future.successful(None))

        intercept[InternalServerException] {
          await(TestBuilder.buildAdminBlock(testRegId))
        }

      }
    }
  }
}
