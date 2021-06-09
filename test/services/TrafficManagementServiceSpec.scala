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

package services

import java.time.LocalDate

import helpers.VatRegSpec
import mocks.{MockDailyQuotaRepository, MockTrafficManagementRepository}
import models.api.{Draft, OTRS, RegistrationInformation, VatReg}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.FakeTimeMachine

import scala.concurrent.Future

class TrafficManagementServiceSpec extends VatRegSpec
  with MockDailyQuotaRepository
  with MockTrafficManagementRepository {

  val timeMachine = new FakeTimeMachine

  object Service extends TrafficManagementService(
    mockDailyQuotaRepository,
    mockTrafficManagementRepository,
    timeMachine
  )

  val testInternalId = "testInternalId"
  val testRegId = "testRegID"
  val testDate = LocalDate.of(2020, 1, 1)
  implicit val hc = HeaderCarrier()

  val testRegInfo = RegistrationInformation(
    internalId = testInternalId,
    registrationId = testRegId,
    status = Draft,
    regStartDate = timeMachine.today,
    channel = VatReg,
    lastModified = timeMachine.today
  )

  "allocate" must {
    "return QuotaReached when the quota is exceeded" in {
      mockCheckQuota(response = true)
      mockUpsertRegInfo(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.today)(
        Future.successful(RegistrationInformation(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.today))
      )

      val res = await(Service.allocate(testInternalId, testRegId))

      res mustBe QuotaReached
    }
    "return Allocated when the quota has not been exceeded" in {
      mockCheckQuota(response = false)
      mockUpsertRegInfo(testInternalId, testRegId, Draft, testDate, VatReg, timeMachine.today)(
        Future.successful(RegistrationInformation(testInternalId, testRegId, Draft, testDate, VatReg, timeMachine.today))
      )

      val res = await(Service.allocate(testInternalId, testRegId))

      res mustBe Allocated
    }
  }

  "getRegistrationInformation" must {
    "return the registration information where it exists" in {
      mockGetRegInfo(testInternalId)(Future.successful(Some(testRegInfo)))

      val res = await(Service.getRegistrationInformation(testInternalId))

      res mustBe Some(testRegInfo)
    }
    "return None where a record doesn't exist" in {
      mockGetRegInfo(testInternalId)(Future.successful(None))

      val res = await(Service.getRegistrationInformation(testInternalId))

      res mustBe None
    }
  }

  "upsertRegistrationInformation" must {
    "return registration information" in {
      val regInfo = RegistrationInformation(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.today)
      mockUpsertRegInfo(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.today)(Future.successful(regInfo))

      val res = await(Service.upsertRegistrationInformation(testInternalId, testRegId, Draft, testDate, OTRS))

      res mustBe regInfo
    }
  }

  "clearDocument" must {
    "return true" in {
      mockClearDocument(testInternalId)(response = Future.successful(true))

      val res = await(Service.clearDocument(testInternalId))

      res mustBe true
    }
  }

}
