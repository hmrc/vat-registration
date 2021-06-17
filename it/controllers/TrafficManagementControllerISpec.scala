
package controllers

import itutil.IntegrationStubbing
import models.api._
import models.submission.UkCompany
import play.api.libs.json.Json
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class TrafficManagementControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper

  val testRegInfo = RegistrationInformation(
    internalId = testInternalid,
    registrationId = testRegId,
    status = Draft,
    regStartDate = testDate,
    channel = VatReg,
    lastModified = testDate
  )

  "POST /traffic-management/:regId/allocate" must {
    "return CREATED if the user can be allocated" in new Setup {
      given
        .user.isAuthorised
        .dailyQuotaRepo.insertIntoDb(DailyQuota(testDate, UkCompany, isEnrolled = true), dailyQuotaRepo.insert)

      val res = await(client(controllers.routes.TrafficManagementController.allocate(testRegId).url)
        .post(Json.obj(
          "partyType" -> "50",
          "isEnrolled" -> true
        )))

      res.status mustBe CREATED
    }
    "return CREATED if there is no quota for the current day" in new Setup {
      given
        .user.isAuthorised

      dailyQuotaRepo.drop

      val res = await(client(controllers.routes.TrafficManagementController.allocate(testRegId).url)
        .post(Json.obj(
          "partyType" -> "50",
          "isEnrolled" -> true
        )))

      res.status mustBe CREATED
    }
    "return TOO_MANY_REQUESTS if the user cannot be allocated" in new Setup {
      given
        .user.isAuthorised
        .dailyQuotaRepo.insertIntoDb(DailyQuota(testDate, UkCompany, isEnrolled = true, currentTotal = 11), dailyQuotaRepo.insert)

      val res = await(client(controllers.routes.TrafficManagementController.allocate(testRegId).url)
        .post(Json.obj(
          "partyType" -> "50",
          "isEnrolled" -> true
        )))

      res.status mustBe TOO_MANY_REQUESTS
    }
    "return BAD_REQUEST if the request JSON is malformed" in new Setup {
      given
        .user.isAuthorised
        .dailyQuotaRepo.insertIntoDb(DailyQuota(testDate, UkCompany, isEnrolled = true, currentTotal = 1), dailyQuotaRepo.insert)

      val res = await(client(controllers.routes.TrafficManagementController.allocate(testRegId).url).post(Json.obj()))

      res.status mustBe BAD_REQUEST
    }
  }

  "GET /traffic-management/reg-info" must {
    "return OK with reg info when a record exists for the internal ID" in new Setup {
      given
        .user.isAuthorised
        .regInfoRepo.insertIntoDb(testRegInfo, trafficManagementRepo.insert)

      val res = await(client(controllers.routes.TrafficManagementController.getRegistrationInformation.url).get)

      res.status mustBe OK
      res.json mustBe Json.toJson(testRegInfo)
    }

    "return NOT_FOUND when no record exists for the internal ID" in new Setup {
      given.user.isAuthorised

      val res = await(client(controllers.routes.TrafficManagementController.getRegistrationInformation.url).get)

      res.status mustBe NOT_FOUND
    }

    "return FORBIDDEN if the user is not authenticated" in new Setup {
      given.user.isNotAuthorised

      val res = await(client(controllers.routes.TrafficManagementController.getRegistrationInformation.url).get)

      res.status mustBe FORBIDDEN
    }
  }

  "PUT /traffic-management/reg-info" must {
    "return OK with reg info" in new Setup {
      given
        .user.isAuthorised
        .regInfoRepo.insertIntoDb(testRegInfo, trafficManagementRepo.insert)

      val json = Json.toJson(testRegInfo)

      val res = await(client(controllers.routes.TrafficManagementController.upsertRegistrationInformation().url).put(json))

      res.status mustBe OK
      res.json mustBe Json.toJson(testRegInfo)
    }

  }

  "DELETE /traffic-management/reg-info/clear" must {
    "return NO_CONTENT when successful" in new Setup {
      given
        .user.isAuthorised
        .regInfoRepo.insertIntoDb(testRegInfo, trafficManagementRepo.insert)

      val res = await(client(controllers.routes.TrafficManagementController.clearDocument().url).delete())

      res.status mustBe NO_CONTENT
    }
  }

}
