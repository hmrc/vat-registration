
package controllers.test

import itutil.{IntegrationSpecBase, IntegrationStubbing}
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.submission.SubmissionPayloadBuilder

class RetrieveVatSubmissionControllerISpec extends IntegrationSpecBase with IntegrationStubbing {
  implicit val request: Request[_] = FakeRequest()
  val url = s"/vatreg/test-only/submissions/$testRegId/submission-payload"
  val builder = app.injector.instanceOf[SubmissionPayloadBuilder]

  "/test-only/submissions/:regId/submission-payload" must {
    "return OK with the submission Json" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner)

      val expectedJson = builder.buildSubmissionPayload(testFullVatSchemeWithUnregisteredBusinessPartner)
      val res = client(url).get

      whenReady(res) { result =>
        result.status mustBe OK
        result.json mustBe expectedJson
      }

    }
  }

}
