
package controllers.test

import itutil.{ITVatSubmissionFixture, IntegrationStubbing}
import models.api.DailyQuota
import play.api.libs.json.{JsString, Json}
import play.api.test.Helpers.{await, _}

import scala.concurrent.ExecutionContext.Implicits.global

class UpdateTrafficManagementControllerISpec extends IntegrationStubbing with ITVatSubmissionFixture {

  class Setup extends SetupHelper

  val url = "/vatreg/test-only/api/daily-quota"

  "PUT /test-only/api/daily-quota" must {
    "update the day's quota" in new Setup {
      given.user.isAuthorised
      await(dailyQuotaRepo.insert(DailyQuota(testDate, 1)))

      val response = await(client(url).put(Json.obj("quota" -> 0)))

      response.status mustBe OK
      await(dailyQuotaRepo.find("date" -> JsString(testDate.toString))) mustBe List(DailyQuota(testDate))
    }
    "only affect the current day's quota" in new Setup {
      given.user.isAuthorised
      val yesterday = testDate.minusDays(1)
      await(dailyQuotaRepo.bulkInsert(Seq(
        DailyQuota(yesterday, 1),
        DailyQuota(testDate, 3)
      )))

      await(client(url).put(Json.obj("quota" -> 2)))

      await(dailyQuotaRepo.find("date" -> JsString(testDate.toString))) mustBe List(DailyQuota(testDate, 2))
      await(dailyQuotaRepo.find("date" -> JsString(yesterday.toString))) mustBe List(DailyQuota(yesterday, 1))
    }
    "return BAD request if the user submits an invalid value" in new Setup {
      val response = await(client(url).put(Json.obj("quota" -> "h")))

      response.status mustBe BAD_REQUEST
    }
  }

}
