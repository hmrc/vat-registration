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
import com.mongodb.client.model.ReturnDocument
import common.exceptions._
import config.BackendConfig
import enums.VatRegStatus
import models.api._
import models.api.vatapplication.{Returns, VatApplication}
import models.registration.VatApplicationSectionId
import org.mongodb.scala.Document
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Indexes.{ascending, descending}
import org.mongodb.scala.model.Projections.{exclude, include}
import org.mongodb.scala.model.Updates.{combine, set, unset}
import org.mongodb.scala.model.{FindOneAndReplaceOptions, IndexModel, IndexOptions, UpdateOptions}
import play.api.Logging
import play.api.libs.json._
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import utils.{JsonErrorUtil, TimeMachine}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// scalastyle:off
@Singleton
class VatSchemeRepository @Inject()(mongoComponent: MongoComponent,
                                    crypto: CryptoSCRS,
                                    timeMachine: TimeMachine,
                                    backendConfig: BackendConfig
                                   )(implicit executionContext: ExecutionContext)
  extends PlayMongoRepository[VatScheme](
    collectionName = "registration-information",
    mongoComponent = mongoComponent,
    domainFormat = VatScheme.format(Some(crypto)),
    indexes = Seq(
      IndexModel(
        keys = ascending("registrationId"),
        indexOptions = IndexOptions()
          .name("RegId")
          .unique(true)
      ),
      IndexModel(
        keys = ascending("registrationId", "internalId"),
        indexOptions = IndexOptions()
          .name("RegIdAndInternalId")
          .unique(true)
      ),
      IndexModel(
        keys = ascending("timestamp"),
        indexOptions = IndexOptions()
          .name("TTL")
          .unique(false)
          .expireAfter(backendConfig.expiryInSeconds, TimeUnit.SECONDS)
      )
    )
  ) with AuthorisationResource with JsonErrorUtil with Logging {

  private val bankAccountCryptoFormatter = BankAccountMongoFormat.encryptedFormat(crypto)
  private val acknowledgementRefPrefix = "VRS"
  private val rootKey = ""
  private val timestampKey = "timestamp"
  private val internalIdKey = "internalId"

  def getInternalId(id: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    collection
      .find(registrationSelector(id))
      .first()
      .map(_.internalId)
      .headOption()

  @deprecated("migrate to the new /registrations API")
  def fetchBlock[T](regId: String, key: String)(implicit rds: Reads[T]): Future[Option[T]] =
    collection
      .find[Document](registrationSelector(regId))
      .projection(include(key))
      .headOption()
      .map {
        case Some(doc) => (Json.parse(doc.toJson()) \ key).validateOpt[T].get
        case _ => throw MissingRegDocument(regId)
      }

  @deprecated("migrate to the new /registrations API")
  def updateBlock[T](regId: String, data: T, key: String)(implicit writes: Writes[T]): Future[T] = {
    collection
      .updateOne(
        filter = registrationSelector(regId),
        update = combine(set(key, Codecs.toBson(data)), set(timestampKey, timeMachine.timestamp)),
        options = UpdateOptions().upsert(false)
      )
      .toFuture()
      .map { result =>
        if (result.getModifiedCount > 0) {
          data
        } else {
          throw MissingRegDocument(regId)
        }
      }
  }

  def createNewVatScheme(regId: String, intId: String): Future[VatScheme] = {
    val doc = VatScheme(
      id = regId,
      internalId = intId,
      status = VatRegStatus.draft,
      createdDate = Some(timeMachine.today)
    )
    collection
      .insertOne(doc)
      .toFuture()
      .flatMap { _ =>
        collection.updateOne(registrationSelector(regId, Some(intId)), set(timestampKey, timeMachine.timestamp))
          .toFuture()
          .map(_ => doc)
      }
  }

  def insertVatScheme(vatScheme: VatScheme): Future[VatScheme] = {
    collection
      .findOneAndReplace(
        filter = registrationSelector(vatScheme.id, Some(vatScheme.internalId)),
        replacement = vatScheme,
        options = FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true)
      )
      .toFuture()
      .recover {
        case e: Exception =>
          logger.error(s"[RegistrationMongoRepository] [insertVatScheme] failed to store a VatScheme with regId: ${vatScheme.id}")
          throw e
      }
    }

  def getAllRegistrations(internalId: String): Future[List[JsValue]] =
    collection
      .find(equal(internalIdKey, internalId))
      .collect()
      .toFuture()
      .map(_.toList.map(scheme => Json.toJson(scheme)(VatScheme.format())))

  def getRegistration(internalId: String, regId: String): Future[Option[JsValue]] =
    collection
      .find(registrationSelector(regId, Some(internalId)))
      .first()
      .toFutureOption()
      .map(_.map(scheme => Json.toJson(scheme)(VatScheme.format())))

  def upsertRegistration(internalId: String, regId: String, data: JsValue): Future[Option[JsValue]] = {
    val json = data.as[JsObject] ++ Json.obj("internalId" -> internalId)
    val scheme = json.validate[VatScheme](VatScheme.format(Some(crypto))).getOrElse(throw new InternalServerException("[upsertRegistration] Couldn't validate given JSON as a VatScheme"))
    collection
      .findOneAndReplace(
        filter = registrationSelector(regId, Some(internalId)),
        replacement = scheme,
        options = FindOneAndReplaceOptions().upsert(true))
      .toFutureOption()
      .flatMap { _ =>
        collection.updateOne(registrationSelector(regId, Some(internalId)), set(timestampKey, timeMachine.timestamp))
          .toFutureOption()
          .map(_.map(_ => data))
      }
  }

  def deleteRegistration(internalId: String, regId: String): Future[Boolean] =
    collection
      .deleteOne(registrationSelector(regId, Some(internalId)))
      .toFuture()
      .map { deletion =>
        if (deletion.getDeletedCount > 0) {
          true
        } else {
          logger.error(s"[deleteVatScheme] - Error deleting vat reg doc for regId $regId")
          deletion.wasAcknowledged()
        }
      }

  def getSection[T](internalId: String, regId: String, section: String)(implicit rds: Reads[T]): Future[Option[T]] = {
    if (section.equals(VatApplicationSectionId.repoKey)) { //TODO Remove if block entirely when removing temp reads
      collection
        .find[Document](registrationSelector(regId, Some(internalId)))
        .projection(exclude("_id"))
        .headOption()
        .map {
          case Some(doc) =>
            val json = Json.parse(doc.toJson())
            (json \ section).validate[VatApplication].orElse(json.validate[VatApplication](VatApplication.tempReads))
              .asOpt.map(Json.toJson[VatApplication])
              .flatMap(_.validate[T].asOpt)
          case None =>
            logger.warn(s"[RegistrationRepository][getSection] No registration exists with regId: $regId")
            None
        }
    } else {
      collection
        .find[Document](registrationSelector(regId, Some(internalId)))
        .projection(include(section))
        .headOption()
        .map {
          case Some(doc) =>
            (Json.parse(doc.toJson()) \ section).validate[T].asOpt
          case _ =>
            logger.warn(s"[RegistrationRepository][getSection] No registration exists with regId: $regId")
            None
        }
    }
  }

  def upsertSection[T](internalId: String, regId: String, section: String = "", data: T)(implicit writes: Writes[T]): Future[Option[T]] =
    collection
      .updateOne(
        filter = registrationSelector(regId, Some(internalId)),
        update = combine(set(section, Codecs.toBson(data)), set(timestampKey, timeMachine.timestamp)),
        options = UpdateOptions().upsert(true)
      )
      .toFuture()
      .map { result =>
        if (result.getModifiedCount > 0) {
          logger.info(s"[${data.getClass.getSimpleName}] updating for regId : $regId - documents modified : ${result.getModifiedCount}")
          Some(data)
        } else {
          logger.warn(s"[${data.getClass.getSimpleName}] updating for regId : $regId - No document found")
          None
        }
      }.recover {
        case e =>
          logger.warn(s"Unable to update $section for regId: $regId")
          throw new InternalServerException(s"Unable to update section $section for regId: $regId, error: ${e.getMessage}")
      }

  def deleteSection(internalId: String, regId: String, section: String): Future[Boolean] =
    collection
      .updateOne(registrationSelector(regId, Some(internalId)), unset(section))
      .toFuture()
      .map { result =>
        if (result.getModifiedCount > 0) {
          logger.info(s"[RegistrationRepository] removing for regId : $regId - documents modified : ${result.getModifiedCount}")
          true
        } else {
          logger.warn(s"[RegistrationRepository] removing for regId : $regId - No document found")
          true
        }
      }.recover {
        case e =>
          logger.warn(s"[RegistrationRepository] Unable to remove for regId: $regId, Error: ${e.getMessage}")
          throw new InternalServerException(s"Unable to delete section $section for regId: $regId, error: ${e.getMessage}")
      }

  def retrieveVatScheme(regId: String): Future[Option[VatScheme]] = {
    implicit val format = VatScheme.format(Some(crypto))
    collection.find(equal("registrationId", regId)).first().headOption()
  }

  // TODO: Remove deprecated methods once migration to new /registrations API is complete

  def retrieveVatSchemeByInternalId(id: String): Future[Option[VatScheme]] = {
    implicit val format = VatScheme.format(Some(crypto))
    collection
      .find(equal("internalId", id))
      .sort(descending("_id"))
      .headOption()
  }

  def updateSubmissionStatus(regId: String, status: VatRegStatus.Value): Future[Boolean] =
    collection
      .updateOne(registrationSelector(regId),  set("status", status.toString))
      .toFuture()
      .map(_.getModifiedCount > 0)

  def finishRegistrationSubmission(regId: String, status: VatRegStatus.Value, formBundleId: String): Future[VatRegStatus.Value] =
    collection.updateOne(registrationSelector(regId), combine(set("status", status.toString), set("acknowledgementReference", s"$acknowledgementRefPrefix$formBundleId")))
      .toFuture()
      .map(_ => status)

  @deprecated("migrate to the new /registrations API")
  def retrieveTradingDetails(regId: String): Future[Option[TradingDetails]] = {
    fetchBlock[TradingDetails](regId, "tradingDetails")
  }

  @deprecated("migrate to the new /registrations API")
  def updateTradingDetails(regId: String, tradingDetails: TradingDetails): Future[TradingDetails] = {
    updateBlock(regId, tradingDetails, "tradingDetails")
  }

  @deprecated("migrate to the new /registrations API")
  def fetchReturns(regId: String): Future[Option[Returns]] =
    fetchBlock[Returns](regId, "returns")

  @deprecated("migrate to the new /registrations API")
  def updateReturns(regId: String, returns: Returns): Future[Returns] =
    updateBlock(regId, returns, "returns")

  @deprecated("migrate to the new /registrations API")
  def fetchBankAccount(regId: String): Future[Option[BankAccount]] =
    collection
      .find[Document](registrationSelector(regId))
      .headOption()
      .map(_.flatMap(doc => (Json.parse(doc.toJson()) \ "bankAccount").validateOpt(bankAccountCryptoFormatter).get))

  @deprecated("migrate to the new /registrations API")
  def updateBankAccount(regId: String, bankAccount: BankAccount): Future[BankAccount] =
    collection
      .updateOne(registrationSelector(regId), set("bankAccount", Codecs.toBson(bankAccount)(bankAccountCryptoFormatter)))
      .toFuture()
      .map { result =>
        logger.info(s"[Returns] updating bank account for regId : $regId - documents modified : ${result.getModifiedCount}")
        bankAccount
      }

  private[repositories] def registrationSelector(regId: String, internalId: Option[String] = None) =
    internalId
      .map(intId => and(equal("registrationId", regId), equal("internalId", intId)))
      .getOrElse(equal("registrationId", regId))

  @deprecated("migrate to the new /registrations API")
  def removeFlatRateScheme(regId: String): Future[Boolean] =
    collection
      .updateOne(registrationSelector(regId), unset("flatRateScheme"))
      .toFuture()
      .map { result =>
        if (result.getMatchedCount == 0) {
          logger.warn(s"[RegistrationMongoRepository][removeFlatRateScheme] removing for regId : $regId - No document found")
          throw MissingRegDocument(regId)
        } else {
          logger.info(s"[RegistrationMongoRepository][removeFlatRateScheme] removing for regId : $regId - documents modified : ${result.getModifiedCount}")
          true
        }
      } recover {
        case e =>
          logger.warn(s"[RegistrationMongoRepository][removeFlatRateScheme] Unable to remove for regId: $regId, Error: ${e.getMessage}")
          throw e
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