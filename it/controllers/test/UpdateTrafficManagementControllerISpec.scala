
package controllers.test

import itutil.{ITVatSubmissionFixture, IntegrationStubbing}
import models.api.DailyQuota
import models.submission.UkCompany
import play.api.libs.json.{JsString, Json}
import play.api.test.Helpers.await
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class UpdateTrafficManagementControllerISpec extends IntegrationStubbing with ITVatSubmissionFixture {

  class Setup extends SetupHelper

  val url = "/vatreg/test-only/api/daily-quota"

  "PUT /test-only/api/daily-quota" must {
    "update the day's quota" in new Setup {
      given.user.isAuthorised
      await(dailyQuotaRepo.insert(DailyQuota(testDate, UkCompany, isEnrolled = true, 1)))

      val response = await(client(url).put(Json.obj(
        "quota" -> 0,
        "partyType" -> "50",
        "isEnrolled" -> true
      )))

      response.status mustBe OK
      await(dailyQuotaRepo.find("date" -> JsString(testDate.toString), "partyType" -> "50", "isEnrolled" -> true)) mustBe List(DailyQuota(testDate, UkCompany, isEnrolled = true, 0))
    }
    "only affect the current day's quota" in new Setup {
      given.user.isAuthorised
      val yesterday = testDate.minusDays(1)
      await(dailyQuotaRepo.bulkInsert(Seq(
        DailyQuota(yesterday, UkCompany, isEnrolled = true, 1),
        DailyQuota(testDate, UkCompany, isEnrolled = true, 3)
      )))

      await(client(url).put(Json.obj(
        "quota" -> 2,
        "partyType" -> "50",
        "isEnrolled" -> true
      )))

      await(dailyQuotaRepo.find("date" -> JsString(testDate.toString))) mustBe List(DailyQuota(testDate, UkCompany, isEnrolled = true, 2))
      await(dailyQuotaRepo.find("date" -> JsString(yesterday.toString))) mustBe List(DailyQuota(yesterday, UkCompany, isEnrolled = true, 1))
    }
    "return BAD request if the user submits an invalid value" in new Setup {
      val response = await(client(url).put(Json.obj("quota" -> "h")))

      response.status mustBe BAD_REQUEST
    }
  }

}
