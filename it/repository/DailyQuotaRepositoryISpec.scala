
package repository

import itutil.{FakeTimeMachine, IntegrationStubbing}
import models.api.DailyQuota
import models.submission.UkCompany
import play.api.libs.json.{JsString, JsValue, Json}
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class DailyQuotaRepositoryISpec extends IntegrationStubbing {

  class Setup(hour: Int = 9) extends SetupHelper {
   FakeTimeMachine.hour = hour
  }

  val testQuota = DailyQuota(testDate, UkCompany, isEnrolled = true)

  "currentTotal" must {
    "return the current total for a party type that we have a quota for" in new Setup {
      given.user.isAuthorised
        .dailyQuotaRepo.insertIntoDb(testQuota.copy(currentTotal = 1), dailyQuotaRepo.insert)

      val res = await(dailyQuotaRepo.currentTotal(UkCompany, isEnrolled = true))

      res mustBe 1
    }
    "return the default quota for a party type that we don't have a quota for" in new Setup {
      given.user.isAuthorised
        .dailyQuotaRepo.insertIntoDb(testQuota, dailyQuotaRepo.insert)

      val res = await(dailyQuotaRepo.currentTotal(UkCompany, isEnrolled = false))

      res mustBe 0
    }
    "return the default quota if there is no record for the current day" in new Setup {
      given.user.isAuthorised
        .dailyQuotaRepo.insertIntoDb(testQuota.copy(date = testDate.minusDays(1), currentTotal = 1), dailyQuotaRepo.insert)

      val res = await(dailyQuotaRepo.currentTotal(UkCompany, isEnrolled = false))

      res mustBe 0
    }
    "convert a daily record in the old format to the new format" in new Setup {
      given.user.isAuthorised
      await(dailyQuotaRepo.insert(DailyQuota(testDate, UkCompany, isEnrolled = true, currentTotal = 10)))
      await(dailyQuotaRepo.findAndUpdate(Json.obj("date" -> testDate.toString), Json.obj("$unset" -> Json.obj("partyType" -> "", "isEnrolled" -> ""))))

      val res = await(dailyQuotaRepo.currentTotal(UkCompany, isEnrolled = true))
      val data = await(dailyQuotaRepo.find("date" -> JsString(testDate.toString)).map(_.headOption))

      res mustBe 10
      data mustBe Some(DailyQuota(testDate, UkCompany, true, 10))
    }
  }

  "incrementTotal" must {
    "increment the quota for the day" in new Setup {
      given.user.isAuthorised
      await(dailyQuotaRepo.bulkInsert(Seq(
        DailyQuota(testDate.minusDays(1), UkCompany, true, 15),
        DailyQuota(testDate, UkCompany, true, 9)))
      )

      val res = await(dailyQuotaRepo.incrementTotal(UkCompany, true))

      res mustBe 10
    }
    "create a new record for the day if one doesn't exist" in new Setup {
      given.user.isAuthorised

      val res = await(dailyQuotaRepo.incrementTotal(UkCompany, true))
      val data = await(dailyQuotaRepo.find("date" -> JsString(testDate.toString)).map(_.headOption))

      res mustBe 1
      data mustBe Some(DailyQuota(testDate, UkCompany, true, 1))
    }
    "convert a daily record in the old format to the new format" in new Setup {
      given.user.isAuthorised
      await(dailyQuotaRepo.insert(DailyQuota(testDate, UkCompany, isEnrolled = true, currentTotal = 1)))
      await(dailyQuotaRepo.findAndUpdate(Json.obj("date" -> testDate.toString), Json.obj("$unset" -> Json.obj("partyType" -> "", "isEnrolled" -> ""))))

      val res = await(dailyQuotaRepo.incrementTotal(UkCompany, isEnrolled = true))
      val data = await(dailyQuotaRepo.find("date" -> JsString(testDate.toString)).map(_.headOption))

      res mustBe 2
      data mustBe Some(DailyQuota(testDate, UkCompany, true, 2))
    }
  }

}
