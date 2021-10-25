
package controllers

import com.github.tomakehurst.wiremock.stubbing.StubMapping
import itutil.IntegrationStubbing
import models.submission.RoleInTheBusiness
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

class ApplicantDetailsControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper {
    def writeAudit: StubMapping = stubPost("/write/audit/merged", 200, "")
  }

  val testApplicantDetailsJson = Json.toJson(testUnregisteredApplicantDetails)

  val invalidTestApplicantDetailsJson = Json.obj(
    "nino" -> testNino,
    "role" -> Json.toJson[RoleInTheBusiness](testRole)
  )

  "getApplicantDetailsData" must {
    "return OK" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme(testRegId).copy(
        eligibilitySubmissionData = Some(testEligibilitySubmissionData),
        applicantDetails = Some(testUnregisteredApplicantDetails))
      )

      val response: WSResponse = await(client(routes.ApplicantDetailsController.getApplicantDetailsData(testRegId).url).get())

      response.status mustBe OK
      response.json mustBe testApplicantDetailsJson
    }

    "return NO_CONTENT" in new Setup {
      given.user.isAuthorised

      insertIntoDb(testEmptyVatScheme(testRegId).copy(eligibilitySubmissionData = Some(testEligibilitySubmissionData)))

      val response: WSResponse = await(client(routes.ApplicantDetailsController.getApplicantDetailsData(testRegId).url).get())

      response.status mustBe NO_CONTENT
    }

    "return NOT_FOUND if no document found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(routes.ApplicantDetailsController.getApplicantDetailsData(testRegId).url).get())

      response.status mustBe NOT_FOUND
    }

    "return FORBIDDEN if user is not authorised" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(routes.ApplicantDetailsController.getApplicantDetailsData(testRegId).url).get())

      response.status mustBe FORBIDDEN
    }
  }

  "updateApplicantDetailsData" must {
    "return OK with a applicantDetails json body" in new Setup {
      given.user.isAuthorised
      insertIntoDb(testEmptyVatScheme(testRegId).copy(
        eligibilitySubmissionData = Some(testEligibilitySubmissionData)
      ))

      val response: WSResponse = await(client(routes.ApplicantDetailsController.updateApplicantDetailsData(testRegId).url)
        .patch(testApplicantDetailsJson))

      response.status mustBe OK
      response.json mustBe testApplicantDetailsJson
    }

    "return BAD_REQUEST if an invalid json body is posted" in new Setup {
      given.user.isAuthorised
      insertIntoDb(testEmptyVatScheme(testRegId).copy(
        eligibilitySubmissionData = Some(testEligibilitySubmissionData)
      ))

      val response: WSResponse = await(client(routes.ApplicantDetailsController.updateApplicantDetailsData(testRegId).url)
        .patch(invalidTestApplicantDetailsJson))

      response.status mustBe BAD_REQUEST
    }

    "return NOT_FOUND if no reg document is found" in new Setup {
      given.user.isAuthorised

      val response: WSResponse = await(client(routes.ApplicantDetailsController.updateApplicantDetailsData(testRegId).url)
        .patch(testApplicantDetailsJson))

      response.status mustBe NOT_FOUND
    }

    "return OK if no data updated because data is same" in new Setup {
      given.user.isAuthorised
      val scheme = testEmptyVatScheme(testRegId).copy(
        eligibilitySubmissionData = Some(testEligibilitySubmissionData),
        applicantDetails = Some(testUnregisteredApplicantDetails)
      )
      insertIntoDb(scheme)

      val response: WSResponse = await(client(routes.ApplicantDetailsController.updateApplicantDetailsData(testRegId).url)
        .patch(testApplicantDetailsJson))

      response.status mustBe OK
    }

    "return FORBIDDEN if user is not authorised obtained" in new Setup {
      given.user.isNotAuthorised

      val response: WSResponse = await(client(routes.ApplicantDetailsController.updateApplicantDetailsData(testRegId).url)
        .patch(testApplicantDetailsJson))

      response.status mustBe FORBIDDEN
    }
  }

}
