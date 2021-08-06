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

import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.ApplicantDetails
import models.submission.UkCompany
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.test.Helpers._

import scala.concurrent.Future

class ApplicantDetailsServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service: ApplicantDetailsService = new ApplicantDetailsService(
      registrationRepository = mockRegistrationMongoRepository
    )

    def updateIVPassedToMongo(): OngoingStubbing[Future[Boolean]] = when(mockRegistrationMongoRepository.updateIVStatus(any(),any()))
      .thenReturn(Future.successful(true))

    def updateIVPassedToMongoFail(): OngoingStubbing[Future[Boolean]] = when(mockRegistrationMongoRepository.updateIVStatus(any(),any()))
      .thenReturn(Future.failed(new Exception("")))

    def updateIVPassedToMongoNoRegDoc(): OngoingStubbing[Future[Boolean]] = when(mockRegistrationMongoRepository.updateIVStatus(any(),any()))
      .thenReturn(Future.failed(MissingRegDocument(testRegId)))
  }

  "getApplicantDetailsData" should {
    "return an applicant if found" in new Setup {
      when(mockRegistrationMongoRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(Some(validApplicantDetails)))

      val result: Option[ApplicantDetails] = await(service.getApplicantDetailsData("regId", UkCompany))
      result mustBe Some(validApplicantDetails)
    }

    "return None if none found matching regId" in new Setup {
      when(mockRegistrationMongoRepository.getApplicantDetails(any(), any()))
        .thenReturn(Future.successful(None))

      val result: Option[ApplicantDetails] = await(service.getApplicantDetailsData("regId", UkCompany))
      result mustBe None
    }
  }

  "updateApplicantDetailsData" should {
    "return the data that is being inputted" in new Setup {
      when(mockRegistrationMongoRepository.patchApplicantDetails(any(),any()))
        .thenReturn(Future.successful(validApplicantDetails))

      val result = await(service.updateApplicantDetailsData("regId", validApplicantDetails))
      result mustBe validApplicantDetails
    }

    "encounter an exception if an error occurs" in new Setup {
      when(mockRegistrationMongoRepository.patchApplicantDetails(any(),any()))
        .thenReturn(Future.failed(new Exception("")))

      intercept[Exception](await(service.updateApplicantDetailsData("regId", validApplicantDetails)))
    }

    "encounter an MissingRegDocument Exception if no document is found" in new Setup {
      when(mockRegistrationMongoRepository.patchApplicantDetails(any(),any()))
        .thenReturn(Future.failed(MissingRegDocument(testRegId)))

      intercept[MissingRegDocument](await(service.updateApplicantDetailsData("regId", validApplicantDetails)))
    }
  }
}
