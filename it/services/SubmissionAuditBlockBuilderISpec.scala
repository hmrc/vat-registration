
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
    bankAccount = Some(BankAccount(isProvided = true, Some(testBankDetails), None, None)),
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
        hasVrn = false,
        vrn = None
      ),
      testOtherBusinessInvolvement.copy(
        hasVrn = false,
        vrn = None,
        hasUtr = Some(false),
        utr = None,
        stillTrading = false
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
