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
import play.api.http.HeaderNames
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{WSClient, WSRequest}
import play.api.test.DefaultAwaitTimeout
import play.api.test.Helpers._
import repositories.{UpscanMongoRepository, VatSchemeRepository}
import utils.{IdGenerator, TimeMachine}

trait IntegrationSpecBase extends PlaySpec
  with GuiceOneServerPerSuite with ScalaFutures with IntegrationPatience
  with WiremockHelper with BeforeAndAfterEach with BeforeAndAfterAll with DefaultAwaitTimeout {

  val mockUrl: String = WiremockHelper.url
  val mockHost: String = WiremockHelper.wiremockHost
  val mockPort: String = WiremockHelper.wiremockPort.toString
  val testAuthToken = "testAuthToken"

  lazy val additionalConfig: Map[String, String] = Map.empty

  lazy val config: Map[String, String] = Map(
    "auditing.consumer.baseUri.host" -> mockHost,
    "auditing.consumer.baseUri.port" -> mockPort,
    "microservice.services.auth.host" -> mockHost,
    "microservice.services.auth.port" -> mockPort,
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
    "microservice.services.email.host" -> mockHost,
    "microservice.services.email.port" -> mockPort,
    "mongo-encryption.key" -> "ABCDEFGHIJKLMNOPQRSTUV=="
  ) ++ additionalConfig


  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
    .overrides(bind[TimeMachine].to[FakeTimeMachine])
    .overrides(bind[IdGenerator].to[FakeIdGenerator])
    .build()

  lazy val repo: VatSchemeRepository = app.injector.instanceOf[VatSchemeRepository]
  lazy val upscanMongoRepository: UpscanMongoRepository = app.injector.instanceOf[UpscanMongoRepository]

  trait SetupHelper {
    await(repo.collection.drop.toFuture())
    await(repo.ensureIndexes)
    await(upscanMongoRepository.collection.drop().toFuture())
    await(upscanMongoRepository.ensureIndexes)

    def insertIntoDb(vatScheme: VatScheme) = {
      val count = await(repo.collection.countDocuments().toFuture())
      val res = await(repo.collection.insertOne(vatScheme).toFuture())
      await(repo.collection.countDocuments().toFuture()) mustBe count + 1
      res
    }

    lazy val ws: WSClient = app.injector.instanceOf(classOf[WSClient])

    def client(path: String): WSRequest = ws.url(s"http://localhost:$port/vatreg${path.replace("/vatreg", "")}")
      .withHttpHeaders(HeaderNames.COOKIE -> "test")
      .withHttpHeaders("authorization" -> testAuthToken)
      .withFollowRedirects(false)

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
