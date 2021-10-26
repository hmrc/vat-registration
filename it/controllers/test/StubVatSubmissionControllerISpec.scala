
package controllers.test

import connectors.stubs.AuditStub.stubAudit
import itutil.{ITVatSubmissionFixture, IntegrationStubbing}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class StubVatSubmissionControllerISpec extends IntegrationStubbing with ITVatSubmissionFixture {

  val testMessageType = "SubmissionCreate"
  val testCustomerStatus = "3"
  val testTradersPartyType = "50"
  val testSafeID = "12345678901234567890"
  val testLine1 = "line1"
  val testLine2 = "line2"
  val testPostCode = "A11 11A"

  class Setup extends SetupHelper()

  "processSubmission" should {
    "return OK if the json is a valid VatSubmission" in new Setup() {
      stubAudit(OK)

      val response: WSResponse = await(client(routes.StubVatSubmissionController.processSubmission.url).post(testSubmissionJson))

      response.status mustBe OK
    }

    "return OK if the json is a valid VatSubmission for a UkCompany with TransactorDetails" in new Setup() {
      stubAudit(OK)

      val response: WSResponse = await(client(routes.StubVatSubmissionController.processSubmission.url).post(testTransactorSubmissionJson))

      response.status mustBe OK
    }

    "return OK if the json is a valid VatSubmission for sole trader" in new Setup() {
      stubAudit(OK)

      val response: WSResponse = await(client(routes.StubVatSubmissionController.processSubmission.url).post(testVerifiedSoleTraderJson))

      response.status mustBe OK
    }

    "return OK if the json is a valid VatSubmission for NETP" in new Setup() {
      stubAudit(OK)

      val response: WSResponse = await(client(routes.StubVatSubmissionController.processSubmission.url).post(testNetpJson))

      response.status mustBe OK
    }

    "return OK if the json is a valid VatSubmission for Non UK Company" in new Setup() {
      stubAudit(OK)

      val response: WSResponse = await(client(routes.StubVatSubmissionController.processSubmission.url).post(testNonUkCompanyJson))

      response.status mustBe OK
    }

    "fail if the json is not a valid VatSubmission" in new Setup() {
      stubAudit(OK)

      val response: WSResponse = await(client(routes.StubVatSubmissionController.processSubmission.url).post(""))

      response.status mustBe UNSUPPORTED_MEDIA_TYPE
    }
  }
}
