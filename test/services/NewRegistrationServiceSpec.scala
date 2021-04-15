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
import featureswitch.core.config.FeatureSwitching
import helpers.VatRegSpec
import mocks.{MockDailyQuotaRepository, MockRegistrationRepository, MockTrafficManagementService}
import models.VatRegStatus
import models.api.{Draft, RegistrationInformation, VatReg, VatScheme}
import org.scalatest.BeforeAndAfterEach
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate

class NewRegistrationServiceSpec extends VatRegSpec
  with MockDailyQuotaRepository
  with MockTrafficManagementService
  with MockRegistrationRepository
  with FeatureSwitching
  with BeforeAndAfterEach {

  implicit val hc = HeaderCarrier()
  implicit val config = app.injector.instanceOf[BackendConfig]
  val testRegId = "testRegId"
  val testInternalId = "testInternalId"
  val testDate = LocalDate.parse("2020-01-01")

  val testVatScheme = VatScheme(
    id = testRegId,
    internalId = testInternalId,
    status = VatRegStatus.draft
  )

  val testRegInfo = RegistrationInformation(
    internalId = testInternalId,
    registrationId = testRegId,
    status = Draft,
    regStartDate = testDate,
    channel = VatReg,
    lastModified = testDate
  )

  object Service extends NewRegistrationService(
    registrationRepository = mockRegistrationRepository
  ) {
    override private[services] def generateRegistrationId(): String = testRegId
  }

  "newRegistration" should {
    "return a vat scheme" in {
      mockCreateRegistration(testRegId, testInternalId)(testVatScheme)

      val res = await(Service.newRegistration(testInternalId))

      res mustBe testVatScheme
    }
  }

  "insertVatScheme" should {
    "return a vat scheme" in {
      mockInsertVatScheme(testVatScheme)

      val res = await(Service.insertVatScheme(testVatScheme))

      res mustBe testVatScheme
    }
  }

}
