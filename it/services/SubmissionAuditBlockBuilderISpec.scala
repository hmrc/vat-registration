
package services

import itutil.{IntegrationStubbing, SubmissionAuditFixture}
import models.api.{BankAccount, VatScheme}
import models.monitoring.SubmissionAuditModel
import services.monitoring.SubmissionAuditBlockBuilder
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation

class SubmissionAuditBlockBuilderISpec extends IntegrationStubbing with SubmissionAuditFixture {

  val service = app.injector.instanceOf[SubmissionAuditBlockBuilder]

  override lazy val testFullVatScheme: VatScheme = testVatScheme.copy(
    tradingDetails = Some(testTradingDetails),
    sicAndCompliance = Some(testFullSicAndCompliance),
    businessContact = Some(testFullBusinessContactDetails),
    bankAccount = Some(BankAccount(isProvided = true, Some(testBankDetails), None)),
    flatRateScheme = Some(testFlatRateScheme),
    applicantDetails = Some(testUnregisteredApplicantDetails),
    eligibilitySubmissionData = Some(testEligibilitySubmissionData),
    confirmInformationDeclaration = Some(true),
    returns = Some(testAASReturns),
    nrsSubmissionPayload = Some(testEncodedPayload)
  )

  "buildAuditJson" must {
    "return the correct JSON" in new SetupHelper {
      given
        .user.isAuthorised

      val res = service.buildAuditJson(
        vatScheme = testFullVatScheme,
        authProviderId = testAuthProviderId,
        affinityGroup = Organisation,
        optAgentReferenceNumber = None
      )

      res mustBe SubmissionAuditModel(
        userAnswers = detailBlockAnswers,
        vatScheme = testFullVatScheme,
        authProviderId = testAuthProviderId,
        affinityGroup = Organisation,
        optAgentReferenceNumber = None
      )
    }
  }

}
