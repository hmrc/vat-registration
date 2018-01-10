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

import java.time.LocalDate

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import common.RegistrationId
import enums.VatRegStatus
import itutil.{ITFixtures, IntegrationStubbing, WiremockHelper}
import models.api.VatScheme
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.FakeApplication
import play.api.test.Helpers._
import play.modules.reactivemongo.ReactiveMongoComponent
import repositories.{RegistrationMongo, RegistrationMongoRepository, SequenceMongo, SequenceMongoRepository}

import scala.concurrent.ExecutionContext.Implicits.global

class VatRegistrationBasicISpec extends IntegrationStubbing with ITFixtures {

  //implicit val hc: HeaderCarrier = HeaderCarrier()

  val mockHost = WiremockHelper.wiremockHost
  val mockPort = WiremockHelper.wiremockPort
  val mockUrl  = s"http://$mockHost:$mockPort"

  override implicit lazy val app = FakeApplication(additionalConfiguration = Map(
    "auditing.consumer.baseUri.host" -> s"$mockHost",
    "auditing.consumer.baseUri.port" -> s"$mockPort",
    "microservice.services.auth.host" -> s"$mockHost",
    "microservice.services.auth.port" -> s"$mockPort",
    "microservice.services.des-stub.host" -> s"$mockHost",
    "microservice.services.des-stub.port" -> s"$mockPort",
    "microservice.services.company-registration.host" -> s"$mockHost",
    "microservice.services.company-registration.port" -> s"$mockPort",
    "microservice.services.incorporation-information.host" -> s"$mockHost",
    "microservice.services.incorporation-information.port" -> s"$mockPort",
    "microservice.services.incorporation-information.uri" -> "/incorporation-information"
  ))

  lazy val reactiveMongoComponent = app.injector.instanceOf[ReactiveMongoComponent]
  lazy val ws   = app.injector.instanceOf(classOf[WSClient])

  private def client(path: String) = ws.url(s"http://localhost:$port$path").withFollowRedirects(false)

  val transID = "transID"
  val crn = "crn"
  val accepted = "accepted"

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

  def incorpUpdate(status: String) = {
    s"""
       |{
       |  "SCRSIncorpStatus": {
       |    "IncorpSubscriptionKey" : {
       |      "subscriber" : "SCRS",
       |      "discriminator" : "PAYE",
       |      "transactionId" : "$transID"
       |    },
       |    "SCRSIncorpSubscription" : {
       |      "callbackUrl" : "scrs-incorporation-update-listener.service/incorp-updates/incorp-status-update"
       |    },
       |    "IncorpStatusEvent": {
       |      "status": "$status",
       |      "crn":"$crn",
       |      "incorporationDate":1470351600000,
       |      "timestamp" : ${Json.toJson(LocalDate.of(2017, 12, 21))}
       |    }
       |  }
       |}
        """.stripMargin
  }

  val returnedFromII =
    s"""
       |{
       |  "company_name":"test"
       |}
        """.stripMargin

  "VAT Registration API - for initial / basic calls" should {

    "Return a 200 for " in {
      given
        .user.isAuthorised

      client(controllers.routes.VatRegistrationController.newVatRegistration().url).post("test") map { response =>
        response.status shouldBe OK
        response.json shouldBe Json.parse("""{"uri":"xxx","gatewayId":"xxx2","userDetailsLink":"xxx3","ids":{"internalId":"Int-xxx","externalId":"Ext-xxx"}}""")
      }
    }

    "Return a 403 for " in {
      client(controllers.routes.VatRegistrationController.newVatRegistration().url).post("test") map { response =>
        response.status shouldBe FORBIDDEN
      }
    }

    "Return a 404 if the registration is missing" in {
      client(s"/12345").post("test") map {
        _.status shouldBe NOT_FOUND
      }
    }
  }

  "/:regId/update-iv-status" should {
    "return an Ok" in new Setup() {
      given
        .user.isAuthorised

      repo.createNewVatScheme(RegistrationId("testRegId"))
      repo.updateLodgingOfficer("testRegId", vatLodgingOfficer)

      val result = await(client(controllers.routes.VatRegistrationController.updateIVStatus("testRegId").url).patch(Json.parse("""{"ivPassed" : true}""")))
      result.status shouldBe OK

      await(repo.remove("registrationId" -> "testRegId"))
    }

    "return an INS" in {
      given
        .user.isAuthorised

      val result = await(client("/vatreg/testRegId2/update-iv-status").patch(Json.parse("""{"ivPassed" : true}""")))
      result.status shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "/:regId/submit-registration" should {
    val registrationID = "testRegId"
    val regime = "vat"
    val subscriber = "scrs"

    def mockGetTransID() : StubMapping =
      stubFor(get(urlMatching(s"/company-registration/corporation-tax-registration/$registrationID/corporation-tax-registration"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(
              s"""{
                 | "confirmationReferences" : {
                 |   "transaction-id" : "$transID"
                 | }
                 |}
                 |""".stripMargin
            )
        )
      )

    def mockIncorpUpdate(): StubMapping =
      stubFor(post(urlMatching(s"/incorporation-information/subscribe/$transID/regime/$regime/subscriber/$subscriber"))
      .willReturn(
        aResponse()
          .withStatus(200)
          .withBody(incorpUpdate(accepted))
      )
    )

    def mockNoIncorpUpdate(): StubMapping =
      stubFor(post(urlMatching(s"/incorporation-information/subscribe/$transID/regime/$regime/subscriber/$subscriber"))
        .willReturn(
          aResponse()
            .withStatus(202)
        )
      )

    def mockGetCompanyProfile(): StubMapping =
      stubFor(get(urlMatching(s"/incorporation-information/$transID/company-profile"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(returnedFromII)
        )
      )

    "return an Ok if the submission is successful for the regID" in new Setup() {
      given
        .user.isAuthorised

      System.setProperty("feature.mockSubmission", "false")

      mockGetTransID()
      mockIncorpUpdate()
      mockGetCompanyProfile()

      stubFor(post(urlMatching(s"/business-registration/value-added-tax"))
        .willReturn(
          aResponse()
            .withStatus(202)
        )
      )

      repo.createNewVatScheme(RegistrationId(registrationID)).flatMap(_ => repo.updateLogicalGroup(RegistrationId(registrationID), returns))

      val result: WSResponse = await(client(
        controllers.routes.VatRegistrationController.submitVATRegistration(RegistrationId(registrationID)).url).put("")
      )
      result.status shouldBe OK

      val reg: Option[VatScheme] = await(repo.retrieveVatScheme(RegistrationId(registrationID)))
      reg.get.status shouldBe VatRegStatus.submitted

      await(repo.remove("registrationId" -> registrationID))
    }

    "return an Ok if the submission is successful for a partial unincorped company regID" in new Setup() {
      given
        .user.isAuthorised

      System.setProperty("feature.mockSubmission", "false")

      mockGetTransID()
      mockNoIncorpUpdate()
      mockGetCompanyProfile()

      stubFor(post(urlMatching(s"/business-registration/value-added-tax"))
        .willReturn(
          aResponse()
            .withStatus(202)
        )
      )

      repo.createNewVatScheme(RegistrationId(registrationID)).flatMap(_ => repo.updateLogicalGroup(RegistrationId(registrationID), tradingDetails))

      val result = await(client(
        controllers.routes.VatRegistrationController.submitVATRegistration(RegistrationId(registrationID)).url).put("")
      )
      result.status shouldBe OK

      val reg = await(repo.retrieveVatScheme(RegistrationId(registrationID)))
      reg.get.status shouldBe VatRegStatus.held

      await(repo.remove("registrationId" -> registrationID))
    }

    "return a 400 status when DES returns a 4xx" in new Setup() {
      given
        .user.isAuthorised

      System.setProperty("feature.mockSubmission", "false")

      mockGetTransID()
      mockIncorpUpdate()
      mockGetCompanyProfile()

      stubFor(post(urlMatching(s"/business-registration/value-added-tax"))
        .willReturn(
          aResponse()
            .withStatus(433)
        )
      )

      repo.createNewVatScheme(RegistrationId(registrationID)).flatMap(_ => repo.updateLogicalGroup(RegistrationId(registrationID), tradingDetails))

      val result = await(client(
        controllers.routes.VatRegistrationController.submitVATRegistration(RegistrationId(registrationID)).url).put("")
      )
      result.status shouldBe 400

      val reg = await(repo.retrieveVatScheme(RegistrationId(registrationID)))
      reg.get.status shouldBe VatRegStatus.draft

      await(repo.remove("registrationId" -> registrationID))
    }

    "mock the return if the mock submission flag is on" in {
      given
        .user.isAuthorised

      System.setProperty("feature.mockSubmission", "true")

      val result = await(client(
        controllers.routes.VatRegistrationController.submitVATRegistration(RegistrationId(registrationID)).url).put("")
      )
      result.status shouldBe OK
    }
  }
}