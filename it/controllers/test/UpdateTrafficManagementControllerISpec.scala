
package controllers.test

import itutil.{ITVatSubmissionFixture, IntegrationStubbing}
import models.api.DailyQuota
import models.submission.UkCompany
import org.mongodb.scala.model._
import play.api.libs.json.Json
import play.api.test.Helpers._

class UpdateTrafficManagementControllerISpec extends IntegrationStubbing with ITVatSubmissionFixture {

  class Setup extends SetupHelper

  val url = "/vatreg/test-only/api/daily-quota"

  "PUT /test-only/api/daily-quota" must {
    "update the day's quota" in new Setup {
      given.user.isAuthorised
      await(dailyQuotaRepo.collection.insertOne(DailyQuota(testDate, UkCompany, isEnrolled = true, 1)).toFuture())

      val response = await(client(url).put(Json.obj(
        "quota" -> 0,
        "partyType" -> "50",
        "isEnrolled" -> true
      )))

      response.status mustBe OK
      await(dailyQuotaRepo.collection
        .find(Filters.and (
          Filters.equal("date", testDate.toString),
          Filters.equal("partyType", "50"),
          Filters.equal("isEnrolled", true))).toFuture()) mustBe List(DailyQuota(testDate, UkCompany, isEnrolled = true, 0))
    }
    "only affect the current day's quota" in new Setup {
      given.user.isAuthorised
      val yesterday = testDate.minusDays(1)
      await(dailyQuotaRepo.collection.insertMany(Seq(
        DailyQuota(yesterday, UkCompany, isEnrolled = true, 1),
        DailyQuota(testDate, UkCompany, isEnrolled = true, 3)
      )).toFuture())

      await(client(url).put(Json.obj(
        "quota" -> 2,
        "partyType" -> "50",
        "isEnrolled" -> true
      )))

      await(dailyQuotaRepo.collection.find(Filters.equal("date", testDate.toString)).toFuture()) mustBe List(DailyQuota(testDate, UkCompany, isEnrolled = true, 2))
      await(dailyQuotaRepo.collection.find(Filters.equal("date", yesterday.toString)).toFuture()) mustBe List(DailyQuota(yesterday, UkCompany, isEnrolled = true, 1))
    }
    "return BAD request if the user submits an invalid value" in new Setup {
      val response = await(client(url).put(Json.obj("quota" -> "h")))

      response.status mustBe BAD_REQUEST
    }
  }

}
