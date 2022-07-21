
package controllers

import itutil.IntegrationStubbing
import models.api._
import models.submission.UkCompany
import play.api.libs.json.Json
import play.api.test.Helpers._
import org.mongodb.scala.model._

class TrafficManagementControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper

  val testRegInfo = RegistrationInformation(
    internalId = testInternalid,
    registrationId = testRegId,
    status = Draft,
    regStartDate = testDate,
    channel = VatReg,
    lastModified = testDateTime
  )

  "POST /traffic-management/:regId/allocate" must {
    "return CREATED if the user can be allocated" in new Setup {
      given
        .user.isAuthorised
        .dailyQuotaRepo.insertIntoDb(DailyQuota(testDate, UkCompany, isEnrolled = true), dailyQuotaRepo.collection.insertOne)

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

      dailyQuotaRepo.collection.drop

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
        .dailyQuotaRepo.insertIntoDb(DailyQuota(testDate, UkCompany, isEnrolled = true, currentTotal = 11), dailyQuotaRepo.collection.insertOne)

      val res = await(client(controllers.routes.TrafficManagementController.allocate(testRegId).url)
        .post(Json.obj(
          "partyType" -> "50",
          "isEnrolled" -> true
        )))

      res.status mustBe TOO_MANY_REQUESTS
    }
    "return TOO_MANY_REQUESTS if the party type is not supported" in new Setup {
      given
        .user.isAuthorised

      val res = await(client(controllers.routes.TrafficManagementController.allocate(testRegId).url)
        .post(Json.obj(
          "partyType" -> "NETP",
          "isEnrolled" -> true
        )))

      res.status mustBe TOO_MANY_REQUESTS
    }
    "return BAD_REQUEST if the request JSON is malformed" in new Setup {
      given
        .user.isAuthorised
        .dailyQuotaRepo.insertIntoDb(DailyQuota(testDate, UkCompany, isEnrolled = true, currentTotal = 1), dailyQuotaRepo.collection.insertOne)

      val res = await(client(controllers.routes.TrafficManagementController.allocate(testRegId).url).post(Json.obj()))

      res.status mustBe BAD_REQUEST
    }
  }

  "GET /traffic-management/:regId/reg-info" must {
    "return OK with reg info when a record exists for the internal ID" in new Setup {
      given
        .user.isAuthorised
        .regInfoRepo.insertIntoDb(testRegInfo, trafficManagementRepo.collection.insertOne)

      val res = await(client(controllers.routes.TrafficManagementController.getRegInfoById(testRegId).url).get)

      res.status mustBe OK
      res.json mustBe Json.toJson(testRegInfo)
    }

    "return OK with the correct reg info when multiple records exist for the internal ID" in new Setup {
      val testRegId2 = "testRegId2"
      val testRegInfo2 = testRegInfo.copy(registrationId = testRegId2)
      given
        .user.isAuthorised
        .regInfoRepo.insertIntoDb(testRegInfo, trafficManagementRepo.collection.insertOne)
        .regInfoRepo.insertIntoDb(testRegInfo2, trafficManagementRepo.collection.insertOne)

      val res = await(client(controllers.routes.TrafficManagementController.getRegInfoById(testRegId2).url).get)

      res.status mustBe OK
      res.json mustBe Json.toJson(testRegInfo2)
    }

    "return NOT_FOUND when no record exists for the internal ID" in new Setup {
      given.user.isAuthorised

      val res = await(client(controllers.routes.TrafficManagementController.getRegInfoById(testRegId).url).get)

      res.status mustBe NOT_FOUND
    }

    "return FORBIDDEN if the user is not authenticated" in new Setup {
      given.user.isNotAuthorised

      val res = await(client(controllers.routes.TrafficManagementController.getRegInfoById(testRegId).url).get)

      res.status mustBe FORBIDDEN
    }
  }

  "PUT /traffic-management/:regId/reg-info" must {
    "return OK with reg info when all required information is present" in new Setup {
      given
        .user.isAuthorised
        .regInfoRepo.insertIntoDb(testRegInfo, trafficManagementRepo.collection.insertOne)

      val json = Json.toJson(testRegInfo.copy(status = Submitted))

      val res = await(client(controllers.routes.TrafficManagementController.upsertRegInfoById(testRegId).url).put(json))

      res.status mustBe OK
      res.json mustBe json
      await(
        trafficManagementRepo.collection.find(Filters.equal("registrationId", testRegId)).toFuture()
      ).map(_.status) must contain(Submitted)
    }
    "return bad request when required fields are missing" in new Setup {
      given
        .user.isAuthorised
        .regInfoRepo.insertIntoDb(testRegInfo, trafficManagementRepo.collection.insertOne)

      val json = Json.obj()

      val res = await(client(controllers.routes.TrafficManagementController.upsertRegInfoById(testRegId).url).put(json))

      res.status mustBe BAD_REQUEST
      await(
        trafficManagementRepo.collection.find(Filters.equal("registrationId", testRegId)).toFuture()
      ).headOption.map(_.status) must contain(Draft)
    }
    "perform an insert operation if the specified registration doesn't exist" in new Setup {
      given
        .user.isAuthorised


      val json = Json.toJson(testRegInfo)

      val res = await(client(controllers.routes.TrafficManagementController.upsertRegInfoById(testRegId).url).put(json))

      res.status mustBe OK
      res.json mustBe json
      await(
        trafficManagementRepo.collection.find(Filters.equal("registrationId", testRegId)).toFuture()
      ).headOption must contain(testRegInfo)
    }
  }

  "DELETE /traffic-management/:regId/reg-info" must {
    "return NO_CONTENT when successful" in new Setup {
      given
        .user.isAuthorised
        .regInfoRepo.insertIntoDb(testRegInfo, trafficManagementRepo.collection.insertOne)

      val res = await(client(controllers.routes.TrafficManagementController.deleteRegInfoById(testRegId).url).delete())

      res.status mustBe NO_CONTENT
      await(
        trafficManagementRepo.collection.find(Filters.equal("registrationId", testRegId)).toFuture()
      ) mustBe Nil
    }

    "return NO_CONTENT even when the record doesn't exist" in new Setup {
      given
        .user.isAuthorised

      val res = await(client(controllers.routes.TrafficManagementController.deleteRegInfoById(testRegId).url).delete())

      res.status mustBe NO_CONTENT
    }
  }

}
