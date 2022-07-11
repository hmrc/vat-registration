
package repository

import itutil.{FakeTimeMachine, IntegrationStubbing}
import models.api.DailyQuota
import models.submission.UkCompany
import org.mongodb.scala.model._
import play.api.test.Helpers._

class DailyQuotaRepositoryISpec extends IntegrationStubbing {

  class Setup(hour: Int = 9) extends SetupHelper {
    FakeTimeMachine.hour = hour
  }

  val testQuota = DailyQuota(testDate, UkCompany, isEnrolled = true)

  "currentTotal" must {
    "return the current total for a party type that we have a quota for" in new Setup {
      given.user.isAuthorised
        .dailyQuotaRepo.insertIntoDb(testQuota.copy(currentTotal = 1), dailyQuotaRepo.collection.insertOne)

      val res = await(dailyQuotaRepo.currentTotal(UkCompany, isEnrolled = true))

      res mustBe 1
    }
    "return the default quota for a party type that we don't have a quota for" in new Setup {
      given.user.isAuthorised
        .dailyQuotaRepo.insertIntoDb(testQuota, dailyQuotaRepo.collection.insertOne)

      val res = await(dailyQuotaRepo.currentTotal(UkCompany, isEnrolled = false))

      res mustBe 0
    }
    "return the default quota if there is no record for the current day" in new Setup {
      given.user.isAuthorised
        .dailyQuotaRepo.insertIntoDb(testQuota.copy(date = testDate.minusDays(1), currentTotal = 1), dailyQuotaRepo.collection.insertOne)

      val res = await(dailyQuotaRepo.currentTotal(UkCompany, isEnrolled = false))

      res mustBe 0
    }
  }

  "incrementTotal" must {
    "increment the quota for the day" in new Setup {
      given.user.isAuthorised
      await(dailyQuotaRepo.collection.insertMany(Seq(
        DailyQuota(testDate.minusDays(1), UkCompany, true, 15),
        DailyQuota(testDate, UkCompany, true, 9))).toFuture()
      )

      val res = await(dailyQuotaRepo.incrementTotal(UkCompany, true))

      res mustBe 10
    }
    "create a new record for the day if one doesn't exist" in new Setup {
      given.user.isAuthorised

      val res = await(dailyQuotaRepo.incrementTotal(UkCompany, true))
      val data = await(dailyQuotaRepo.collection.find(Filters.equal("date", testDate.toString)).head())

      res mustBe 1
      data mustBe DailyQuota(testDate, UkCompany, true, 1)
    }
  }

}
