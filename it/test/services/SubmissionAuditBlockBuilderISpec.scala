/*
 * Copyright 2024 HM Revenue & Customs
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

import itutil.{IntegrationStubbing, SubmissionAuditFixture}
import models.api.{BankAccount, VatScheme}
import models.monitoring.SubmissionAuditModel
import play.api.mvc.Request
import play.api.test.FakeRequest
import services.monitoring.SubmissionAuditBlockBuilder
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation

class SubmissionAuditBlockBuilderISpec extends IntegrationStubbing with SubmissionAuditFixture {

  val service = app.injector.instanceOf[SubmissionAuditBlockBuilder]

  implicit val request: Request[_] = FakeRequest()

  override lazy val testFullVatScheme: VatScheme = testVatScheme.copy(
    bankAccount = Some(BankAccount(isProvided = true, Some(testBankDetails), None)),
    flatRateScheme = Some(testFlatRateScheme),
    applicantDetails = Some(testUnregisteredApplicantDetails),
    eligibilitySubmissionData = Some(testEligibilitySubmissionData),
    confirmInformationDeclaration = Some(true),
    vatApplication = Some(testAASVatApplicationDetails),
    nrsSubmissionPayload = Some(testEncodedPayload),
    otherBusinessInvolvements = Some(List(
      testOtherBusinessInvolvement.copy(
        hasUtr = None,
        utr = None
      ),
      testOtherBusinessInvolvement.copy(
        hasVrn = Some(false),
        vrn = None
      ),
      testOtherBusinessInvolvement.copy(
        hasVrn = Some(false),
        vrn = None,
        hasUtr = Some(false),
        utr = None,
        stillTrading = Some(false)
      )
    )),
    business = Some(testBusiness.copy(otherBusinessInvolvement = Some(true)))
  )

  "buildAuditJson" must {
    "return the correct JSON" in new SetupHelper {
      given
        .user.isAuthorised

      val res = service.buildAuditJson(
        vatScheme = testFullVatScheme,
        authProviderId = testAuthProviderId,
        affinityGroup = Organisation,
        optAgentReferenceNumber = None,
        testFormBundleId
      )

      res mustBe SubmissionAuditModel(
        userAnswers = detailBlockAnswers,
        vatScheme = testFullVatScheme,
        authProviderId = testAuthProviderId,
        affinityGroup = Organisation,
        optAgentReferenceNumber = None,
        testFormBundleId
      )
    }
  }

}
