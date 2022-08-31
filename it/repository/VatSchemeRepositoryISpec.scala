/*
 * Copyright 2016 HM Revenue & Customs
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

package repository

import enums.VatRegStatus
import itutil._
import models.api._
import models.api.vatapplication._
import org.mongodb.scala.MongoWriteException
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.mongodb.scala.result.InsertOneResult
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json._
import play.api.test.Helpers._
import repositories.VatSchemeRepository
import utils.TimeMachine

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class VatSchemeRepositoryISpec extends MongoBaseSpec with IntegrationStubbing with FutureAssertions with ITFixtures {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .overrides(bind[TimeMachine].to[FakeTimeMachine])
    .build()

  class Setup {
    val repository: VatSchemeRepository = app.injector.instanceOf[VatSchemeRepository]

    def insert(vatScheme: VatScheme): Future[InsertOneResult] = repository.collection.insertOne(vatScheme).toFuture()

    def count: Int = await(repository.collection.countDocuments().toFuture().map(_.toInt))

    def getRegistration: Option[VatScheme] = await(repository.collection.find(mongoEqual("registrationId", testRegId)).headOption())

    await(repository.collection.drop().toFuture())
    await(repository.ensureIndexes)
  }

  val ACK_REF_NUM = "REF0000001"
  val registrationId: String = "reg-12345"
  val otherRegId = "other-reg-12345"
  val jsonEligiblityData = Json.obj("foo" -> "bar")

  def vatSchemeWithEligibilityDataJson(regId: String = registrationId): JsObject = Json.parse(
    s"""
       |{
       | "registrationId":"$regId",
       | "status":"draft"
       |}
      """.stripMargin).as[JsObject]

  val otherUsersVatScheme: JsObject = vatSchemeWithEligibilityDataJson(otherRegId)

  val testAccountNumber = "12345678"
  val encryptedAccountNumber = "V0g2RXVUcUZpSUk4STgvbGNFdlAydz09"
  val sortCode = "12-34-56"
  val bankAccountDetails: BankAccountDetails = BankAccountDetails("testAccountName", sortCode, testAccountNumber, ValidStatus)
  val bankAccount: BankAccount = BankAccount(isProvided = true, Some(bankAccountDetails), None, None)

  val vatSchemeWithEligibilityData = VatScheme(
    registrationId = testRegId,
    internalId = testInternalid,
    status = VatRegStatus.draft,
    createdDate = testDate,
    eligibilitySubmissionData = Some(testEligibilitySubmissionData)
  )

  val vatApplication = VatApplication(
    None, None,
    turnoverEstimate = Some(testTurnover),
    zeroRatedSupplies = Some(12.99),
    claimVatRefunds = Some(true),
    returnsFrequency = Some(Quarterly),
    staggerStart = Some(JanuaryStagger),
    startDate = Some(testDate),
    northernIrelandProtocol = Some(NIPCompliance(
      goodsToEU = ConditionalValue(true, Some(testTurnover)),
      goodsFromEU = ConditionalValue(true, Some(testTurnover))
    )),
    appliedForExemption = None,
    annualAccountingDetails = None,
    overseasCompliance = None,
    hasTaxRepresentative = Some(false),
    currentlyTrading = None
  )

  val vatSchemeWithReturns = VatScheme(
    registrationId = testRegId,
    internalId = testInternalid,
    status = VatRegStatus.draft,
    createdDate = testDate,
    vatApplication = Some(vatApplication)
  )

  "Calling createNewVatScheme" should {
    "create a new, blank VatScheme with the correct ID" in new Setup {
      await(repository.createNewVatScheme(testRegId, testInternalid)) mustBe vatScheme.copy(createdDate = testDate)
    }
    "throw an MongoWriteException when creating a new VAT scheme when one already exists with the same int Id and reg id" in new Setup {
      await(repository.createNewVatScheme(vatSchemeWithEligibilityData.registrationId, testInternalid))
      intercept[MongoWriteException](await(repository.createNewVatScheme(vatScheme.registrationId, testInternalid)))
    }
    "throw an MongoWriteException when creating a new VAT scheme where one already exists with the same regId but different Internal id" in new Setup {
      await(repository.createNewVatScheme(vatSchemeWithEligibilityData.registrationId, testInternalid))
      intercept[MongoWriteException](await(repository.createNewVatScheme(vatScheme.registrationId, "fooBarWizz")))
    }
  }
  "Calling insertVatScheme" should {
    "insert the VatScheme object" in new Setup {
      await(repository.upsertRegistration(testInternalid, testRegId, testFullVatScheme)) mustBe Some(testFullVatScheme)
      await(repository.getRegistration(testInternalid, testRegId)) mustBe Some(testFullVatScheme)
    }
    "override a VatScheme with the same regId" in new Setup {
      await(repository.upsertRegistration(testInternalid, testRegId, testVatScheme)) mustBe Some(testVatScheme)
      await(repository.getRegistration(testInternalid, testRegId)) mustBe Some(testVatScheme)

      await(repository.upsertRegistration(testInternalid, testRegId, testFullVatScheme)) mustBe Some(testFullVatScheme)
      await(repository.getRegistration(testInternalid, testRegId)) mustBe Some(testFullVatScheme)
    }
  }
  "Calling retrieveVatScheme" should {
    "retrieve a VatScheme object" in new Setup {
      insert(testVatScheme).flatMap(_ => repository.getRegistration(testVatScheme.internalId, testVatScheme.registrationId)) returns Some(testVatScheme)
    }
    "return a None when there is no corresponding VatScheme object" in new Setup {
      insert(testVatScheme).flatMap(_ => repository.getRegistration("fakeInternalId", "fakeRegId")) returns None
    }
  }
  "Calling updateSubmissionStatus" should {
    "set the status" in new Setup {
      val result: Future[VatRegStatus.Value] = for {
        _ <- insert(testVatScheme)
        _ <- repository.updateSubmissionStatus(testVatScheme.internalId, testVatScheme.registrationId, VatRegStatus.locked)
        Some(updatedScheme) <- repository.getRegistration(testVatScheme.internalId, testVatScheme.registrationId)
      } yield updatedScheme.status

      await(result) mustBe VatRegStatus.locked
    }
  }
  "Calling finishRegistrationSubmission" should {
    "update the vat scheme to submitted with the provided ackref" in new Setup {
      val result: Future[VatScheme] = for {
        _ <- insert(testVatScheme)
        _ <- repository.finishRegistrationSubmission(testVatScheme.registrationId, VatRegStatus.submitted, testFormBundleId)
        Some(updatedScheme) <- repository.getRegistration(testVatScheme.internalId, testVatScheme.registrationId)
      } yield updatedScheme

      val res = await(result)

      res.status mustBe VatRegStatus.submitted
      res.acknowledgementReference must contain(s"VRS$testFormBundleId")
    }
  }

  "getInternalId" should {
    "return a Future[Option[String]] containing Some(InternalId)" in new Setup {
      val result: Future[Option[String]] = for {
        _ <- insert(vatSchemeWithEligibilityData)
        result <- repository.getInternalId(vatSchemeWithEligibilityData.registrationId)

      } yield result
      await(result) mustBe Some(testInternalid)
    }
    "return a None when no regId document is found" in new Setup {
      await(repository.getInternalId(vatSchemeWithEligibilityData.registrationId)) mustBe None
    }
  }
}

