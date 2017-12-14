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

package services

import java.time.LocalDate

import common.RegistrationId
import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.LodgingOfficer
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.Json
import repositories.RegistrationMongoRepository

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class LodgingOfficerServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service = new LodgingOfficerService(
      registrationMongo = mockRegistrationMongo
    ) {
      override val registrationRepository: RegistrationMongoRepository = mockRegistrationMongoRepository
    }

    def upsertToMongo(): OngoingStubbing[Future[LodgingOfficer]] = when(mockRegistrationMongoRepository.updateLodgingOfficer(any(),any())(any()))
      .thenReturn(Future.successful(validLodgingOfficerPostIv))

    def upsertToMongoFail(): OngoingStubbing[Future[LodgingOfficer]] = when(mockRegistrationMongoRepository.updateLodgingOfficer(any(),any())(any()))
      .thenReturn(Future.failed(new Exception("")))

    def upsertToMongoNoRegDoc(): OngoingStubbing[Future[LodgingOfficer]] = when(mockRegistrationMongoRepository.updateLodgingOfficer(any(),any())(any()))
      .thenReturn(Future.failed(MissingRegDocument(RegistrationId("testId"))))

    def getsFromMongo(): OngoingStubbing[Future[Option[LodgingOfficer]]] = when(mockRegistrationMongoRepository.getLodgingOfficer(any())(any()))
      .thenReturn(Future.successful(Some(validLodgingOfficerPreIV)))

    def getsNothingFromMongo(): OngoingStubbing[Future[Option[LodgingOfficer]]] = when(mockRegistrationMongoRepository.getLodgingOfficer(any())(any()))
      .thenReturn(Future.successful(None))
  }

  val upsertLodgingOfficerModel = Json.parse(
    s"""
      |{
      | "name": {
      |   "first" : "Skylake",
      |   "last" : "Valiarm"
      | },
      | "dob" : "${LocalDate.now()}",
      | "nino" : "AB123456A",
      | "role" : "secretary",
      | "ivPassed" : false,
      | "details" : {
      |   "currentAddress" : {
      |     "line1" : "12 Lukewarm",
      |     "line2"  : "Oriental lane"
      |   },
      |   "contact" : {
      |     "email" : "skylake@vilikariet.com"
      |   }
      | }
      |}
    """.stripMargin).as[LodgingOfficer]

  val validLodgingOfficerModel = Json.parse(
    s"""
       |{
       | "name": {
       |   "first" : "Skylake",
       |   "last" : "Valiarm"
       | },
       | "dob" : "${LocalDate.now()}",
       | "nino" : "AB123456A",
       | "role" : "secretary",
       | "ivPassed" : false
       |}
    """.stripMargin).as[LodgingOfficer]

  "updatLodging Officer" should {
    "return the data that is being inputted" in new Setup {
      upsertToMongo()
      val result = await(service.updateLodgingOfficer("regId", upsertLodgingOfficerModel))
      result shouldBe upsertLodgingOfficerModel
    }

    "encounter an exception if an error occurs" in new Setup {
      upsertToMongoFail()
      intercept[Exception](await(service.updateLodgingOfficer("regId", upsertLodgingOfficerModel)))
    }

    "encounter an MissingRegDocument Exception if no docuemnt is found" in new Setup {
      upsertToMongoNoRegDoc()
      intercept[MissingRegDocument](await(service.updateLodgingOfficer("regId", upsertLodgingOfficerModel)))
    }
  }

  "getLodgingOfficer" should {
    "return an Lodging Officer if found" in new Setup {
      getsFromMongo()
      val result = await(service.getLodgingOfficer("regId"))
      result shouldBe Some(validLodgingOfficerModel)
    }

    "return None if none found matching regId" in new Setup {
      getsNothingFromMongo()
      val result = await(service.getLodgingOfficer("regId"))
      result shouldBe None
    }
  }
}
