
package controllers.registrations

import itutil.IntegrationStubbing
import models.api.BankAccount
import models.registration.{BankAccountSectionId, TransactorSectionId}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException

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

      "return OK with decrypted json for a decryptable section" in new SetupHelper {
        val testBankAccount: BankAccount = BankAccount(isProvided = true, Some(testBankDetails), None)

        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(bankAccount = Some(testBankAccount)))

        val res = await(client(url(BankAccountSectionId.key)).get())

        res.status mustBe OK
        res.json mustBe Json.toJson(testBankAccount)
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

  "PUT /registrations/:regId/sections/:sectionId" when {
    "the section exists in the registration" must {
      "replace the existing section with the given JSON and return OK with the updated JSON" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(transactorDetails = Some(testTransactorDetails)))

        val res = await(client(url(testSectionId)).put(
          Json.toJson(testTransactorDetails.copy(personalDetails = Some(testPersonalDetails.copy(trn = Some(testTrn))))))
        )

        res.status mustBe OK
        res.json mustBe Json.toJson(testTransactorDetails.copy(personalDetails = Some(testPersonalDetails.copy(trn = Some(testTrn)))))
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
      "return BAD_REQUEST with JSON for the new section for an invalid answer" in new SetupHelper {
        given.user.isAuthorised
        insertIntoDb(testVatScheme.copy(transactorDetails = Some(testTransactorDetails)))

        val res = await(client(url(testSectionId)).put(Json.obj(
          "isPartOfOrganisation" -> "notBoolean"
        )))

        res.status mustBe BAD_REQUEST
      }
    }
    "the registration doesn't exist" must {
      "return an exception" in new SetupHelper {
        given.user.isAuthorised

        val res = await(client(url(testSectionId)).put(Json.toJson(testTransactorDetails)))

        res.status mustBe INTERNAL_SERVER_ERROR
        res.body mustBe s"[RegistrationSectionController][upsertSection] Unable to upsert section '${TransactorSectionId.key}' for regId '$testRegId'"
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
