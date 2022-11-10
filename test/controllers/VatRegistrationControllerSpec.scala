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

package controllers

import enums.VatRegStatus
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import mocks.MockRegistrationService
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.libs.json.Json
import play.api.mvc.{Request, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

class VatRegistrationControllerSpec extends VatRegSpec with VatRegistrationFixture with MockRegistrationService {

  import play.api.test.Helpers._

  class Setup {
    val controller: VatRegistrationController = new VatRegistrationController(
      mockVatRegistrationService,
      mockSubmissionService,
      mockAuthConnector,
      mockRegistrationService,
      stubControllerComponents()
    )
  }

  "Calling submitVATRegistration" should {
    "return a Forbidden response if the user is not logged in" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised(testRegId)

      val response: Future[Result] = controller.submitVATRegistration(testRegId)(FakeRequest().withBody(Json.obj()))
      status(response) mustBe Status.FORBIDDEN
    }

    "return Conflict for a duplicate submission" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      ServiceMocks.mockGetDocumentStatus(VatRegStatus.duplicateSubmission)

      val response: Future[Result] = controller.submitVATRegistration(testRegId)(FakeRequest().withBody(
        Json.obj("userHeaders" -> testUserHeaders)
      ))

      status(response) mustBe CONFLICT
    }

    "return TooManyRequests if there is already a submission being processed" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      ServiceMocks.mockGetDocumentStatus(VatRegStatus.locked)

      val response: Future[Result] = controller.submitVATRegistration(testRegId)(FakeRequest().withBody(
        Json.obj("userHeaders" -> testUserHeaders)
      ))

      status(response) mustBe TOO_MANY_REQUESTS
    }

    "return Ok if the submission has already been completed" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      ServiceMocks.mockGetDocumentStatus(VatRegStatus.submitted)

      val response: Future[Result] = controller.submitVATRegistration(testRegId)(FakeRequest().withBody(
        Json.obj("userHeaders" -> testUserHeaders)
      ))

      status(response) mustBe OK
    }

    "return an exception if the Submission Service can't make a DES submission" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      ServiceMocks.mockGetDocumentStatus(VatRegStatus.draft)

      when(mockSubmissionService.submitVatRegistration(
        ArgumentMatchers.eq(testInternalId),
        ArgumentMatchers.eq(testRegId),
        ArgumentMatchers.eq(testUserHeaders),
        ArgumentMatchers.eq("en")

      )(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.failed(UpstreamErrorResponse("message", 501)))

      val response: UpstreamErrorResponse = intercept[UpstreamErrorResponse](await(controller.submitVATRegistration(testRegId)(FakeRequest().withBody(
        Json.obj("userHeaders" -> testUserHeaders)
      ))))

      response mustBe UpstreamErrorResponse("message", 501)
    }

    "return an Ok response for a valid submit" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      ServiceMocks.mockGetDocumentStatus(VatRegStatus.draft)

      when(mockSubmissionService.submitVatRegistration(
        ArgumentMatchers.eq(testInternalId),
        ArgumentMatchers.eq(testRegId),
        ArgumentMatchers.eq(testUserHeaders),
        ArgumentMatchers.eq("en")
      )(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.successful("VRS00000000001"))

      val response: Future[Result] = controller.submitVATRegistration(testRegId)(FakeRequest().withBody(
        Json.obj("userHeaders" -> testUserHeaders)
      ))
      status(response) mustBe Status.OK
    }
    "return an Ok response for a valid submission with partners" in new Setup {
      AuthorisationMocks.mockAuthorised(testRegId, testInternalId)
      ServiceMocks.mockGetDocumentStatus(VatRegStatus.draft)

      when(mockSubmissionService.submitVatRegistration(
        ArgumentMatchers.eq(testInternalId),
        ArgumentMatchers.eq(testRegId),
        ArgumentMatchers.eq(testUserHeaders),
        ArgumentMatchers.eq("en")
      )(any[HeaderCarrier], any[Request[_]]))
        .thenReturn(Future.successful("VRS00000000001"))

      val response: Future[Result] = controller.submitVATRegistration(testRegId)(FakeRequest().withBody(
        Json.obj("userHeaders" -> testUserHeaders)
      ))
      status(response) mustBe Status.OK
    }
  }
}