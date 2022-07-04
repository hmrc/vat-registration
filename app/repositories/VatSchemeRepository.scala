/*
 * Copyright 2022 HM Revenue & Customs
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

package repositories

import auth.{AuthorisationResource, CryptoSCRS}
import common.exceptions._
import config.BackendConfig
import enums.VatRegStatus
import models.api._
import models.api.returns.Returns
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.Cursor.FailOnError
import reactivemongo.api.commands.WriteResult.Message
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.api.{ReadPreference, WriteConcern}
import reactivemongo.bson.{BSONDocument, BSONInteger, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import utils.{JsonErrorUtil, TimeMachine}

import java.time.ZoneOffset
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// scalastyle:off
@Singleton
class VatSchemeRepository @Inject()(mongo: ReactiveMongoComponent,
                                    crypto: CryptoSCRS,
                                    timeMachine: TimeMachine,
                                    backendConfig: BackendConfig
                                   )(implicit executionContext: ExecutionContext)
  extends ReactiveRepository[VatScheme, BSONObjectID](
    collectionName = "registration-information",
    mongo = mongo.mongoConnector.db,
    domainFormat = VatScheme.format(Some(crypto))
  ) with ReactiveMongoFormats with AuthorisationResource with JsonErrorUtil {

  startUp

  private val bankAccountCryptoFormatter = BankAccountMongoFormat.encryptedFormat(crypto)
  private val acknowledgementRefPrefix = "VRS"
  private val omitIdProjection = Json.obj("_id" -> 0)

  def startUp: Future[Unit] = collection.indexesManager.list() map { indexes =>
    logger.info("[Startup] Outputting current indexes")
    indexes foreach { index =>
      val name = index.name.getOrElse("<no-name>")
      val keys = (index.key map { case (k, a) => s"$k -> ${a.value}" }) mkString (",")
      logger.info(s"[Index] name: $name keys: $keys unique: ${index.unique} sparse: ${index.sparse}")
    }
    logger.info("[Startup] Finished outputting current indexes")
  }

  override def indexes: Seq[Index] = Seq(
    Index(
      name = Some("RegId"),
      key = Seq("registrationId" -> IndexType.Ascending),
      unique = true
    ),
    Index(
      name = Some("RegIdAndInternalId"),
      key = Seq(
        "registrationId" -> IndexType.Ascending,
        "internalId" -> IndexType.Ascending
      ),
      unique = true
    ),
    Index(
      name = Some("TTL"),
      key = Seq(
        "timestamp" -> IndexType.Ascending
      ),
      unique = false,
      options = BSONDocument("expireAfterSeconds" -> BSONInteger(backendConfig.expiryInSeconds))
    )
  )

  def getInternalId(id: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val projection = Some(Json.obj("internalId" -> 1, "_id" -> 0))
    collection.find(registrationSelector(id), projection).one[JsObject].map {
      _.map(js => (js \ "internalId").as[String])
    }
  }

  @deprecated("migrate to the new /registrations API")
  def fetchBlock[T](regId: String, key: String)(implicit rds: Reads[T]): Future[Option[T]] = {
    val projection = Some(Json.obj(key -> 1))
    collection.find(registrationSelector(regId), projection).one[JsObject].map { doc =>
      doc.fold(throw MissingRegDocument(regId)) { js =>
        (js \ key).validateOpt[T].get
      }
    }
  }

  @deprecated("migrate to the new /registrations API")
  def updateBlock[T](regId: String, data: T, key: String = "")(implicit writes: Writes[T]): Future[T] = {
    def toCamelCase(str: String): String = str.head.toLower + str.tail

    val selectorKey = if (key == "") toCamelCase(data.getClass.getSimpleName) else key

    val setDoc = Json.obj("$set" -> Json.obj(selectorKey -> Json.toJson(data)))
    collection.update.one(registrationSelector(regId), setDoc) map { updateResult =>
      if (updateResult.n == 0) {
        logger.warn(s"[${data.getClass.getSimpleName}] updating for regId : $regId - No document found")
        throw MissingRegDocument(regId)
      } else {
        logger.info(s"[${data.getClass.getSimpleName}] updating for regId : $regId - documents modified : ${updateResult.nModified}")
        data
      }
    } recover {
      case e =>
        logger.warn(s"Unable to update ${toCamelCase(data.getClass.getSimpleName)} for regId: $regId, Error: ${e.getMessage}")
        throw e
    }
  }

  def createNewVatScheme(regId: String, intId: String): Future[VatScheme] = {
    val set = Json.obj(
      "registrationId" -> Json.toJson[String](regId),
      "status" -> Json.toJson(VatRegStatus.draft),
      "internalId" -> Json.toJson[String](intId),
      "createdDate" -> Json.toJson(timeMachine.today),
      "timestamp" -> Json.obj("$date" -> timeMachine.timestamp.toInstant(ZoneOffset.UTC).toEpochMilli)
    )
    collection.insert.one(set).map { _ =>
      VatScheme(regId, internalId = intId, status = VatRegStatus.draft, createdDate = Some(timeMachine.today))
    }.recover {
      case e: Exception =>
        logger.error(s"[RegistrationMongoRepository] [createNewVatScheme] threw an exception when attempting to create a new record with exception: ${e.getMessage} for regId: $regId and internalid: $intId")
        throw InsertFailed(regId, "VatScheme")
    }
  }

  def insertVatScheme(vatScheme: VatScheme): Future[VatScheme] = {
    implicit val vatSchemeWrites: OWrites[VatScheme] = VatScheme.format(Some(crypto))

    collection.update.one(registrationSelector(vatScheme.id), vatScheme, upsert = true).map { writeResult =>
      logger.info(s"[RegistrationMongoRepository] [insertVatScheme] successfully stored a preexisting VatScheme")
      vatScheme
    }.recover {
      case e: Exception =>
        logger.error(s"[RegistrationMongoRepository] [insertVatScheme] failed to store a VatScheme with regId: ${vatScheme.id}")
        throw e
    }
  }

  def getAllRegistrations(internalId: String): Future[List[JsValue]] =
    collection
      .find(
        selector = Json.obj("internalId" -> internalId),
        projection = Some(omitIdProjection)
      )
      .cursor[JsValue](ReadPreference.primaryPreferred)
      .collect(maxDocs = -1, FailOnError[List[JsValue]]())

  def getRegistration(internalId: String, regId: String): Future[Option[JsValue]] = {
    collection.find(
      selector = Json.obj("registrationId" -> regId, "internalId" -> internalId),
      projection = Some(omitIdProjection)
    ).one[JsValue]
  }

  def upsertRegistration(internalId: String, regId: String, data: JsValue): Future[Option[JsValue]] = {
    collection.findAndUpdate(
      selector = BSONDocument("registrationId" -> regId, "internalId" -> internalId),
      update = Json.obj("$set" -> data.as[JsObject]),
      fetchNewObject = true,
      upsert = true,
      sort = None,
      fields = Some(Json.obj("_id" -> 0)),
      bypassDocumentValidation = false,
      writeConcern = WriteConcern.Default,
      maxTime = None,
      collation = None,
      arrayFilters = Nil
    ).map { writeResult =>
      logger.info(s"[RegistrationMongoRepository] [insertVatScheme] successfully stored a preexisting VatScheme")
      writeResult.result[JsValue]
    }.recoverWith {
      case e: Exception =>
        logger.error(s"[RegistrationMongoRepository] [insertVatScheme] failed to store a VatScheme with regId: $regId")
        throw new InternalServerException(s"[RegistrationMongoRepository] [insertVatScheme] failed to store a VatScheme with regId: $regId, error: ${e.getMessage}")
    }
  }

  def deleteRegistration(internalId: String, regId: String): Future[Boolean] = {
    collection.delete.one(registrationSelector(regId, Some(internalId))) map { wr =>
      if (!wr.ok) logger.error(s"[deleteVatScheme] - Error deleting vat reg doc for regId $regId - Error: ${Message.unapply(wr)}")
      wr.ok
    }
  }

  def getSection[T](internalId: String, regId: String, section: String)(implicit rds: Reads[T]): Future[Option[T]] = {
    val projection = Some(Json.obj(section -> 1, "_id" -> 0))
    collection.find(registrationSelector(regId, Some(internalId)), projection).one[JsObject].map {
      case Some(json) =>
        (json \ section).validate[T].asOpt
      case _ =>
        logger.warn(s"[RegistrationRepository][getSection] No registration exists with regId: $regId")
        None
    }
  }

  def upsertSection[T](internalId: String, regId: String, section: String = "", data: T)(implicit writes: Writes[T]): Future[Option[T]] = {
    def toCamelCase(str: String): String = str.head.toLower + str.tail

    val selectorKey = if (section == "") toCamelCase(data.getClass.getSimpleName) else section

    val setDoc = Json.obj("$set" -> Json.obj(selectorKey -> Json.toJson(data)))
    collection.update.one(registrationSelector(regId, Some(internalId)), setDoc, upsert = true) map { updateResult =>
      if (updateResult.n == 0) {
        logger.warn(s"[${data.getClass.getSimpleName}] updating for regId : $regId - No document found")
        None
      } else {
        logger.info(s"[${data.getClass.getSimpleName}] updating for regId : $regId - documents modified : ${updateResult.nModified}")
        Some(data)
      }
    } recover {
      case e =>
        logger.warn(s"Unable to update ${toCamelCase(data.getClass.getSimpleName)} for regId: $regId")
        throw new InternalServerException(s"Unable to update section ${toCamelCase(data.getClass.getSimpleName)} for regId: $regId, error: ${e.getMessage}")
    }
  }

  def deleteSection(internalId: String, regId: String, section: String): Future[Boolean] = {
    val update = Json.obj("$unset" -> Json.obj(section -> ""))
    collection.update.one(registrationSelector(regId, Some(internalId)), update) map { updateResult =>
      if (updateResult.n == 0) {
        logger.warn(s"[RegistrationRepository] removing for regId : $regId - No document found")
        true
      } else {
        logger.info(s"[RegistrationRepository] removing for regId : $regId - documents modified : ${updateResult.nModified}")
        true
      }
    } recover {
      case e =>
        logger.warn(s"[RegistrationRepository] Unable to remove for regId: $regId, Error: ${e.getMessage}")
        throw new InternalServerException(s"Unable to delete section $section for regId: $regId, error: ${e.getMessage}")
    }
  }

  def retrieveVatScheme(regId: String): Future[Option[VatScheme]] = {
    implicit val format = VatScheme.format(Some(crypto))
    find("registrationId" -> regId).map(_.headOption)
  }

  // TODO: Remove deprecated methods once migration to new /registrations API is complete

  def retrieveVatSchemeByInternalId(id: String): Future[Option[VatScheme]] = {
    implicit val format = VatScheme.format(Some(crypto))
    collection.find[JsObject, VatScheme](Json.obj("internalId" -> id), None).sort(Json.obj("_id" -> -1)).one[VatScheme]
  }

  def updateSubmissionStatus(regId: String, status: VatRegStatus.Value): Future[Boolean] = {
    val modifier = toBSON(Json.obj(
      "status" -> status
    )).get

    collection.update.one(registrationSelector(regId), BSONDocument("$set" -> modifier)).map(_.ok)
  }

  def finishRegistrationSubmission(regId: String, status: VatRegStatus.Value, formBundleId: String): Future[VatRegStatus.Value] = {
    val modifier = toBSON(Json.obj(
      "status" -> status,
      "acknowledgementReference" -> s"$acknowledgementRefPrefix$formBundleId"
    )).get

    collection.update.one(registrationSelector(regId), BSONDocument("$set" -> modifier)).map(_ => status)
  }

  @deprecated("migrate to the new /registrations API")
  def fetchReturns(regId: String): Future[Option[Returns]] = {
    val selector = registrationSelector(regId)
    val projection = Some(Json.obj("returns" -> 1))
    collection.find(selector, projection).one[JsObject].map { doc =>
      doc.flatMap { js =>
        (js \ "returns").validateOpt[Returns].get
      }
    }
  }

  @deprecated("migrate to the new /registrations API")
  def retrieveTradingDetails(regId: String): Future[Option[TradingDetails]] = {
    fetchBlock[TradingDetails](regId, "tradingDetails")
  }

  @deprecated("migrate to the new /registrations API")
  def updateTradingDetails(regId: String, tradingDetails: TradingDetails): Future[TradingDetails] = {
    updateBlock(regId, tradingDetails, "tradingDetails")
  }

  @deprecated("migrate to the new /registrations API")
  def updateReturns(regId: String, returns: Returns): Future[Returns] = {
    val selector = registrationSelector(regId)
    val update = BSONDocument("$set" -> BSONDocument("returns" -> Json.toJson(returns)))
    collection.update.one(selector, update) map { updateResult =>
      logger.info(s"[Returns] updating returns for regId : $regId - documents modified : ${updateResult.nModified}")
      returns
    }
  }

  @deprecated("migrate to the new /registrations API")
  def fetchBankAccount(regId: String): Future[Option[BankAccount]] = {
    val selector = registrationSelector(regId)
    val projection = Some(Json.obj("bankAccount" -> 1))
    collection.find(selector, projection).one[JsObject].map(
      _.flatMap(js => (js \ "bankAccount").validateOpt(bankAccountCryptoFormatter).get)
    )
  }

  @deprecated("migrate to the new /registrations API")
  def updateBankAccount(regId: String, bankAccount: BankAccount): Future[BankAccount] = {
    val selector = registrationSelector(regId)
    val update = BSONDocument("$set" -> Json.obj("bankAccount" -> Json.toJson(bankAccount)(bankAccountCryptoFormatter)))
    collection.update.one(selector, update) map { updateResult =>
      logger.info(s"[Returns] updating bank account for regId : $regId - documents modified : ${updateResult.nModified}")
      bankAccount
    }
  }

  private[repositories] def registrationSelector(regId: String, internalId: Option[String] = None) =
    BSONDocument("registrationId" -> regId) ++
      internalId.map(id => BSONDocument("internalId" -> id))
        .getOrElse(BSONDocument())

  @deprecated("migrate to the new /registrations API")
  def removeFlatRateScheme(regId: String): Future[Boolean] = {
    val selector = registrationSelector(regId)
    val update = BSONDocument("$unset" -> BSONDocument("flatRateScheme" -> ""))
    collection.update.one(selector, update) map { updateResult =>
      if (updateResult.n == 0) {
        logger.warn(s"[RegistrationMongoRepository][removeFlatRateScheme] removing for regId : $regId - No document found")
        throw MissingRegDocument(regId)
      } else {
        logger.info(s"[RegistrationMongoRepository][removeFlatRateScheme] removing for regId : $regId - documents modified : ${updateResult.nModified}")
        true
      }
    } recover {
      case e =>
        logger.warn(s"[RegistrationMongoRepository][removeFlatRateScheme] Unable to remove for regId: $regId, Error: ${e.getMessage}")
        throw e
    }
  }

  def fetchNrsSubmissionPayload(regId: String): Future[Option[String]] =
    fetchBlock[String](regId, "nrsSubmissionPayload")

  def updateNrsSubmissionPayload(regId: String, encodedHTML: String): Future[String] =
    updateBlock[String](regId, encodedHTML, "nrsSubmissionPayload")

  @deprecated("migrate to the new /registrations API")
  def fetchFlatRateScheme(regId: String): Future[Option[FlatRateScheme]] =
    fetchBlock[FlatRateScheme](regId, "flatRateScheme")

  @deprecated("migrate to the new /registrations API")
  def updateFlatRateScheme(regId: String, flatRateScheme: FlatRateScheme): Future[FlatRateScheme] =
    updateBlock(regId, flatRateScheme, "flatRateScheme")

  def fetchEligibilityData(regId: String): Future[Option[JsObject]] =
    fetchBlock[JsObject](regId, "eligibilityData")

  def updateEligibilityData(regId: String, eligibilityData: JsObject): Future[JsObject] =
    updateBlock(regId, eligibilityData, "eligibilityData")

  @deprecated("migrate to the new /registrations API")
  def fetchEligibilitySubmissionData(regId: String): Future[Option[EligibilitySubmissionData]] =
    fetchBlock[EligibilitySubmissionData](regId, "eligibilitySubmissionData")

  @deprecated("migrate to the new /registrations API")
  def updateEligibilitySubmissionData(regId: String, eligibilitySubmissionData: EligibilitySubmissionData): Future[EligibilitySubmissionData] =
    updateBlock(regId, eligibilitySubmissionData, "eligibilitySubmissionData")

  def storeHonestyDeclaration(regId: String, honestyDeclarationData: Boolean): Future[Boolean] =
    updateBlock(regId, honestyDeclarationData, "confirmInformationDeclaration")

}