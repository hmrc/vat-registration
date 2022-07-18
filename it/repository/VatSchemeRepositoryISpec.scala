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

import common.exceptions._
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
    testRegId, internalId = testInternalid, status = VatRegStatus.draft, eligibilitySubmissionData = Some(testEligibilitySubmissionData)
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
    hasTaxRepresentative = Some(false)
  )

  val vatSchemeWithReturns = VatScheme(
    id = testRegId,
    internalId = testInternalid,
    status = VatRegStatus.draft,
    vatApplication = Some(vatApplication)
  )

  "Calling createNewVatScheme" should {
    "create a new, blank VatScheme with the correct ID" in new Setup {
      await(repository.createNewVatScheme(testRegId, testInternalid)) mustBe vatScheme.copy(createdDate = Some(testDate))
    }
    "throw an MongoWriteException when creating a new VAT scheme when one already exists with the same int Id and reg id" in new Setup {
      await(repository.createNewVatScheme(vatSchemeWithEligibilityData.id, testInternalid))
      intercept[MongoWriteException](await(repository.createNewVatScheme(vatScheme.id, testInternalid)))
    }
    "throw an MongoWriteException when creating a new VAT scheme where one already exists with the same regId but different Internal id" in new Setup {
      await(repository.createNewVatScheme(vatSchemeWithEligibilityData.id, testInternalid))
      intercept[MongoWriteException](await(repository.createNewVatScheme(vatScheme.id, "fooBarWizz")))
    }
  }
  "Calling insertVatScheme" should {
    "insert the VatScheme object" in new Setup {
      await(repository.insertVatScheme(testFullVatScheme)) mustBe testFullVatScheme
      await(repository.retrieveVatScheme(testRegId)) mustBe Some(testFullVatScheme)
    }
    "override a VatScheme with the same regId" in new Setup {
      await(repository.insertVatScheme(testVatScheme)) mustBe testVatScheme
      await(repository.retrieveVatScheme(testRegId)) mustBe Some(testVatScheme)

      await(repository.insertVatScheme(testFullVatScheme)) mustBe testFullVatScheme
      await(repository.retrieveVatScheme(testRegId)) mustBe Some(testFullVatScheme)
    }
  }
  "Calling retrieveVatScheme" should {
    "retrieve a VatScheme object" in new Setup {
      insert(testVatScheme).flatMap(_ => repository.retrieveVatScheme(testVatScheme.id)) returns Some(testVatScheme)
    }
    "return a None when there is no corresponding VatScheme object" in new Setup {
      insert(testVatScheme).flatMap(_ => repository.retrieveVatScheme("fakeRegId")) returns None
    }
  }
  "Calling updateSubmissionStatus" should {
    "set the status" in new Setup {
      val result: Future[VatRegStatus.Value] = for {
        _ <- insert(testVatScheme)
        _ <- repository.updateSubmissionStatus(testVatScheme.id, VatRegStatus.locked)
        Some(updatedScheme) <- repository.retrieveVatScheme(testVatScheme.id)
      } yield updatedScheme.status

      await(result) mustBe VatRegStatus.locked
    }
  }
  "Calling finishRegistrationSubmission" should {
    "update the vat scheme to submitted with the provided ackref" in new Setup {
      val result: Future[VatScheme] = for {
        _ <- insert(testVatScheme)
        _ <- repository.finishRegistrationSubmission(testVatScheme.id, VatRegStatus.submitted, testFormBundleId)
        Some(updatedScheme) <- repository.retrieveVatScheme(testVatScheme.id)
      } yield updatedScheme

      val res = await(result)

      res.status mustBe VatRegStatus.submitted
      res.acknowledgementReference must contain(s"VRS$testFormBundleId")
    }
  }
  "updateTradingDetails" should {
    val tradingDetails = TradingDetails(Some(testTradingName), Some(testShortOrgName))

    "update tradingDetails block in registration when there is no tradingDetails data" in new Setup {
      val result: Future[Option[TradingDetails]] = for {
        _ <- insert(testVatScheme)
        _ <- repository.updateTradingDetails(testVatScheme.id, tradingDetails)
        Some(updatedScheme) <- repository.retrieveVatScheme(testVatScheme.id)
      } yield updatedScheme.tradingDetails

      await(result) mustBe Some(tradingDetails)
    }
    "update tradingDetails block in registration when there is already tradingDetails data" in new Setup {
      val result: Future[Option[TradingDetails]] = for {
        _ <- insert(testVatScheme.copy(tradingDetails = Some(tradingDetails)))
        _ <- repository.updateTradingDetails(testVatScheme.id, tradingDetails)
        Some(updatedScheme) <- repository.retrieveVatScheme(testVatScheme.id)
      } yield updatedScheme.tradingDetails

      await(result) mustBe Some(tradingDetails)
    }
    "not update or insert tradingDetails if registration does not exist" in new Setup {
      await(insert(testVatScheme))

      count mustBe 1
      await(repository.collection.find().toFuture()).head mustBe testVatScheme

      a[MissingRegDocument] mustBe thrownBy(await(repository.updateTradingDetails("wrongRegId", tradingDetails)))
    }
  }
  "Calling retrieveTradingDetails" should {
    val tradingDetails = TradingDetails(Some(testTradingName), Some(testShortOrgName))

    "return trading details data from an existing registration containing data" in new Setup {
      val result: Future[Option[TradingDetails]] = for {
        _ <- insert(testVatScheme.copy(tradingDetails = Some(tradingDetails)))
        res <- repository.retrieveTradingDetails(testVatScheme.id)
      } yield res

      await(result) mustBe Some(tradingDetails)
    }
    "return None from an existing registration containing no data" in new Setup {
      val result: Future[Option[TradingDetails]] = for {
        _ <- insert(testVatScheme)
        res <- repository.retrieveTradingDetails(testVatScheme.id)
      } yield res

      await(result) mustBe None
    }
    "throw a MissingRegDocument for a none existing registration" in new Setup {
      val result: Future[Option[TradingDetails]] = for {
        _ <- insert(testVatScheme.copy(tradingDetails = Some(tradingDetails)))
        res <- repository.retrieveTradingDetails("wrongRegId")
      } yield res

      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
  }
  "fetchBankAccount" should {
    "return a BankAccount case class if one is found in mongo with the supplied regId" in new Setup {
      await(insert(testVatScheme.copy(bankAccount = Some(bankAccount))))

      val fetchedBankAccount: Option[BankAccount] = await(repository.fetchBankAccount(testRegId))

      fetchedBankAccount mustBe Some(bankAccount)
    }
    "return a None if a VatScheme already exists but a bank account block does not" in new Setup {
      await(insert(testVatScheme))
      val fetchedBankAccount: Option[BankAccount] = await(repository.fetchBankAccount(testRegId))
      fetchedBankAccount mustBe None
    }
    "return None if no BankAccount is found in mongo for the supplied regId" in new Setup {
      count mustBe 0

      val fetchedBankAccount: Option[BankAccount] = await(repository.fetchBankAccount(testRegId))

      fetchedBankAccount mustBe None
    }
    "return None if other users' data exists but no BankAccount is found in mongo for the supplied regId" in new Setup {
      await(insert(testVatScheme.copy(id = "otherUser")))

      val fetchedBankAccount: Option[BankAccount] = await(repository.fetchBankAccount(testRegId))

      fetchedBankAccount mustBe None
    }
  }
  "updateBankAccount" should {
    "update the registration doc with the provided bank account details and encrypt the account number" in new Setup {
      await(insert(testVatScheme))

      await(repository.updateBankAccount(testRegId, bankAccount))

      getRegistration mustBe Some(testVatScheme.copy(bankAccount = Some(bankAccount)))
    }
    "not update or insert new data into the registration doc if the supplied bank account details already exist on the doc" in new Setup {
      val testSchemeEnc = testVatScheme.copy(bankAccount = Some(bankAccount.copy(details = Some(bankAccountDetails.copy(number = encryptedAccountNumber)))))
      await(insert(testSchemeEnc))

      await(repository.updateBankAccount(testRegId, bankAccount))

      getRegistration mustBe Some(testVatScheme.copy(bankAccount = Some(bankAccount)))
    }
    "not update or insert bank account if a registration doc doesn't already exist" in new Setup {
      count mustBe 0

      await(repository.updateBankAccount(testRegId, bankAccount))

      getRegistration mustBe None
    }
    "not update or insert bank account if a registration doc associated with the given reg id doesn't already exist" in new Setup {
      await(insert(testVatScheme))

      await(repository.updateBankAccount(testRegId, bankAccount))

      getRegistration mustBe Some(testVatScheme.copy(bankAccount = Some(bankAccount)))
    }
  }

  "fetchFlatRateScheme" should {
    "return flat rate scheme data from an existing registration containing data" in new Setup {
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- insert(vatSchemeWithEligibilityData.copy(flatRateScheme = Some(testFlatRateScheme)))
        res <- repository.fetchFlatRateScheme(vatSchemeWithEligibilityData.id)
      } yield res

      await(result) mustBe Some(testFlatRateScheme)
    }
    "return None from an existing registration containing no data" in new Setup {
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- insert(vatSchemeWithEligibilityData)
        res <- repository.fetchFlatRateScheme(vatSchemeWithEligibilityData.id)
      } yield res

      await(result) mustBe None
    }
    "throw a MissingRegDocument for a none existing registration" in new Setup {
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- insert(vatSchemeWithEligibilityData.copy(flatRateScheme = Some(testFlatRateScheme)))
        res <- repository.fetchFlatRateScheme("wrongRegId")
      } yield res

      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
  }
  "updateFlatRateScheme" should {
    "update flat rate scheme block in registration when there is no flat rate scheme data" in new Setup {
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- insert(vatSchemeWithEligibilityData)
        _ <- repository.updateFlatRateScheme(vatSchemeWithEligibilityData.id, testFlatRateScheme)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatSchemeWithEligibilityData.id)
      } yield updatedScheme.flatRateScheme

      await(result) mustBe Some(testFlatRateScheme)
    }
    "update flat rate scheme block in registration when there is already flat rate scheme data" in new Setup {
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- insert(testVatScheme.copy(flatRateScheme = Some(testFlatRateScheme)))
        _ <- repository.updateFlatRateScheme(testRegId, testFlatRateScheme)
        Some(updatedScheme) <- repository.retrieveVatScheme(testRegId)
      } yield updatedScheme.flatRateScheme

      await(result) mustBe Some(testFlatRateScheme)
    }
    "not update or insert flat rate scheme if registration does not exist" in new Setup {
      await(insert(vatSchemeWithEligibilityData))

      count mustBe 1
      await(repository.collection.find().toFuture()).head mustBe vatSchemeWithEligibilityData

      a[MissingRegDocument] mustBe thrownBy(await(repository.updateFlatRateScheme("wrongRegId", testFlatRateScheme)))
    }
  }
  "removeFlatRateScheme" should {
    "remove a flatRateScheme block in the registration document if it exists in the registration doc" in new Setup {
      val result: Future[Option[VatScheme]] = for {
        _ <- insert(vatSchemeWithEligibilityData.copy(flatRateScheme = Some(testFlatRateScheme)))
        _ <- repository.removeFlatRateScheme(vatSchemeWithEligibilityData.id)
        updatedScheme <- repository.retrieveVatScheme(vatSchemeWithEligibilityData.id)
      } yield updatedScheme

      await(result) mustBe Some(vatSchemeWithEligibilityData)
    }
    "throw a MissingRegDocument if the vat scheme does not exist for the regId" in new Setup {
      a[MissingRegDocument] mustBe thrownBy(await(repository.removeFlatRateScheme(vatSchemeWithEligibilityData.id)))
    }
  }
  "getInternalId" should {
    "return a Future[Option[String]] containing Some(InternalId)" in new Setup {
      val result: Future[Option[String]] = for {
        _ <- insert(vatSchemeWithEligibilityData)
        result <- repository.getInternalId(vatSchemeWithEligibilityData.id)

      } yield result
      await(result) mustBe Some(testInternalid)
    }
    "return a None when no regId document is found" in new Setup {
      await(repository.getInternalId(vatSchemeWithEligibilityData.id)) mustBe None
    }
  }
  "getEligibilityData" should {
    "return some of eligibilityData" in new Setup {
      await(insert(vatSchemeWithEligibilityData.copy(eligibilityData = Some(jsonEligiblityData))))
      count mustBe 1

      await(repository.fetchEligibilityData(vatSchemeWithEligibilityData.id)) mustBe Some(jsonEligiblityData)
    }
    "return None of eligibilityData" in new Setup {
      await(insert(vatSchemeWithEligibilityData))
      count mustBe 1

      await(repository.fetchEligibilityData(vatSchemeWithEligibilityData.id)) mustBe None
    }
  }
  "updateEligibilityData" should {
    "update eligibilityData successfully when no eligibilityData block exists" in new Setup {
      await(insert(vatSchemeWithEligibilityData))
      count mustBe 1

      await(repository.fetchEligibilityData(vatSchemeWithEligibilityData.id)) mustBe None

      val res: JsObject = await(repository.updateEligibilityData(vatSchemeWithEligibilityData.id, jsonEligiblityData))
      res mustBe jsonEligiblityData

      await(repository.fetchEligibilityData(vatSchemeWithEligibilityData.id)) mustBe Some(jsonEligiblityData)
    }
    "update eligibilityData successfully when eligibilityData block already exists" in new Setup {
      val newJsonEligiblityData = Json.obj("wizz" -> "new bar")

      await(insert(vatSchemeWithEligibilityData.copy(eligibilityData = Some(jsonEligiblityData))))
      count mustBe 1

      await(repository.fetchEligibilityData(vatSchemeWithEligibilityData.id)) mustBe Some(jsonEligiblityData)

      val res: JsObject = await(repository.updateEligibilityData(vatSchemeWithEligibilityData.id, newJsonEligiblityData))
      res mustBe newJsonEligiblityData

      await(repository.fetchEligibilityData(vatSchemeWithEligibilityData.id)) mustBe Some(newJsonEligiblityData)
    }
  }
  "calling fetchNrsSubmissionPayload" should {
    val testPayload = "testPayload"

    "return an encoded payload string from existing data based on the reg Id" in new Setup {
      val result: Future[Option[String]] = for {
        _ <- insert(vatSchemeWithEligibilityData)
        _ <- repository.updateNrsSubmissionPayload(vatSchemeWithEligibilityData.id, testPayload)
        _ = count mustBe 1
        res <- repository.fetchNrsSubmissionPayload(vatSchemeWithEligibilityData.id)
      } yield res

      await(result).get mustBe testPayload
    }
    "return None from an existing registration that exists but the payload does not exist" in new Setup {
      val result: Future[Option[String]] = for {
        _ <- insert(vatSchemeWithEligibilityData)
        res <- repository.fetchNrsSubmissionPayload(vatSchemeWithEligibilityData.id)
      } yield res

      await(result) mustBe None
    }
    "return a MissingRegDocument when nothing is returned from mongo for the reg id" in new Setup {
      val result: Future[Option[String]] = repository.fetchNrsSubmissionPayload("madeUpRegId")

      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
  }
  "calling updateNrsSubmissionPayload" should {
    val testPayload = "testPayload"

    "return an updated payload string when an entry already exists in the repo" in new Setup {
      val testOldPayload = "testOldPayload"

      val result: Future[String] = for {
        _ <- insert(vatSchemeWithEligibilityData.copy(nrsSubmissionPayload = Some(testOldPayload)))
        _ = count mustBe 1
        res <- repository.updateNrsSubmissionPayload(vatSchemeWithEligibilityData.id, testPayload)
      } yield res

      await(result) mustBe testPayload
    }
    "return the payload string after storing it" in new Setup {
      val result: Future[String] = for {
        _ <- insert(vatSchemeWithEligibilityData)
        res <- repository.updateNrsSubmissionPayload(vatSchemeWithEligibilityData.id, testPayload)
      } yield res
      await(result) mustBe testPayload
    }
    "return an MissingRegDocument if registration document does not exist for the registration id" in new Setup {
      val result: Future[String] = repository.updateNrsSubmissionPayload("madeUpRegId", testPayload)
      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
  }
}

