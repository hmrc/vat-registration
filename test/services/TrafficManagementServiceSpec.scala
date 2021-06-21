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

import config.BackendConfig
import helpers.VatRegSpec
import mocks.{MockDailyQuotaRepository, MockTrafficManagementRepository}
import models.api.{Draft, OTRS, RegistrationInformation, VatReg}
import models.submission.{Individual, UkCompany}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.FakeTimeMachine

import java.time.LocalDate
import scala.concurrent.Future

class TrafficManagementServiceSpec extends VatRegSpec
  with MockDailyQuotaRepository
  with MockTrafficManagementRepository {


  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(Map(
      "traffic-management.hours.from"-> 9,
      "traffic-management.hours.until"-> 17,
      "traffic-management.quotas.uk-company-enrolled"-> 15,
      "traffic-management.quotas.sole-trader" -> 1
    ))
    .build()

  implicit val config = app.injector.instanceOf[BackendConfig]

  class Setup(hour: Int = 9) {
    FakeTimeMachine.hour = hour
    val timeMachine = new FakeTimeMachine

    object Service extends TrafficManagementService(
      mockDailyQuotaRepository,
      mockTrafficManagementRepository,
      timeMachine
    )

    val testRegInfo = RegistrationInformation(
      internalId = testInternalId,
      registrationId = testRegId,
      status = Draft,
      regStartDate = timeMachine.today,
      channel = VatReg,
      lastModified = timeMachine.today
    )
  }

  val testInternalId = "testInternalId"
  val testRegId = "testRegID"
  val testDate = LocalDate.of(2020, 1, 1)
  implicit val hc = HeaderCarrier()

  "allocate" must {
    "return QuotaReached when the quota is exceeded" in new Setup() {
      mockCurrentTotal(UkCompany, isEnrolled = true)(16)
      mockUpsertRegInfo(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.today)(
        Future.successful(RegistrationInformation(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.today))
      )

      val res = await(Service.allocate(testInternalId, testRegId, UkCompany, isEnrolled = true))

      res mustBe QuotaReached
    }
    "return Allocated when the quota has not been exceeded" in new Setup() {
      mockCurrentTotal(UkCompany, isEnrolled = true)(1)
      mockUpsertRegInfo(testInternalId, testRegId, Draft, testDate, VatReg, timeMachine.today)(
        Future.successful(RegistrationInformation(testInternalId, testRegId, Draft, testDate, VatReg, timeMachine.today))
      )
      mockIncrement(UkCompany, isEnrolled = true)(1)

      val res = await(Service.allocate(testInternalId, testRegId, UkCompany, isEnrolled = true))

      res mustBe Allocated
    }
    "return quota reached before opening hours" in new Setup(hour = 8) {
      mockCurrentTotal(UkCompany, isEnrolled = true)(1)
      mockUpsertRegInfo(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.today)(
        Future.successful(RegistrationInformation(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.today))
      )

      val res = await(Service.allocate(testInternalId, testRegId, UkCompany, isEnrolled = true))

      res mustBe QuotaReached
    }
    "return quota reached after opening hours" in new Setup(hour = 18) {
      mockCurrentTotal(UkCompany, isEnrolled = true)(1)
      mockUpsertRegInfo(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.today)(
        Future.successful(RegistrationInformation(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.today))
      )

      val res = await(Service.allocate(testInternalId, testRegId, UkCompany, isEnrolled = true))

      res mustBe QuotaReached
    }
    "apply different quotas for different entity types" in new Setup() {
      mockCurrentTotal(UkCompany, isEnrolled = true)(14)
      mockCurrentTotal(Individual, isEnrolled = false)(1)
      mockUpsertRegInfo(testInternalId, testRegId, Draft, testDate, VatReg, timeMachine.today)(
        Future.successful(RegistrationInformation(testInternalId, testRegId, Draft, testDate, VatReg, timeMachine.today))
      )
      mockIncrement(UkCompany, isEnrolled = true)(1)

      val allocatableRes = await(Service.allocate(testInternalId, testRegId, UkCompany, isEnrolled = true))
      allocatableRes mustBe Allocated
      val nonAllocatableRes = await(Service.allocate(testInternalId, testRegId, Individual, isEnrolled = false))
      nonAllocatableRes mustBe QuotaReached
    }
  }

  "getRegistrationInformation" must {
    "return the registration information where it exists" in new Setup {
      mockGetRegInfo(testInternalId)(Future.successful(Some(testRegInfo)))

      val res = await(Service.getRegistrationInformation(testInternalId))

      res mustBe Some(testRegInfo)
    }
    "return None where a record doesn't exist" in new Setup {
      mockGetRegInfo(testInternalId)(Future.successful(None))

      val res = await(Service.getRegistrationInformation(testInternalId))

      res mustBe None
    }
  }

  "upsertRegistrationInformation" must {
    "return registration information" in new Setup {
      val regInfo = RegistrationInformation(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.today)
      mockUpsertRegInfo(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.today)(Future.successful(regInfo))

      val res = await(Service.upsertRegistrationInformation(testInternalId, testRegId, Draft, testDate, OTRS))

      res mustBe regInfo
    }
  }

  "clearDocument" must {
    "return true" in new Setup {
      mockClearDocument(testInternalId)(response = Future.successful(true))

      val res = await(Service.clearDocument(testInternalId))

      res mustBe true
    }
  }

}
