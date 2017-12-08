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

import java.time.LocalDate

import common.exceptions._
import common.{LogicalGroup, RegistrationId, TransactionId}
import enums.VatRegStatus
import itutil.{FutureAssertions, ITFixtures, MongoBaseSpec}
import models.api.{Eligibility, Threshold, TradingDetails}
import models.{AcknowledgementReferencePath, VatBankAccountPath}
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json._
import reactivemongo.api.commands.WriteResult
import repositories.{RegistrationMongo, RegistrationMongoRepository}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationMongoRepositoryISpec extends UnitSpec with MongoBaseSpec with MongoSpecSupport
  with FutureAssertions with BeforeAndAfterEach with WithFakeApplication with ITFixtures {

  class Setup {
    val mongo: RegistrationMongo = fakeApplication.injector.instanceOf[RegistrationMongo]
    val repository: RegistrationMongoRepository = mongo.store

    def insert(json: JsObject): WriteResult = await(repository.collection.insert(json))
    def count: Int = await(repository.count)
    def fetchAll: Option[JsObject] = await(repository.collection.find(Json.obj()).one[JsObject])

    protected def updateLogicalGroup[G: LogicalGroup : Writes](g: G, rid: RegistrationId = regId): Future[G] =
      repository.updateLogicalGroup(rid, g)

    await(repository.drop)
    await(repository.ensureIndexes)
  }

  "Calling createNewVatScheme" should {

    "create a new, blank VatScheme with the correct ID" in new Setup {
      repository.createNewVatScheme(regId) returns vatScheme
    }

    "throw an InsertFailed exception when creating a new VAT scheme when one already exists" in new Setup {
      repository.createNewVatScheme(vatScheme.id).flatMap(_ => repository.createNewVatScheme(vatScheme.id)) failedWith classOf[InsertFailed]
    }

  }

  "Calling retrieveVatScheme" should {

    "retrieve a VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.retrieveVatScheme(vatScheme.id)) returns Some(vatScheme)
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.retrieveVatScheme(RegistrationId("NOT_THERE"))) returns None
    }

  }


  "Calling updateLogicalGroup" should {

    "should update to VatTradingDetails success" in new Setup {
      repository.insert(vatScheme).flatMap(_ => updateLogicalGroup(vatTradingDetails)) returns vatTradingDetails
    }

    "should update to VatSicAndCompliance success" in new Setup {
      repository.insert(vatScheme).flatMap(_ => updateLogicalGroup(compliance)) returns compliance
    }

    "should update to VatFinancials success" in new Setup {
      repository.insert(vatScheme).flatMap(_ => updateLogicalGroup(vatFinancials)) returns vatFinancials
    }

    "should update to VatContact success" in new Setup {
      repository.insert(vatScheme).flatMap(_ => updateLogicalGroup(vatContact)) returns vatContact
    }

    "should update to VatLodgingOfficer success" in new Setup {
      repository.insert(vatScheme).flatMap(_ => updateLogicalGroup(vatLodgingOfficer)) returns vatLodgingOfficer
    }

    "should throw UpdateFailed exception when regId not found" in new Setup {
      repository.insert(vatScheme).flatMap(_ => updateLogicalGroup(vatTradingDetails, RegistrationId("0"))) failedWith classOf[UpdateFailed]
    }

  }

  "Calling deleteVatScheme" should {

    "delete a VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.deleteVatScheme(vatScheme.id.value)) returns true
    }
  }

  "Calling deleteByElement" should {

    "delete BankAccountDetails object when one exists" in new Setup {
      val schemeWithAccount = vatScheme.copy(financials = Some(vatFinancials))
      repository.insert(schemeWithAccount).flatMap(_ => repository.deleteByElement(schemeWithAccount.id, VatBankAccountPath)) returns true
    }

    "delete BankAccountDetails object when one does not exist" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.deleteByElement(vatScheme.id, VatBankAccountPath)) returns true
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.deleteByElement(RegistrationId("0"), VatBankAccountPath)) failedWith classOf[UpdateFailed]
    }
  }

  val ACK_REF_NUM = "REF0000001"
  "Calling updateByElement" should {

    "update Element object when one exists" in new Setup {
      val schemeWithAckRefNumber = vatScheme.copy(acknowledgementReference = Some(ACK_REF_NUM))
      repository.insert(schemeWithAckRefNumber).flatMap(_ => repository.updateByElement(vatScheme.id, AcknowledgementReferencePath, ACK_REF_NUM)) returns ACK_REF_NUM
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.updateByElement(RegistrationId("0"), AcknowledgementReferencePath, ACK_REF_NUM)) failedWith classOf[UpdateFailed]
    }
  }

  "Calling updateIVStatus" should {
    "update the ivStatus in lodging officer" in new Setup {
      val result = for {
        insert                <- repository.insert(vatScheme)
        update                <- repository.updateIVStatus(vatScheme.id.value, true)
        Some(updatedScheme)   <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.lodgingOfficer.get.ivPassed

      await(result) shouldBe true
    }
  }

  "Calling prepareRegistrationSubmission" should {
    val testAckRef = "testAckRef"
    "update the vat scheme with the provided ackref" in new Setup {
      val result = for {
        insert                <- repository.insert(vatScheme)
        update                <- repository.prepareRegistrationSubmission(vatScheme.id, testAckRef)
        Some(updatedScheme)   <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.acknowledgementReference

      await(result).get shouldBe testAckRef
    }
  }

  "Calling finishRegistrationSubmission" should {
    "update the vat scheme to submitted with the provided ackref" in new Setup {
      val result = for {
        insert                <- repository.insert(vatScheme)
        update                <- repository.finishRegistrationSubmission(vatScheme.id, VatRegStatus.submitted)
        Some(updatedScheme)   <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.status

      await(result) shouldBe VatRegStatus.submitted
    }

    "update the vat scheme to held with the provided ackref" in new Setup {
      val result = for {
        insert                <- repository.insert(vatScheme)
        update                <- repository.finishRegistrationSubmission(vatScheme.id, VatRegStatus.held)
        Some(updatedScheme)   <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.status

      await(result) shouldBe VatRegStatus.held
    }
  }

  "Calling saveTransId" should {
    "store the transaction id provided into the specified vat scheme document" in new Setup {
      val testTransId = "testTransId"

      val result = for {
        insert                <- repository.insert(vatScheme)
        update                <- repository.saveTransId(testTransId, vatScheme.id)
        Some(updatedScheme)   <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.transactionId

      await(result).get shouldBe TransactionId(testTransId)
    }
  }

  "Calling fetchRegIdByTxId" should {
    "retrieve the vat scheme by transactionid" in new Setup {
      val testTransId = "testTransId"

      val result = for {
        insert                <- repository.insert(vatScheme)
        update                <- repository.saveTransId(testTransId, vatScheme.id)
        Some(updatedScheme)   <- repository.fetchRegByTxId(testTransId)
      } yield updatedScheme

      await(result) shouldBe vatScheme.copy(transactionId = Some(TransactionId(testTransId)))
    }
  }

  "updateTradingDetails" should {

    val registrationId: String = "reg-12345"
    val tradingName = "testTradingName"

    def vatSchemeJson(regId: String = registrationId): JsObject = Json.parse(
      s"""
        |{
        | "registrationId":"$regId",
        | "status":"draft"
        |}
      """.stripMargin).as[JsObject]

    val vatSchemeWithTradingDetailsJson: JsObject = Json.parse(
      s"""
        |{
        | "registrationId":"$registrationId",
        | "status":"draft",
        | "tradingDetails":{
        |   "tradingName":"$tradingName"
        | }
        |}
      """.stripMargin).as[JsObject]

    val otherRegId = "other-reg-12345"
    val otherUsersVatScheme = vatSchemeJson(otherRegId)

    val tradingDetails: TradingDetails = TradingDetails(Some(tradingName), None)

    "update the registration doc with the provided trading details" in new Setup {
      insert(vatSchemeJson())

      await(repository.updateTradingDetails(registrationId, tradingDetails))

      fetchAll without _id shouldBe Some(vatSchemeWithTradingDetailsJson)
    }

    "not update or insert new data into the registration doc if the supplied details already exist on the doc" in new Setup {
      insert(vatSchemeWithTradingDetailsJson)

      await(repository.updateTradingDetails(registrationId, tradingDetails))

      fetchAll without _id shouldBe Some(vatSchemeWithTradingDetailsJson)
    }

    "not update or insert trading details if a registration doc doesn't already exist" in new Setup {
      count shouldBe 0

      await(repository.updateTradingDetails(registrationId, tradingDetails))

      fetchAll without _id shouldBe None
    }

    "not update or insert trading details if a registration doc associated with the given reg id doesn't already exist" in new Setup {
      insert(otherUsersVatScheme)

      fetchAll without _id shouldBe Some(otherUsersVatScheme)

      await(repository.updateTradingDetails(registrationId, tradingDetails))

      fetchAll without _id shouldBe Some(otherUsersVatScheme)
    }
  }

  "Calling getEligibility" should {
    val eligibility = Eligibility(version = 1, result = "testResult")

    "return eligibility data from an existing registration containing data" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme.copy(eligibility = Some(eligibility)))
        res <- repository.getEligibility(vatScheme.id.value)
      } yield res

      await(result) shouldBe Some(eligibility)
    }

    "return None from an existing registration containing no data" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme)
        res <- repository.getEligibility(vatScheme.id.value)
      } yield res

      await(result) shouldBe None
    }

    "return None from a none existing registration" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme.copy(eligibility = Some(eligibility)))
        res <- repository.getEligibility("wrongRegId")
      } yield res

      await(result) shouldBe None
    }

    "return an exception if there is no version in eligibility block in repository" in new Setup {
      val regId = "reg-123"
      val json: JsObject = Json.parse(
        s"""
           |{
           |  "registrationId": "$regId",
           |  "status": "${VatRegStatus.draft}",
           |  "eligibility": {
           |    "result": "test result"
           |  }
           |}
         """.stripMargin).as[JsObject]

      insert(json)

      an[Exception] shouldBe thrownBy(await(repository.getEligibility(regId)))
    }

    "return an exception if there is no result in eligibility block in repository" in new Setup {
      val regId = "reg-123"
      val json: JsObject = Json.parse(
        s"""
           |{
           |  "registrationId": "$regId",
           |  "status": "${VatRegStatus.draft}",
           |  "eligibility": {
           |    "version": 1
           |  }
           |}
         """.stripMargin).as[JsObject]

      insert(json)

      an[Exception] shouldBe thrownBy(await(repository.getEligibility(regId)))
    }
  }

  "Calling updateEligibility" should {
    val eligibility = Eligibility(version = 1, result = "testResult")

    "update eligibility block in registration when there is no eligibility data" in new Setup {
      val result = for {
        _                   <- repository.insert(vatScheme)
        _                   <- repository.updateEligibility(vatScheme.id.value, eligibility)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.eligibility

      await(result) shouldBe Some(eligibility)
    }

    "update eligibility block in registration when there is already eligibility data" in new Setup {
      val otherEligibility = Eligibility(version = 2, result = "oldResult")
      val result = for {
        _                   <- repository.insert(vatScheme.copy(eligibility = Some(otherEligibility)))
        _                   <- repository.updateEligibility(vatScheme.id.value, eligibility)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.eligibility

      await(result) shouldBe Some(eligibility)
    }

    "not update or insert eligibility if registration does not exist" in new Setup {
      await(repository.insert(vatScheme))

      count shouldBe 1
      await(repository.findAll()).head shouldBe vatScheme

      a[MissingRegDocument] shouldBe thrownBy(await(repository.updateEligibility("wrongRegId", eligibility)))
    }
  }

  "Calling getThreshold" should {
    val threshold = Threshold(mandatoryRegistration = false,
                              voluntaryReason = Some("a reason"),
                              overThresholdDate = Some(LocalDate.of(2017, 12, 28)),
                              expectedOverThresholdDate = Some(LocalDate.of(2018, 1, 31)))

    "return threshold data from an existing registration containing data" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme.copy(threshold = Some(threshold)))
        res <- repository.getThreshold(vatScheme.id.value)
      } yield res

      await(result) shouldBe Some(threshold)
    }

    "return None from an existing registration containing no data" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme)
        res <- repository.getThreshold(vatScheme.id.value)
      } yield res

      await(result) shouldBe None
    }

    "return None from a none existing registration" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme.copy(threshold = Some(threshold)))
        res <- repository.getThreshold("wrongRegId")
      } yield res

      await(result) shouldBe None
    }

    "return an exception if there is no mandatoryRegistration in threshold block in repository" in new Setup {
      val regId = "reg-123"
      val json: JsObject = Json.parse(
        s"""
           |{
           |  "registrationId": "$regId",
           |  "status": "${VatRegStatus.draft}",
           |  "threshold": {
           |    "voluntaryReason": "test reason"
           |  }
           |}
         """.stripMargin).as[JsObject]

      insert(json)

      an[Exception] shouldBe thrownBy(await(repository.getThreshold(regId)))
    }
  }

  "Calling updateThreshold" should {
    val threshold = Threshold(mandatoryRegistration = false, voluntaryReason = Some("a reason"), overThresholdDate = None, expectedOverThresholdDate = None)

    "update threshold block in registration when there is no threshold data" in new Setup {
      val result = for {
        _                   <- repository.insert(vatScheme)
        _                   <- repository.updateThreshold(vatScheme.id.value, threshold)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.threshold

      await(result) shouldBe Some(threshold)
    }

    "update threshold block in registration when there is already threshold data" in new Setup {
      val otherThreshold = Threshold(mandatoryRegistration = true, voluntaryReason = None, overThresholdDate = None, expectedOverThresholdDate = None)
      val result = for {
        _                   <- repository.insert(vatScheme.copy(threshold = Some(otherThreshold)))
        _                   <- repository.updateThreshold(vatScheme.id.value, threshold)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.threshold

      await(result) shouldBe Some(threshold)
    }

    "not update or insert threshold if registration does not exist" in new Setup {
      await(repository.insert(vatScheme))

      count shouldBe 1
      await(repository.findAll()).head shouldBe vatScheme

      a[MissingRegDocument] shouldBe thrownBy(await(repository.updateThreshold("wrongRegId", threshold)))
    }
  }
}
