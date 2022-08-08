
package controllers.registrations

import itutil.IntegrationStubbing
import models.registration.TransactorSectionId
import play.api.libs.json.Json
import play.api.test.Helpers._

class RegistrationSectionControllerISpec extends IntegrationStubbing {

  def url(section: String) = s"/registrations/$testRegId/sections/$section"

  val testSectionId = TransactorSectionId.key

  "GET /registrations/:regId/sections/:sectionId" when {
    "the section exists in the registration" must {
      "return OK with the json for the section" in new SetupHelper {
        given.user.isAuthorised

        insertIntoDb(testVatScheme.copy(transactorDetails = Some(testTransactorDetails)))

        val res = await(client(url(testSectionId)).get())

        res.status mustBe OK
        res.json mustBe Json.toJson(testTransactorDetails)
      }
    }
    "the section doesn't exist in the registration" must {
      "return NOT FOUND" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res = await(client(url("applicant")).get())

        res.status mustBe NOT_FOUND
      }
    }
  }

  "PATCH /registrations/:regId/sections/:sectionId" when {
    "the section exists in the registration" must {
      "update the existing section with a full model and return OK with the updated JSON" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(transactorDetails = Some(testTransactorDetails)))

        val res = await(client(url(testSectionId)).patch(
          Json.toJson(testTransactorDetails.copy(personalDetails = testPersonalDetails.copy(trn = Some(testTrn)))))
        )

        res.status mustBe OK
        res.json mustBe Json.toJson(testTransactorDetails.copy(personalDetails = testPersonalDetails.copy(trn = Some(testTrn))))
      }
    }
    "the section doesn't exist in the registration" must {
      "return OK with JSON for the new section" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res = await(client(url(testSectionId)).patch(Json.toJson(testTransactorDetails)))

        res.status mustBe OK
        res.json mustBe Json.toJson(testTransactorDetails)
      }
      "return OK with JSON for the new section for a single answer" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(transactorDetails = Some(testTransactorDetails)))

        val updatedAnswer = testPersonalDetails.copy(trn = Some(testTrn))
        val res = await(client(url(testSectionId)).patch(Json.obj(
          "personalDetails" -> Json.toJson(updatedAnswer)
        )))

        res.status mustBe OK
        res.json mustBe Json.toJson(testTransactorDetails.copy(personalDetails = updatedAnswer))
      }
    }
  }

  "PUT /registrations/:regId/sections/:sectionId" when {
    "the section exists in the registration" must {
      "replace the existing section with the given JSON and return OK with the updated JSON" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(transactorDetails = Some(testTransactorDetails)))

        val res = await(client(url(testSectionId)).put(
          Json.toJson(testTransactorDetails.copy(personalDetails = testPersonalDetails.copy(trn = Some(testTrn)))))
        )

        res.status mustBe OK
        res.json mustBe Json.toJson(testTransactorDetails.copy(personalDetails = testPersonalDetails.copy(trn = Some(testTrn))))
      }
    }
    "the section doesn't exist in the registration" must {
      "return OK with JSON for the new section" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res = await(client(url(testSectionId)).put(Json.toJson(testTransactorDetails)))

        res.status mustBe OK
        res.json mustBe Json.toJson(testTransactorDetails)
      }
      "return BAD_REQUEST with JSON for the new section for a single answer" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(transactorDetails = Some(testTransactorDetails)))

        val updatedAnswer = testPersonalDetails.copy(trn = Some(testTrn))
        val res = await(client(url(testSectionId)).put(Json.obj(
          "personalDetails" -> Json.toJson(updatedAnswer)
        )))

        res.status mustBe BAD_REQUEST
      }
    }
  }

  "DELETE /registrations/:regId/sections/:sectionId" when {
    "the section exists in the registration" must {
      "return NO CONTENT" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(applicantDetails = Some(testUnregisteredApplicantDetails)))

        val res = await(client(url(testSectionId)).delete())

        res.status mustBe NO_CONTENT
        await(repo.getRegistration(testInternalid, testRegId)) mustBe Some(testVatScheme)
      }
    }
    "the section doesn't exist in the registration" must {
      "return NO CONTENT" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme)

        val res = await(client(url(testSectionId)).delete())

        res.status mustBe NO_CONTENT
        await(repo.getRegistration(testInternalid, testRegId)) mustBe Some(testVatScheme)
      }
    }
  }

}
