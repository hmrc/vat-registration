/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package itutil

import models.api.VatScheme
import org.scalatest._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers._
import reactivemongo.api.commands.WriteResult
import repositories.trafficmanagement.{DailyQuotaRepository, TrafficManagementRepository}
import repositories.{UpscanMongoRepository, VatSchemeRepository}
import utils.TimeMachine

import scala.concurrent.ExecutionContext.Implicits.global

trait IntegrationSpecBase extends PlaySpec
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with DefaultAwaitTimeout {

  val mockUrl: String = WiremockHelper.url
  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: String = WiremockHelper.wiremockPort.toString

  lazy val additionalConfig: Map[String, String] = Map.empty

  lazy val config: Map[String, String] = Map(
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort,
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort,
    "microservice.services.business-registration.host" -> mockHost,
    "microservice.services.business-registration.port" -> mockPort,
    "microservice.services.integration-framework.url" -> mockUrl,
    "microservice.services.integration-framework.environment" -> "local",
    "microservice.services.integration-framework.authorization-token" -> "Bearer FakeToken",
    "microservice.services.company-registration.host" -> mockHost,
    "microservice.services.company-registration.port" -> mockPort,
    "microservice.services.incorporation-information.host" -> mockHost,
    "microservice.services.incorporation-information.port" -> mockPort,
    "microservice.services.incorporation-information.uri" -> "/incorporation-information",
    "microservice.services.ThresholdsJsonLocation" -> "conf/thresholds.json",
    "microservice.services.vat-registration.host" -> mockHost,
    "microservice.services.vat-registration.port" -> mockPort,
    "microservice.services.non-repudiation.host" -> mockHost,
    "microservice.services.non-repudiation.port" -> mockPort,
    "microservice.services.sdes.host" -> mockHost,
    "microservice.services.sdes.port" -> mockPort,
    "mongo-encryption.key" -> "ABCDEFGHIJKLMNOPQRSTUV==",
    "traffic-management.quotas.uk-company-enrolled" -> "11",
    "traffic-management.quotas.uk-company" -> "1",
    "traffic-management.quotas.sole-trader" -> "1",
    "traffic-management.hours.from" -> "9",
    "traffic-management.hours.until" -> "17",
    "traffic-management.quotas.time-to-live" -> "86400"
  ) ++ additionalConfig


  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
    .overrides(bind[TimeMachine].to[FakeTimeMachine])
    .build()

  lazy val repo: VatSchemeRepository = app.injector.instanceOf[VatSchemeRepository]
  lazy val dailyQuotaRepo: DailyQuotaRepository = app.injector.instanceOf[DailyQuotaRepository]
  lazy val trafficManagementRepo: TrafficManagementRepository = app.injector.instanceOf[TrafficManagementRepository]
  lazy val upscanMongoRepository: UpscanMongoRepository = app.injector.instanceOf[UpscanMongoRepository]

  trait SetupHelper {
    await(repo.drop)
    await(repo.ensureIndexes)
    await(dailyQuotaRepo.drop)
    await(dailyQuotaRepo.ensureIndexes)
    await(trafficManagementRepo.drop)
    await(trafficManagementRepo.ensureIndexes)
    await(upscanMongoRepository.drop)
    await(upscanMongoRepository.ensureIndexes)

    def insertIntoDb(vatScheme: VatScheme): WriteResult = {
      implicit val format = VatScheme.format()
      val count = await(repo.count)
      val res = await(repo.insert(vatScheme))
      await(repo.count) mustBe count + 1
      res
    }

    lazy val ws: WSClient = app.injector.instanceOf(classOf[WSClient])

    def client(path: String): WSRequest = ws.url(s"http://localhost:$port/vatreg${path.replace("/vatreg", "")}").withFollowRedirects(false)
  }

  override def beforeEach(): Unit = {
    resetWiremock()
  }

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }
}
