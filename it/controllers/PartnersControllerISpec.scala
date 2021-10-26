
package controllers

import enums.VatRegStatus
import itutil.IntegrationStubbing
import models.api.{Partner, VatScheme}
import models.registration.sections.PartnersSection
import models.submission.{Individual, Partnership, UkCompany}
import play.api.libs.json.Json
import play.api.test.Helpers._

class PartnersControllerISpec extends IntegrationStubbing {

  val testSoleTraderPartner = Partner(testSoleTraderEntity, Individual, isLeadPartner = true)
  val testLtdCoPartner = testSoleTraderPartner.copy(details = testLtdCoEntity, partyType = UkCompany)
  val testPartnershipPartner = testSoleTraderPartner.copy(details = testGeneralPartnershipEntity, partyType = Partnership)

  "GET /:regId/partners/:index" must {
    "return OK with partner details if the partner exists" in new SetupHelper {
      val partners = List(testPartnershipPartner, testSoleTraderPartner)
      given.user.isAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft,
        partners = Some(PartnersSection(partners))
      ))

      val result = client(s"/vatreg/$testRegId/partners/1").get()

      whenReady(result) { res =>
        res.status mustBe OK
        res.json mustBe Json.toJson(testPartnershipPartner)
      }
    }
    "return NOT_FOUND if the partner doesn't exist" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft
      ))

      val result = client(s"/vatreg/$testRegId/partners/1").get()

      whenReady(result) { res =>
        res.status mustBe NOT_FOUND
      }
    }
    "return BAD_REQUEST if the user requests an index below 1" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft
      ))

      val result = client(s"/vatreg/$testRegId/partners/0").get()

      whenReady(result) { res =>
        res.status mustBe BAD_REQUEST
      }
    }
    "return FORBIDDEN if the user is not authorised" in new SetupHelper {
      val partners = List(testPartnershipPartner, testSoleTraderPartner)
      given.user.isNotAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft,
        partners = Some(PartnersSection(partners))
      ))

      val result = client(s"/vatreg/$testRegId/partners/1").get()

      whenReady(result) { res =>
        res.status mustBe FORBIDDEN
      }
    }
  }

  "PUT /:regId/partners/:index" must {
    "return CREATED if the JSON contains valid sole trader details" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(VatScheme(testRegId, testInternalid, status = VatRegStatus.draft))

      val result = client(s"/vatreg/$testRegId/partners/1").put(Json.toJson(testSoleTraderPartner))

      whenReady(result) { res =>
        res.status mustBe CREATED
        res.json mustBe Json.toJson(testSoleTraderPartner)
        await(repo.fetchBlock[List[Partner]](testRegId, "partners")) mustBe Some(List(testSoleTraderPartner))
      }
    }
    "return CREATED if the JSON contains valid Limited Company details" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(VatScheme(testRegId, testInternalid, status = VatRegStatus.draft))

      val result = client(s"/vatreg/$testRegId/partners/1").put(Json.toJson(testLtdCoPartner))

      whenReady(result) { res =>
        res.status mustBe CREATED
        res.json mustBe Json.toJson(testLtdCoPartner)
        await(repo.fetchBlock[List[Partner]](testRegId, "partners")) mustBe Some(List(testLtdCoPartner))
      }
    }
    "return CREATED if the JSON contains valid General Partnership details" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(VatScheme(testRegId, testInternalid, status = VatRegStatus.draft))

      val result = client(s"/vatreg/$testRegId/partners/1").put(Json.toJson(testPartnershipPartner))

      whenReady(result) { res =>
        res.status mustBe CREATED
        res.json mustBe Json.toJson(testPartnershipPartner)
        await(repo.fetchBlock[List[Partner]](testRegId, "partners")) mustBe Some(List(testPartnershipPartner))
      }
    }
    "add a new partner and return CREATED" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft,
        partners = Some(PartnersSection(List(testPartnershipPartner)))
      ))

      val result = client(s"/vatreg/$testRegId/partners/2").put(Json.toJson(testSoleTraderPartner))

      whenReady(result) { res =>
        res.status mustBe CREATED
        res.json mustBe Json.toJson(testSoleTraderPartner)
        await(repo.fetchBlock[PartnersSection](testRegId, "partners")) mustBe Some(PartnersSection(List(testPartnershipPartner, testSoleTraderPartner)))
      }
    }
    "Update an specific entry and not alter the rest" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft,
        partners = Some(PartnersSection(List(testPartnershipPartner, testSoleTraderPartner)))
      ))

      val result = client(s"/vatreg/$testRegId/partners/1").put(Json.toJson(testLtdCoPartner))

      whenReady(result) { res =>
        res.status mustBe CREATED
        res.json mustBe Json.toJson(testLtdCoPartner)
        await(repo.fetchBlock[PartnersSection](testRegId, "partners")) mustBe Some(PartnersSection(List(testLtdCoPartner, testSoleTraderPartner)))
      }
    }
    "return BAD_REQUEST if the json isn't valid" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(VatScheme(testRegId, testInternalid, status = VatRegStatus.draft))

      val result = client(s"/vatreg/$testRegId/partners/1").put(Json.obj())

      whenReady(result) { res =>
        res.status mustBe BAD_REQUEST
        await(repo.fetchBlock[List[Partner]](testRegId, "partners")) mustBe None
      }
    }
    "return BAD_REQUEST if the user requests an index below 1" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft
      ))

      val result = client(s"/vatreg/$testRegId/partners/0").put(Json.toJson(testPartnershipPartner))

      whenReady(result) { res =>
        res.status mustBe BAD_REQUEST
      }
    }
    "return FORBIDDEN if the user is not authorised" in new SetupHelper {
      val partners = List(testPartnershipPartner, testSoleTraderPartner)
      given.user.isNotAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft,
        partners = Some(PartnersSection(partners))
      ))

      val result = client(s"/vatreg/$testRegId/partners/1").put(Json.toJson(testSoleTraderPartner))

      whenReady(result) { res =>
        res.status mustBe FORBIDDEN
      }
    }
  }

  "GET /:regId/partners" must {
    "return OK with partner JSON for all partners" in new SetupHelper {
      val partners = List(testPartnershipPartner, testSoleTraderPartner)
      given.user.isAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft,
        partners = Some(PartnersSection(partners))
      ))

      val result = client(s"/vatreg/$testRegId/partners").get()

      whenReady(result) { res =>
        res.status mustBe OK
        res.json mustBe Json.toJson(partners)
      }
    }
    "return NOT_FOUND when there are no partners" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft
      ))

      val result = client(s"/vatreg/$testRegId/partners").get()

      whenReady(result) { res =>
        res.status mustBe NOT_FOUND
      }
    }
    "return FORBIDDEN if the user is not authorised" in new SetupHelper {
      val partners = List(testPartnershipPartner, testSoleTraderPartner)
      given.user.isNotAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft,
        partners = Some(PartnersSection(partners))
      ))

      val result = client(s"/vatreg/$testRegId/partners").get()

      whenReady(result) { res =>
        res.status mustBe FORBIDDEN
      }
    }
  }

  "DELETE /:regId/partners/:index" must {
    "remove the correct partner and return NO_CONTENT" in new SetupHelper {
      val partners = List(testPartnershipPartner, testSoleTraderPartner, testLtdCoPartner)
      given.user.isAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft,
        partners = Some(PartnersSection(partners))
      ))

      val result = client(s"/vatreg/$testRegId/partners/2").delete()

      whenReady(result) { res =>
        res.status mustBe NO_CONTENT
        await(repo.fetchBlock[PartnersSection](testRegId, "partners")) mustBe Some(PartnersSection(List(testPartnershipPartner, testLtdCoPartner)))
      }
    }
    "affect nothing and return NO_CONTENT if the partner ID is outside the range of what's in the repo" in new SetupHelper {
      val partners = List(testPartnershipPartner, testSoleTraderPartner, testLtdCoPartner)
      given.user.isAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft,
        partners = Some(PartnersSection(partners))
      ))

      val result = client(s"/vatreg/$testRegId/partners/4").delete()

      whenReady(result) { res =>
        res.status mustBe NO_CONTENT
        await(repo.fetchBlock[PartnersSection](testRegId, "partners")) mustBe Some(PartnersSection(partners))
      }
    }
    "affect nothing and return NO_CONTENT if there are no partners" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft
      ))

      val result = client(s"/vatreg/$testRegId/partners/4").delete()

      whenReady(result) { res =>
        res.status mustBe NO_CONTENT
        await(repo.fetchBlock[List[Partner]](testRegId, "partners")) mustBe Some(Nil)
      }
    }
    "return BAD_REQUEST if the user requests an index below 1" in new SetupHelper {
      given.user.isAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft
      ))

      val result = client(s"/vatreg/$testRegId/partners/0").delete()

      whenReady(result) { res =>
        res.status mustBe BAD_REQUEST
      }
    }
    "return FORBIDDEN if the user is not authorised" in new SetupHelper {
      val partners = List(testPartnershipPartner, testSoleTraderPartner)
      given.user.isNotAuthorised
      insertIntoDb(VatScheme(
        id = testRegId,
        internalId = testInternalid,
        status = VatRegStatus.draft,
        partners = Some(PartnersSection(partners))
      ))

      val result = client(s"/vatreg/$testRegId/partners/1").delete()

      whenReady(result) { res =>
        res.status mustBe FORBIDDEN
      }
    }
  }

}
