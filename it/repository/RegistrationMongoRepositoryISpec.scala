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
import itutil.{FutureAssertions, ITFixtures, MongoBaseSpec}
import models.api._
import models.api.returns._
import play.api.libs.json._
import play.api.test.Helpers._
import reactivemongo.api.commands.WriteResult
import repositories.RegistrationMongoRepository

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationMongoRepositoryISpec extends MongoBaseSpec with FutureAssertions with ITFixtures {

  class Setup {
    val repository: RegistrationMongoRepository = app.injector.instanceOf[RegistrationMongoRepository]

    def insert(json: JsObject): WriteResult = await(repository.collection.insert(json))

    def count: Int = await(repository.count)

    def fetchAll: Option[JsObject] = await(repository.collection.find(Json.obj()).one[JsObject])

    await(repository.drop)
    await(repository.ensureIndexes)
  }

  val ACK_REF_NUM = "REF0000001"
  val registrationId: String = "reg-12345"
  val otherRegId = "other-reg-12345"
  val jsonEligiblityData = Json.obj("foo" -> "bar")

  def vatSchemeJson(regId: String = registrationId): JsObject = Json.parse(
    s"""
       |{
       | "registrationId":"$regId",
       | "status":"draft"
       |}
      """.stripMargin).as[JsObject]

  val otherUsersVatScheme: JsObject = vatSchemeJson(otherRegId)

  val accountNumber = "12345678"
  val encryptedAccountNumber = "V0g2RXVUcUZpSUk4STgvbGNFdlAydz09"
  val sortCode = "12-34-56"
  val bankAccountDetails: BankAccountDetails = BankAccountDetails("testAccountName", sortCode, accountNumber)
  val bankAccount: BankAccount = BankAccount(isProvided = true, Some(bankAccountDetails), None)

  val vatSchemeWithBankAccount: JsObject = Json.parse(
    s"""
       |{
       | "registrationId":"$registrationId",
       | "status":"draft",
       | "bankAccount":{
       |   "isProvided":true,
       |   "details":{
       |     "name":"testAccountName",
       |     "sortCode":"$sortCode",
       |     "number":"$encryptedAccountNumber"
       |    }
       |  }
       |}
      """.stripMargin).as[JsObject]

  val vatSchemeWithReturns: JsObject = Json.parse(
    s"""
       |{
       |  "registrationId":"$registrationId",
       |  "status": "draft",
       |  "returns": {
       |    "reclaimVatOnMostReturns": true,
       |    "returnsFrequency": ${Json.toJson[ReturnsFrequency](Quarterly)},
       |    "staggerStart": ${Json.toJson[Stagger](JanuaryStagger)},
       |    "startDate": "$testDate",
       |    "zeroRatedSupplies": 12.99
       |  }
       |}
     """.stripMargin).as[JsObject]


  val vatTaxable = 1000L
  val turnoverEstimates: TurnoverEstimates = TurnoverEstimates(vatTaxable)

  def vatSchemeWithTurnoverEstimates(regId: String = registrationId): JsObject = vatSchemeJson(regId) ++ Json.parse(
    """
      |{
      | "turnoverEstimates":{
      |   "vatTaxable":1000
      | }
      |}
    """.stripMargin).as[JsObject]

  "Calling createNewVatScheme" should {
    "create a new, blank VatScheme with the correct ID" in new Setup {
      await(repository.createNewVatScheme(testRegId, testInternalid)) mustBe vatScheme
    }
    "throw an InsertFailed exception when creating a new VAT scheme when one already exists with the same int Id and reg id" in new Setup {
      await(repository.createNewVatScheme(vatScheme.id, testInternalid))
      intercept[InsertFailed](await(repository.createNewVatScheme(vatScheme.id, testInternalid)))
    }
    "throw an InsertFailed exception when creating a new VAT scheme where one already exists with the same regId but different Internal id" in new Setup {
      await(repository.createNewVatScheme(vatScheme.id, testInternalid))
      intercept[InsertFailed](await(repository.createNewVatScheme(vatScheme.id, "fooBarWizz")))
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
      repository.insert(vatScheme).flatMap(_ => repository.retrieveVatScheme(vatScheme.id)) returns Some(vatScheme)
    }
    "return a None when there is no corresponding VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.retrieveVatScheme("fakeRegId")) returns None
    }
  }
  "Calling deleteVatScheme" should {
    "delete a VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.deleteVatScheme(vatScheme.id)) returns true
    }
  }
  "Calling prepareRegistrationSubmission" should {
    val testAckRef = "testAckRef"
    "update the vat scheme with the provided ackref" in new Setup {
      val result: Future[(VatRegStatus.Value, Option[String])] = for {
        insert <- repository.insert(vatScheme)
        update <- repository.prepareRegistrationSubmission(vatScheme.id, testAckRef, VatRegStatus.draft)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield (updatedScheme.status, updatedScheme.acknowledgementReference)

      await(result) mustBe(VatRegStatus.locked, Some(testAckRef))
    }
    "update the vat scheme with the provided ackref on a topup" in new Setup {
      val result: Future[(VatRegStatus.Value, Option[String])] = for {
        insert <- repository.insert(vatScheme)
        update <- repository.prepareRegistrationSubmission(vatScheme.id, testAckRef, VatRegStatus.held)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield (updatedScheme.status, updatedScheme.acknowledgementReference)

      await(result) mustBe(VatRegStatus.held, Some(testAckRef))
    }
  }
  "Calling finishRegistrationSubmission" should {
    "update the vat scheme to submitted with the provided ackref" in new Setup {
      val result: Future[VatRegStatus.Value] = for {
        insert <- repository.insert(vatScheme)
        update <- repository.finishRegistrationSubmission(vatScheme.id, VatRegStatus.submitted)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.status

      await(result) mustBe VatRegStatus.submitted
    }
    "update the vat scheme to held with the provided ackref" in new Setup {
      val result: Future[VatRegStatus.Value] = for {
        insert <- repository.insert(vatScheme)
        update <- repository.finishRegistrationSubmission(vatScheme.id, VatRegStatus.held)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.status

      await(result) mustBe VatRegStatus.held
    }
  }
  "updateTradingDetails" should {
    val tradingDetails = TradingDetails(Some("trading-name"), true)

    "update tradingDetails block in registration when there is no tradingDetails data" in new Setup {
      val result: Future[Option[TradingDetails]] = for {
        _ <- repository.insert(vatScheme)
        _ <- repository.updateTradingDetails(vatScheme.id, tradingDetails)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.tradingDetails

      await(result) mustBe Some(tradingDetails)
    }
    "update tradingDetails block in registration when there is already tradingDetails data" in new Setup {
      val result: Future[Option[TradingDetails]] = for {
        _ <- repository.insert(vatScheme.copy(tradingDetails = Some(tradingDetails)))
        _ <- repository.updateTradingDetails(vatScheme.id, tradingDetails)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.tradingDetails

      await(result) mustBe Some(tradingDetails)
    }
    "not update or insert tradingDetails if registration does not exist" in new Setup {
      await(repository.insert(vatScheme))

      count mustBe 1
      await(repository.findAll()).head mustBe vatScheme

      a[MissingRegDocument] mustBe thrownBy(await(repository.updateTradingDetails("wrongRegId", tradingDetails)))
    }
  }
  "Calling retrieveTradingDetails" should {
    val tradingDetails = TradingDetails(Some("trading-name"), true)

    "return trading details data from an existing registration containing data" in new Setup {
      val result: Future[Option[TradingDetails]] = for {
        _ <- repository.insert(vatScheme.copy(tradingDetails = Some(tradingDetails)))
        res <- repository.retrieveTradingDetails(vatScheme.id)
      } yield res

      await(result) mustBe Some(tradingDetails)
    }
    "return None from an existing registration containing no data" in new Setup {
      val result: Future[Option[TradingDetails]] = for {
        _ <- repository.insert(vatScheme)
        res <- repository.retrieveTradingDetails(vatScheme.id)
      } yield res

      await(result) mustBe None
    }
    "throw a MissingRegDocument for a none existing registration" in new Setup {
      val result: Future[Option[TradingDetails]] = for {
        _ <- repository.insert(vatScheme.copy(tradingDetails = Some(tradingDetails)))
        res <- repository.retrieveTradingDetails("wrongRegId")
      } yield res

      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
  }
  "fetchBankAccount" should {
    "return a BankAccount case class if one is found in mongo with the supplied regId" in new Setup {
      insert(vatSchemeWithBankAccount)

      val fetchedBankAccount: Option[BankAccount] = await(repository.fetchBankAccount(registrationId))

      fetchedBankAccount mustBe Some(bankAccount)
    }
    "return a None if a VatScheme already exists but a bank account block does not" in new Setup {
      insert(vatSchemeJson(registrationId))
      val fetchedBankAccount: Option[BankAccount] = await(repository.fetchBankAccount(registrationId))
      fetchedBankAccount mustBe None
    }
    "return None if no BankAccount is found in mongo for the supplied regId" in new Setup {
      count mustBe 0

      val fetchedBankAccount: Option[BankAccount] = await(repository.fetchBankAccount(registrationId))

      fetchedBankAccount mustBe None
    }
    "return None if other users' data exists but no BankAccount is found in mongo for the supplied regId" in new Setup {
      insert(otherUsersVatScheme)

      val fetchedBankAccount: Option[BankAccount] = await(repository.fetchBankAccount(registrationId))

      fetchedBankAccount mustBe None
    }
  }
  "updateBankAccount" should {
    "update the registration doc with the provided bank account details and encrypt the account number" in new Setup {
      insert(vatSchemeJson())

      await(repository.updateBankAccount(registrationId, bankAccount))

      fetchAll without _id mustBe Some(vatSchemeWithBankAccount)
    }
    "not update or insert new data into the registration doc if the supplied bank account details already exist on the doc" in new Setup {
      insert(vatSchemeWithBankAccount)

      await(repository.updateBankAccount(registrationId, bankAccount))

      fetchAll without _id mustBe Some(vatSchemeWithBankAccount)
    }
    "not update or insert returns if a registration doc doesn't already exist" in new Setup {
      count mustBe 0

      await(repository.updateBankAccount(registrationId, bankAccount))

      fetchAll without _id mustBe None
    }
    "not update or insert returns if a registration doc associated with the given reg id doesn't already exist" in new Setup {
      insert(otherUsersVatScheme)

      fetchAll without _id mustBe Some(otherUsersVatScheme)

      await(repository.updateBankAccount(registrationId, bankAccount))

      fetchAll without _id mustBe Some(otherUsersVatScheme)
    }
  }
  "fetchReturns" should {
    "return a Returns case class if one is found in mongo with the supplied regId" in new Setup {
      insert(vatSchemeWithReturns)

      val fetchedReturns: Option[Returns] = await(repository.fetchReturns(registrationId))

      fetchedReturns mustBe Some(testReturns)
    }
    "return None if no BankAccount is found in mongo for the supplied regId" in new Setup {
      count mustBe 0

      val fetchedReturns: Option[Returns] = await(repository.fetchReturns(registrationId))

      fetchedReturns mustBe None
    }
  }
  "updateReturns" should {
    val registrationId: String = "reg-12345"
    val otherRegId = "other-reg-12345"
    val otherUsersVatScheme = vatSchemeJson(otherRegId)
    val dateValue = LocalDate of(1990, 10, 10)
    val startDate = dateValue
    val returns: Returns = Returns(Some(12.99), reclaimVatOnMostReturns = true, Quarterly, JanuaryStagger, Some(startDate), None)
    val vatSchemeWithReturns = Json.parse(
      s"""
         |{
         | "registrationId":"$registrationId",
         | "status":"draft",
         | "returns":{
         |   "reclaimVatOnMostReturns":true,
         |   "returnsFrequency": ${Json.toJson[ReturnsFrequency](Quarterly)},
         |   "staggerStart": ${Json.toJson[Stagger](JanuaryStagger)},
         |   "startDate": "$dateValue",
         |   "zeroRatedSupplies": 12.99
         | }
         |}
      """.stripMargin).as[JsObject]

    "update the registration doc with the provided returns" in new Setup {
      insert(vatSchemeJson())

      await(repository.updateReturns(registrationId, returns))

      fetchAll without _id mustBe Some(vatSchemeWithReturns)
    }
    "not update or insert new data into the registration doc if the supplied returns already exist on the doc" in new Setup {
      insert(vatSchemeWithReturns)

      await(repository.updateReturns(registrationId, returns))

      fetchAll without _id mustBe Some(vatSchemeWithReturns)
    }
    "not update or insert returns if a registration doc doesn't already exist" in new Setup {
      count mustBe 0

      await(repository.updateReturns(registrationId, returns))

      fetchAll without _id mustBe None
    }
    "not update or insert returns if a registration doc associated with the given reg id doesn't already exist" in new Setup {
      insert(otherUsersVatScheme)

      fetchAll without _id mustBe Some(otherUsersVatScheme)

      await(repository.updateReturns(registrationId, returns))

      fetchAll without _id mustBe Some(otherUsersVatScheme)
    }
  }
  "calling getSicAndCompliance" should {
    val validSicAndCompliance = Some(SicAndCompliance(
      "this is my business description",
      Some(ComplianceLabour(numOfWorkersSupplied = Some(1000), intermediaryArrangement = Some(true), supplyWorkers = true)),
      SicCode("11111", "the flu", "sic details"),
      businessActivities = Nil
    ))
    "return a SicAndComplianceModel from existing data based on the reg Id" in new Setup {
      val result: Future[Option[SicAndCompliance]] = for {
        _ <- repository.insert(vatScheme)
        _ <- repository.updateSicAndCompliance(vatScheme.id, validSicAndCompliance.get)
        _ = count mustBe 1
        res <- repository.fetchSicAndCompliance(vatScheme.id)
      } yield res

      await(result).get mustBe validSicAndCompliance.get
    }
    "return None from an existing registration that exists but SicAndCompliance does not exist" in new Setup {
      val result: Future[Option[SicAndCompliance]] = for {
        _ <- repository.insert(vatScheme)
        res <- repository.fetchSicAndCompliance(vatScheme.id)
      } yield res
      await(result) mustBe None
    }
    "return a MissingRegDocument when nothing is returned from mongo for the reg id" in new Setup {
      val result: Future[Option[SicAndCompliance]] = repository.fetchSicAndCompliance("madeUpRegId")

      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
    "return an exception if the json is incorrect in the repository (an element is missing)" in new Setup {
      val json: JsValue = Json.toJson(
        Json.obj("registrationId" -> testRegId,
          "status" -> VatRegStatus.draft,
          "sicAndCompliance" -> Json.toJson(validSicAndCompliance).as[JsObject].-("businessDescription")))
      insert(json.as[JsObject])
      an[Exception] mustBe thrownBy(await(repository.fetchSicAndCompliance(vatScheme.id)))
    }
  }
  "calling updateSicAndCompliance" should {
    val validSicAndCompliance: Option[SicAndCompliance] = Some(SicAndCompliance(
      "this is my business description",
      Some(ComplianceLabour(numOfWorkersSupplied = Some(1000), intermediaryArrangement = Some(true), supplyWorkers = true)),
      SicCode("12345", "the flu", "sic details"),
      businessActivities = List(SicCode("99999", "fooBar", "other foo"))
    ))
    "return an amended SicAndCompliance Model when an entry already exists in the repo for 1 field" in new Setup {
      val amendedModel: Option[SicAndCompliance] = validSicAndCompliance.map(a => a.copy(businessDescription = "fooBarWizz"))
      val result: Future[SicAndCompliance] = for {
        _ <- repository.insert(vatScheme.copy(sicAndCompliance = validSicAndCompliance))
        _ = count mustBe 1
        res <- repository.updateSicAndCompliance(vatScheme.id, amendedModel.get)
      } yield res

      await(result) mustBe amendedModel.get
    }
    "return an amended Option SicAndCompliance Model when an entry already exists and all fields have changed in the model" in new Setup {
      val amendedModel: SicAndCompliance = SicAndCompliance(
        "foo",
        Some(ComplianceLabour(numOfWorkersSupplied = Some(1), intermediaryArrangement = None, supplyWorkers = true)),
        SicCode("foo", "bar", "wizz"),
        businessActivities = List(SicCode("11111", "barFoo", "amended other foo")))
      val result: Future[SicAndCompliance] = for {
        _ <- repository.insert(vatScheme.copy(sicAndCompliance = validSicAndCompliance))
        res <- repository.updateSicAndCompliance(vatScheme.id, amendedModel)
      } yield res
      await(result) mustBe amendedModel
    }
    "return an SicAndComplance Model when the block did not exist in the existing reg doc" in new Setup {
      val result: Future[SicAndCompliance] = for {
        _ <- repository.insert(vatScheme)
        _ = count mustBe 1
        res <- repository.updateSicAndCompliance(vatScheme.id, validSicAndCompliance.get)
      } yield res
      await(result) mustBe validSicAndCompliance.get
    }

    "return an MissingRegDocument if registration document does not exist for the registration id" in new Setup {
      val result: Future[SicAndCompliance] = repository.updateSicAndCompliance("madeUpRegId", validSicAndCompliance.get)
      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
  }
  "calling getBusinessContact" should {

    "return a BusinessContact Model from existing data based on the reg Id" in new Setup {
      val result: Future[Option[BusinessContact]] = for {
        _ <- repository.insert(vatScheme)
        _ <- repository.updateBusinessContact(vatScheme.id, testBusinessContactDetails)
        _ = count mustBe 1
        res <- repository.fetchBusinessContact(vatScheme.id)
      } yield res

      await(result).get mustBe testBusinessContactDetails
    }
    "return None from an existing registration that exists but BusinessContact does not exist" in new Setup {
      val result: Future[Option[BusinessContact]] = for {
        _ <- repository.insert(vatScheme)
        res <- repository.fetchBusinessContact(vatScheme.id)
      } yield res
      await(result) mustBe None
    }
    "return a MissingRegDocument when nothing is returned from mongo for the reg id" in new Setup {
      val result: Future[Option[BusinessContact]] = repository.fetchBusinessContact("madeUpRegId")

      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
    "return an exception if the json is incorrect in the repository (an element is missing)" in new Setup {
      val json: JsValue = Json.toJson(
        Json.obj("registrationId" -> testRegId,
          "status" -> VatRegStatus.draft,
          "businessContact" -> Json.toJson(testBusinessContactDetails).as[JsObject].-("digitalContact")))
      insert(json.as[JsObject])
      an[Exception] mustBe thrownBy(await(repository.fetchBusinessContact(vatScheme.id)))
    }
  }
  "calling updateBusinessContact" should {
    "return an amended Business Contact Model when an entry already exists in the repo for 1 field" in new Setup {
      val amendedModel: BusinessContact = testBusinessContactDetails.copy(website = Some("fooBARUpdated"))

      val result: Future[BusinessContact] = for {
        _ <- repository.insert(vatScheme.copy(businessContact = Some(testBusinessContactDetails)))
        _ = count mustBe 1
        res <- repository.updateBusinessContact(vatScheme.id, amendedModel)
      } yield res

      await(result) mustBe amendedModel
    }
    "return an amended Option BusinessContact Model when an entry already exists and all fields have changed in the model" in new Setup {
      val amendedModel: BusinessContact = testBusinessContactDetails.copy(
        digitalContact = DigitalContact("foozle", Some("2434738"), Some("37483784")),
        website = Some("myLittleWebsite"),
        ppob = Address("lino1", "lino2", None, None, None, Some(testCountry))
      )
      val result: Future[BusinessContact] = for {
        _ <- repository.insert(vatScheme.copy(businessContact = Some(testBusinessContactDetails)))
        res <- repository.updateBusinessContact(vatScheme.id, amendedModel)
      } yield res
      await(result) mustBe amendedModel
    }
    "return an BusinessContact Model when the block did not exist in the existing reg doc" in new Setup {
      val result: Future[BusinessContact] = for {
        _ <- repository.insert(vatScheme)
        _ = count mustBe 1
        res <- repository.updateBusinessContact(vatScheme.id, testBusinessContactDetails)
      } yield res
      await(result) mustBe testBusinessContactDetails
    }

    "return an MissingRegDocument if registration document does not exist for the registration id" in new Setup {
      val result: Future[BusinessContact] = repository.updateBusinessContact("madeUpRegId", testBusinessContactDetails)
      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
  }
  "fetchFlatRateScheme" should {
    "return flat rate scheme data from an existing registration containing data" in new Setup {
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- repository.insert(vatScheme.copy(flatRateScheme = Some(testFlatRateScheme)))
        res <- repository.fetchFlatRateScheme(vatScheme.id)
      } yield res

      await(result) mustBe Some(testFlatRateScheme)
    }
    "return None from an existing registration containing no data" in new Setup {
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- repository.insert(vatScheme)
        res <- repository.fetchFlatRateScheme(vatScheme.id)
      } yield res

      await(result) mustBe None
    }
    "throw a MissingRegDocument for a none existing registration" in new Setup {
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- repository.insert(vatScheme.copy(flatRateScheme = Some(testFlatRateScheme)))
        res <- repository.fetchFlatRateScheme("wrongRegId")
      } yield res

      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
  }
  "updateFlatRateScheme" should {
    "update flat rate scheme block in registration when there is no flat rate scheme data" in new Setup {
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- repository.insert(vatScheme)
        _ <- repository.updateFlatRateScheme(vatScheme.id, testFlatRateScheme)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.flatRateScheme

      await(result) mustBe Some(testFlatRateScheme)
    }
    "update flat rate scheme block in registration when there is already flat rate scheme data" in new Setup {
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- repository.insert(vatScheme.copy(flatRateScheme = Some(testFlatRateScheme)))
        _ <- repository.updateFlatRateScheme(vatScheme.id, testFlatRateScheme)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.flatRateScheme

      await(result) mustBe Some(testFlatRateScheme)
    }
    "not update or insert flat rate scheme if registration does not exist" in new Setup {
      await(repository.insert(vatScheme))

      count mustBe 1
      await(repository.findAll()).head mustBe vatScheme

      a[MissingRegDocument] mustBe thrownBy(await(repository.updateFlatRateScheme("wrongRegId", testFlatRateScheme)))
    }
  }
  "removeFlatRateScheme" should {
    "remove a flatRateScheme block in the registration document if it exists in the registration doc" in new Setup {
      val result: Future[Option[VatScheme]] = for {
        _ <- repository.insert(vatScheme.copy(flatRateScheme = Some(testFlatRateScheme)))
        _ <- repository.removeFlatRateScheme(vatScheme.id)
        updatedScheme <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme

      await(result) mustBe Some(vatScheme)
    }
    "throw a MissingRegDocument if the vat scheme does not exist for the regId" in new Setup {
      a[MissingRegDocument] mustBe thrownBy(await(repository.removeFlatRateScheme(vatScheme.id)))
    }
  }
  "getInternalId" should {
    "return a Future[Option[String]] containing Some(InternalId)" in new Setup {
      val result: Future[Option[String]] = for {
        _ <- repository.insert(vatScheme)
        result <- repository.getInternalId(vatScheme.id)

      } yield result
      await(result) mustBe Some(testInternalid)
    }
    "return a None when no regId document is found" in new Setup {
      await(repository.getInternalId(vatScheme.id)) mustBe None
    }
  }
  "getApplicantDetails" should {
    "return applicant details if they exist" in new Setup {
      await(repository.insert(vatScheme.copy(applicantDetails = Some(testUnregisteredApplicantDetails))))
      await(repository.count) mustBe 1

      val res = await(repository.getApplicantDetails(vatScheme.id))
      res mustBe Some(testUnregisteredApplicantDetails)
    }
    "return None if the record is missing" in new Setup {
      await(repository.insert(vatScheme))
      await(repository.count) mustBe 1

      val res = await(repository.getApplicantDetails(vatScheme.id))
      res mustBe None
    }
    "return an exception if no vatscheme doc exists" in new Setup {
      intercept[MissingRegDocument](await(repository.getApplicantDetails("1")))
    }
  }
  "patchApplicantDetails" should {
    "patch with details only" in new Setup {
      val updatedApplicantDetails = testUnregisteredApplicantDetails.copy(previousAddress = Some(testAddress))

      await(repository.insert(vatScheme))
      await(repository.count) mustBe 1
      await(repository.patchApplicantDetails(vatScheme.id, updatedApplicantDetails))
      await(repository.count) mustBe 1
      (fetchAll.get \ "applicantDetails").as[JsObject] mustBe Json.toJson(updatedApplicantDetails)
    }
  }
  "getEligibilityData" should {
    "return some of eligibilityData" in new Setup {
      await(repository.insert(vatScheme.copy(eligibilityData = Some(jsonEligiblityData))))
      await(repository.count) mustBe 1

      await(repository.fetchEligibilityData(vatScheme.id)) mustBe Some(jsonEligiblityData)
    }
    "return None of eligibilityData" in new Setup {
      await(repository.insert(vatScheme))
      await(repository.count) mustBe 1

      await(repository.fetchEligibilityData(vatScheme.id)) mustBe None
    }
  }
  "updateEligibilityData" should {
    "update eligibilityData successfully when no eligibilityData block exists" in new Setup {
      await(repository.insert(vatScheme))
      await(repository.count) mustBe 1

      await(repository.fetchEligibilityData(vatScheme.id)) mustBe None

      val res: JsObject = await(repository.updateEligibilityData(vatScheme.id, jsonEligiblityData))
      res mustBe jsonEligiblityData

      await(repository.fetchEligibilityData(vatScheme.id)) mustBe Some(jsonEligiblityData)
    }
    "update eligibilityData successfully when eligibilityData block already exists" in new Setup {
      val newJsonEligiblityData = Json.obj("wizz" -> "new bar")

      await(repository.insert(vatScheme.copy(eligibilityData = Some(jsonEligiblityData))))
      await(repository.count) mustBe 1

      await(repository.fetchEligibilityData(vatScheme.id)) mustBe Some(jsonEligiblityData)

      val res: JsObject = await(repository.updateEligibilityData(vatScheme.id, newJsonEligiblityData))
      res mustBe newJsonEligiblityData

      await(repository.fetchEligibilityData(vatScheme.id)) mustBe Some(newJsonEligiblityData)
    }
  }
  "calling fetchNrsSubmissionPayload" should {
    val testPayload = "testPayload"

    "return an encoded payload string from existing data based on the reg Id" in new Setup {
      val result: Future[Option[String]] = for {
        _ <- repository.insert(vatScheme)
        _ <- repository.updateNrsSubmissionPayload(vatScheme.id, testPayload)
        _ = count mustBe 1
        res <- repository.fetchNrsSubmissionPayload(vatScheme.id)
      } yield res

      await(result).get mustBe testPayload
    }
    "return None from an existing registration that exists but the payload does not exist" in new Setup {
      val result: Future[Option[String]] = for {
        _ <- repository.insert(vatScheme)
        res <- repository.fetchNrsSubmissionPayload(vatScheme.id)
      } yield res

      await(result) mustBe None
    }
    "return a MissingRegDocument when nothing is returned from mongo for the reg id" in new Setup {
      val result: Future[Option[SicAndCompliance]] = repository.fetchSicAndCompliance("madeUpRegId")

      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
  }
  "calling updateNrsSubmissionPayload" should {
    val testPayload = "testPayload"

    "return an updated payload string when an entry already exists in the repo" in new Setup {
      val testOldPayload = "testOldPayload"

      val result: Future[String] = for {
        _ <- repository.insert(vatScheme.copy(nrsSubmissionPayload = Some(testOldPayload)))
        _ = count mustBe 1
        res <- repository.updateNrsSubmissionPayload(vatScheme.id, testPayload)
      } yield res

      await(result) mustBe testPayload
    }
    "return the payload string after storing it" in new Setup {
      val result: Future[String] = for {
        _ <- repository.insert(vatScheme)
        res <- repository.updateNrsSubmissionPayload(vatScheme.id, testPayload)
      } yield res
      await(result) mustBe testPayload
    }
    "return an MissingRegDocument if registration document does not exist for the registration id" in new Setup {
      val result: Future[String] = repository.updateNrsSubmissionPayload("madeUpRegId", testPayload)
      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
  }
}

