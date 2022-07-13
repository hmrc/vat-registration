
package repository

import itutil.{FakeTimeMachine, FutureAssertions, ITFixtures, IntegrationSpecBase}
import models.api._
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.TimeMachine

import java.time.{LocalDate, LocalDateTime, LocalTime}
import scala.concurrent.ExecutionContext.Implicits.global

class TrafficManagementRepositoryISpec extends IntegrationSpecBase with FutureAssertions {

  class Setup(hour: Int = 9) extends SetupHelper with ITFixtures {

    class TimestampMachine extends FakeTimeMachine {
      override val timestamp = LocalDateTime.of(testDate, LocalTime.of(hour, 0))
    }

    implicit lazy val timeApp: Application = new GuiceApplicationBuilder()
      .configure(config)
      .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
      .overrides(bind[TimeMachine].to[TimestampMachine])
      .build()
  }

  val internalId1 = "internalId1"
  val internalId2 = "internalId2"
  val regId1 = "regId1"
  val regId2 = "regId2"
  val testDate1 = LocalDate.parse("2020-01-01")
  val testDate2 = LocalDate.parse("2020-01-02")
  val testDateTime1: LocalDateTime = LocalDateTime.of(testDate1, LocalTime.MIDNIGHT)
  val testDateTime2: LocalDateTime = LocalDateTime.of(testDate2, LocalTime.MIDNIGHT)
  val regInfo1 = RegistrationInformation(internalId1, regId1, Draft, testDate1, VatReg, testDateTime1)
  val regInfo2 = RegistrationInformation(internalId2, regId2, Draft, testDate2, VatReg, testDateTime2)
  implicit val hc = HeaderCarrier()


  "getRegInfoById" must {
    "return the correct information for the internalID/regID when it exists" in new Setup {
      await(trafficManagementRepo.collection.insertMany(Seq(regInfo1, regInfo2)).toFuture())

      val res = await(trafficManagementRepo.getRegInfoById(internalId1, regId1))

      res mustBe Some(regInfo1)
    }
    "return None if a record doesn't exist" in new Setup {
      await(trafficManagementRepo.collection.insertOne(regInfo1).toFuture())

      val res = await(trafficManagementRepo.getRegInfoById(internalId2, regId2))

      res mustBe None
    }
  }

  "upsertRegInfoById" must {
    "Update an existing record" in new Setup {
      await(trafficManagementRepo.collection.insertMany(Seq(regInfo1, regInfo2)).toFuture())

      val res = await(trafficManagementRepo.upsertRegInfoById(internalId2, regId2, Submitted, testDate2, OTRS, testDateTime2))

      res mustBe regInfo2.copy(status = Submitted, channel = OTRS)
    }
    "create a new record where one doesn't exist" in new Setup {
      await(trafficManagementRepo.collection.insertOne(regInfo1).toFuture())

      val res = await(trafficManagementRepo.upsertRegInfoById(internalId2, regId2, Draft, testDate2, VatReg, testDateTime2))

      res mustBe regInfo2
    }
  }

  "Calling deleteRegInfoById(regId)" must {
    "delete the entry to the traffic management repo" in new Setup {
      trafficManagementRepo.collection.insertOne(regInfo1).toFuture().flatMap(_ => trafficManagementRepo.deleteRegInfoById(internalId1, testRegId)) returns true
    }
  }

}
