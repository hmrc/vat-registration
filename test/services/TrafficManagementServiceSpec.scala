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

import config.BackendConfig
import helpers.VatRegSpec
import mocks.{MockDailyQuotaRepository, MockTrafficManagementRepository}
import models.api.{Draft, OTRS, RegistrationInformation, VatReg}
import models.submission._
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
      "traffic-management.hours.from" -> 9,
      "traffic-management.hours.until" -> 17,
      "traffic-management.quotas.uk-company-enrolled" -> 15,
      "traffic-management.quotas.sole-trader" -> 1,
      "traffic-management.quotas.sole-trader-enrolled" -> 2,
      "traffic-management.quotas.netp-enrolled" -> 2,
      "traffic-management.quotas.non-uk-company-enrolled" -> 2,
      "traffic-management.quotas.reg-society-enrolled" -> 2,
      "traffic-management.quotas.charitable-incorp-org-enrolled" -> 2,
      "traffic-management.quotas.partnership-enrolled" -> 2,
      "traffic-management.quotas.partnership" -> 2,
      "traffic-management.quotas.limited-partnership-enrolled" -> 2,
      "traffic-management.quotas.limited-partnership" -> 2,
      "traffic-management.quotas.scottish-partnership" -> 2,
      "traffic-management.quotas.scottish-partnership-enrolled" -> 2,
      "traffic-management.quotas.scottish-limited-partnership" -> 2,
      "traffic-management.quotas.scottish-limited-partnership-enrolled" -> 2,
      "traffic-management.quotas.limited-liability-partnership-enrolled" -> 2,
      "traffic-management.quotas.limited-liability-partnership" -> 2
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
      lastModified = timeMachine.timestamp
    )
  }

  val testInternalId = "testInternalId"
  val testRegId = "testRegID"
  val testDate = LocalDate.of(2020, 1, 1)
  implicit val hc = HeaderCarrier()

  "allocate" must {
    "return QuotaReached when the quota is exceeded" in new Setup() {
      mockCurrentTotal(UkCompany, isEnrolled = true)(16)
      mockUpsertRegInfoById(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.timestamp)(
        Future.successful(RegistrationInformation(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.timestamp))
      )

      val res = await(Service.allocate(testInternalId, testRegId, UkCompany, isEnrolled = true))

      res mustBe QuotaReached
    }
    Seq(UkCompany, Individual, NETP, NonUkNonEstablished, RegSociety, CharitableOrg, Partnership, LtdPartnership, ScotPartnership, ScotLtdPartnership, LtdLiabilityPartnership).foreach { partyType =>
      s"return Allocated when the quota has not been exceeded for $partyType" in new Setup() {
        mockCurrentTotal(partyType, isEnrolled = true)(1)
        mockUpsertRegInfoById(testInternalId, testRegId, Draft, testDate, VatReg, timeMachine.timestamp)(
          Future.successful(RegistrationInformation(testInternalId, testRegId, Draft, testDate, VatReg, timeMachine.timestamp))
        )
        mockIncrement(partyType, isEnrolled = true)(1)

        val res = await(Service.allocate(testInternalId, testRegId, partyType, isEnrolled = true))

        res mustBe Allocated
      }
    }
    "return quota reached before opening hours" in new Setup(hour = 8) {
      mockCurrentTotal(UkCompany, isEnrolled = true)(1)
      mockUpsertRegInfoById(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.timestamp)(
        Future.successful(RegistrationInformation(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.timestamp))
      )

      val res = await(Service.allocate(testInternalId, testRegId, UkCompany, isEnrolled = true))

      res mustBe QuotaReached
    }
    "return quota reached after opening hours" in new Setup(hour = 18) {
      mockCurrentTotal(UkCompany, isEnrolled = true)(1)
      mockUpsertRegInfoById(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.timestamp)(
        Future.successful(RegistrationInformation(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.timestamp))
      )

      val res = await(Service.allocate(testInternalId, testRegId, UkCompany, isEnrolled = true))

      res mustBe QuotaReached
    }
    "apply different quotas for different entity types" in new Setup() {
      mockCurrentTotal(UkCompany, isEnrolled = true)(14)
      mockCurrentTotal(Individual, isEnrolled = false)(1)
      mockUpsertRegInfoById(testInternalId, testRegId, Draft, testDate, VatReg, timeMachine.timestamp)(
        Future.successful(RegistrationInformation(testInternalId, testRegId, Draft, testDate, VatReg, timeMachine.timestamp))
      )
      mockIncrement(UkCompany, isEnrolled = true)(1)

      val allocatableRes = await(Service.allocate(testInternalId, testRegId, UkCompany, isEnrolled = true))
      allocatableRes mustBe Allocated
      val nonAllocatableRes = await(Service.allocate(testInternalId, testRegId, Individual, isEnrolled = false))
      nonAllocatableRes mustBe QuotaReached
    }
  }

  "getRegInfoById" must {
    "return the registration information where it exists" in new Setup {
      mockGetRegInfoById(testInternalId, testRegId)(Future.successful(Some(testRegInfo)))

      val res = await(Service.getRegInfoById(testInternalId, testRegId))

      res mustBe Some(testRegInfo)
    }
    "return None where a record doesn't exist" in new Setup {
      mockGetRegInfoById(testInternalId, testRegId)(Future.successful(None))

      val res = await(Service.getRegInfoById(testInternalId, testRegId))

      res mustBe None
    }
  }

  "upsertRegInfoById" must {
    "return registration information" in new Setup {
      val regInfo = RegistrationInformation(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.timestamp)
      mockUpsertRegInfoById(testInternalId, testRegId, Draft, testDate, OTRS, timeMachine.timestamp)(Future.successful(regInfo))

      val res = await(Service.upsertRegInfoById(testInternalId, testRegId, Draft, testDate, OTRS))

      res mustBe regInfo
    }
  }

  "clearDocument" must {
    "return true" in new Setup {
      mockDeleteRegInfoById(testInternalId, testRegId)(response = Future.successful(true))

      val res = await(Service.deleteRegInfoById(testInternalId, testRegId))

      res mustBe true
    }
  }

}
