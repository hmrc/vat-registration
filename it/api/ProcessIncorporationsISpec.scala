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

package api

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlMatching}
import common.RegistrationId
import enums.VatRegStatus
import itutil.{ITFixtures, IntegrationStubbing, WiremockHelper}
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.FakeApplication
import play.api.test.Helpers._
import play.modules.reactivemongo.ReactiveMongoComponent
import repositories.{RegistrationMongo, RegistrationMongoRepository, SequenceMongo, SequenceMongoRepository}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ProcessIncorporationsISpec extends IntegrationStubbing with ITFixtures {

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl  = s"http://$mockHost:$mockPort"

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
    "auditing.consumer.baseUri.host" -> s"$mockHost",
    "auditing.consumer.baseUri.port" -> s"$mockPort",
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort",
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.des-stub.host" -> s"$mockHost",
    "microservice.services.des-stub.port" -> s"$mockPort",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "microservice.services.incorporation-information.uri" -> "/incorporation-information"
  ))

  lazy val reactiveMongoComponent = app.injector.instanceOf[ReactiveMongoComponent]
  lazy val ws   = app.injector.instanceOf(classOf[WSClient])

  private def client(path: String) = ws.url(s"http://localhost:$port$path").withFollowRedirects(false)

  class Setup {
    val mongo = new RegistrationMongo(reactiveMongoComponent)
    val sequenceMongo = new SequenceMongo(reactiveMongoComponent)
    val repo: RegistrationMongoRepository = mongo.store
    val sequenceRepository: SequenceMongoRepository = sequenceMongo.store

    await(repo.drop)
    await(repo.ensureIndexes)
    await(sequenceRepository.drop)
    await(sequenceRepository.ensureIndexes)
  }

  "/incorporation-data" should {
    val transactionId: String = "transId"
    val incorpDate: DateTime = DateTime.now
    val registrationID = "regId"
    val regIDCase = RegistrationId(registrationID)

    def prepareHeldSubmission(repo : RegistrationMongoRepository): Future[Unit] = {
      for {
        _    <- repo.createNewVatScheme(regIDCase)
        _    <- repo.updateLogicalGroup(regIDCase, returns)
        _    <- repo.saveTransId(transactionId, regIDCase)
        _    <- repo.finishRegistrationSubmission(regIDCase, VatRegStatus.held)
      } yield {
        ()
      }
    }

    "return an Ok if the accepted top up succeeds" in new Setup() {
      stubFor(post(urlMatching(s"/business-registration/value-added-tax"))
        .willReturn(
          aResponse()
            .withStatus(202)
        )
      )

      await(prepareHeldSubmission(repo))

      val json = Json.parse(
        s"""
           |{
           |  "SCRSIncorpStatus":{
           |    "IncorpSubscriptionKey":{
           |      "subscriber":"scrs",
           |      "discriminator":"vat",
           |      "transactionId":"$transactionId"
           |    },
           |    "SCRSIncorpSubscription":{
           |      "callbackUrl":"http://localhost:9896/TODO-CHANGE-THIS"
           |    },
           |    "IncorpStatusEvent":{
           |      "status":"accepted",
           |      "timestamp":1501061996345
           |    }
           |  }
           |}
        """.stripMargin)

      val result = await(client(
        controllers.routes.ProcessIncorporationsController.processIncorp().url).post(json))

      result.status shouldBe OK

      val reg = await(repo.retrieveVatScheme(RegistrationId(registrationID)))
      reg.get.status shouldBe VatRegStatus.submitted

      await(repo.remove("registrationId" -> registrationID))

    }

    "return an Ok if the rejected top up succeeds" in new Setup() {
      stubFor(post(urlMatching(s"/business-registration/value-added-tax"))
        .willReturn(
          aResponse()
            .withStatus(202)
        )
      )

      await(prepareHeldSubmission(repo))

      val json = Json.parse(
        s"""
           |{
           |  "SCRSIncorpStatus":{
           |    "IncorpSubscriptionKey":{
           |      "subscriber":"scrs",
           |      "discriminator":"vat",
           |      "transactionId":"$transactionId"
           |    },
           |    "SCRSIncorpSubscription":{
           |      "callbackUrl":"http://localhost:9896/TODO-CHANGE-THIS"
           |    },
           |    "IncorpStatusEvent":{
           |      "status":"rejected",
           |      "timestamp":1501061996345
           |    }
           |  }
           |}
        """.stripMargin)

      val result = await(client(
        controllers.routes.ProcessIncorporationsController.processIncorp().url).post(json))

      result.status shouldBe OK

      val reg = await(repo.retrieveVatScheme(RegistrationId(registrationID)))
      reg.get.status shouldBe VatRegStatus.rejected

      await(repo.remove("registrationId" -> registrationID))
    }

    "return a 400 status when DES returns a 4xx" in new Setup() {
      stubFor(post(urlMatching(s"/business-registration/value-added-tax"))
        .willReturn(
          aResponse()
            .withStatus(433)
        )
      )

      await(prepareHeldSubmission(repo))

      val json = Json.parse(
        s"""
           |{
           |  "SCRSIncorpStatus":{
           |    "IncorpSubscriptionKey":{
           |      "subscriber":"scrs",
           |      "discriminator":"vat",
           |      "transactionId":"$transactionId"
           |    },
           |    "SCRSIncorpSubscription":{
           |      "callbackUrl":"http://localhost:9896/TODO-CHANGE-THIS"
           |    },
           |    "IncorpStatusEvent":{
           |      "status":"accepted",
           |      "timestamp":1501061996345
           |    }
           |  }
           |}
        """.stripMargin)

      val result = await(client(
        controllers.routes.ProcessIncorporationsController.processIncorp().url).post(json))

      result.status shouldBe 400

      val reg = await(repo.retrieveVatScheme(RegistrationId(registrationID)))
      reg.get.status shouldBe VatRegStatus.held

      await(repo.remove("registrationId" -> registrationID))
    }
  }

}
