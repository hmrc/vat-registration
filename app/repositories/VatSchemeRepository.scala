/*
 * Copyright 2023 HM Revenue & Customs
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

import auth.CryptoSCRS
import config.BackendConfig
import enums.VatRegStatus
import models.api._
import models.registration.{AcknowledgementReferenceSectionId, StatusSectionId}
import org.mongodb.scala.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.Projections.include
import org.mongodb.scala.model.Updates.{combine, set, unset}
import org.mongodb.scala.model._
import play.api.libs.json._
import play.api.mvc.Request
import uk.gov.hmrc.http.InternalServerException
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import utils.{JsonErrorUtil, LoggingUtils, TimeMachine}

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// scalastyle:off
@Singleton
class VatSchemeRepository @Inject() (
                                      mongoComponent: MongoComponent,
                                      crypto: CryptoSCRS,
                                      timeMachine: TimeMachine,
                                      backendConfig: BackendConfig
                                    )(implicit executionContext: ExecutionContext)
  extends PlayMongoRepository[VatScheme](
    collectionName = "registration-information",
    mongoComponent = mongoComponent,
    domainFormat = VatScheme.format(Some(crypto)),
    replaceIndexes = true,
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
  )
    with JsonErrorUtil
    with LoggingUtils {

  private val acknowledgementRefPrefix = "VRS"
  private val timestampKey             = "timestamp"
  private val internalIdKey            = "internalId"
  private val registrationIdKey        = "registrationId"
  private val cutoffDate               = backendConfig.ttCutOffDate

  def getInternalId(id: String): Future[Option[String]] =
    collection
      .find(registrationSelector(id))
      .first()
      .map(_.internalId)
      .headOption()

  def createNewVatScheme(regId: String, intId: String): Future[VatScheme] = {
    val doc = VatScheme(
      registrationId = regId,
      internalId = intId,
      status = VatRegStatus.draft,
      createdDate = timeMachine.today
    )
    collection
      .insertOne(doc)
      .toFuture()
      .flatMap { _ =>
        collection
          .updateOne(registrationSelectorNewApp(regId, Some(intId)), set(timestampKey, timeMachine.timestamp))
          .toFuture()
          .map(_ => doc)
      }
  }

  def getAllRegistrations(internalId: String): Future[List[JsValue]] =
    collection
      .find(internalIdAndDateFilterSelector(internalId))
      .collect()
      .toFuture()
      .map(_.toList.map(scheme => Json.toJson(scheme)(VatScheme.format())))

  def getRegistration(internalId: String, regId: String): Future[Option[VatScheme]] =
    collection
      .find(registrationSelector(regId, Some(internalId)))
      .first()
      .toFutureOption()

  def upsertRegistration(internalId: String, regId: String, scheme: VatScheme): Future[Option[VatScheme]] =
    collection
      .findOneAndReplace(
        filter = registrationSelector(regId, Some(internalId)),
        replacement = scheme,
        options = FindOneAndReplaceOptions().upsert(true)
      )
      .toFutureOption()
      .flatMap { _ =>
        collection
          .updateOne(registrationSelector(regId, Some(internalId)), set(timestampKey, timeMachine.timestamp))
          .toFutureOption()
          .map(_.map(_ => scheme))
      }

  def deleteRegistration(internalId: String, regId: String)(implicit request: Request[_]): Future[Boolean] =
    collection
      .deleteOne(registrationSelector(regId, Some(internalId)))
      .toFuture()
      .map { deletion =>
        if (deletion.getDeletedCount > 0) {
          true
        } else {
          errorLog(s"[deleteVatScheme] - Error deleting vat reg doc for regId $regId")
          deletion.wasAcknowledged()
        }
      }

  def getSection[T](internalId: String, regId: String, section: String)(implicit
                                                                        rds: Reads[T],
                                                                        request: Request[_]
  ): Future[Option[T]] =
    collection
      .find[Document](registrationSelector(regId, Some(internalId)))
      .projection(include(section))
      .headOption()
      .map {
        case Some(doc) =>
          (Json.parse(doc.toJson()) \ section).validate[T].asOpt
        case _ =>
          warnLog(s"[RegistrationRepository][getSection] No registration exists with regId: $regId")
          None
      }

  def upsertSection[T](internalId: String, regId: String, section: String = "", data: T)(implicit
                                                                                         writes: Writes[T],
                                                                                         request: Request[_]
  ): Future[Option[T]] =
    collection
      .updateOne(
        filter = registrationSelector(regId, Some(internalId)),
        update = combine(set(section, Codecs.toBson(data)), set(timestampKey, timeMachine.timestamp)),
        options = UpdateOptions()
      )
      .toFuture()
      .map { result =>
        if (result.getModifiedCount > 0) {
          infoLog(
            s"[${data.getClass.getSimpleName}] updating for regId : $regId - documents modified : ${result.getModifiedCount}"
          )
          Some(data)
        } else {
          warnLog(s"[${data.getClass.getSimpleName}] updating for regId : $regId - No document found")
          None
        }
      }
      .recover { case e =>
        warnLog(s"Unable to update $section for regId: $regId")
        throw new InternalServerException(
          s"Unable to update section $section for regId: $regId, error: ${e.getMessage}"
        )
      }

  def deleteSection(internalId: String, regId: String, section: String)(implicit request: Request[_]): Future[Boolean] =
    collection
      .updateOne(registrationSelector(regId, Some(internalId)), unset(section))
      .toFuture()
      .map { result =>
        if (result.getModifiedCount > 0) {
          infoLog(
            s"[RegistrationRepository] removing for regId : $regId - documents modified : ${result.getModifiedCount}"
          )
          true
        } else {
          warnLog(s"[RegistrationRepository] removing for regId : $regId - No document found")
          true
        }
      }
      .recover { case e =>
        warnLog(s"[RegistrationRepository] Unable to remove for regId: $regId, Error: ${e.getMessage}")
        throw new InternalServerException(
          s"Unable to delete section $section for regId: $regId, error: ${e.getMessage}"
        )
      }

  def updateSubmissionStatus(internalId: String, regId: String, status: VatRegStatus.Value)(implicit
                                                                                            request: Request[_]
  ): Future[Option[VatRegStatus.Value]] = {
    infoLog("[VatSchemeRepository][updateSubmissionStatus] attempting to update submission status", regId)
    upsertSection(internalId, regId, StatusSectionId.repoKey, status)
  }

  def finishRegistrationSubmission(
                                    regId: String,
                                    status: VatRegStatus.Value,
                                    formBundleId: String
                                  ): Future[VatRegStatus.Value] =
    collection
      .updateOne(
        registrationSelector(regId),
        combine(
          set(StatusSectionId.repoKey, status.toString),
          set(AcknowledgementReferenceSectionId.repoKey, s"$acknowledgementRefPrefix$formBundleId")
        )
      )
      .toFuture()
      .map(_ => status)

  private[repositories] def registrationSelector(regId: String, internalId: Option[String] = None) = {

    val baseFilter: Bson = registrationIdFilter(regId)

    val regFilter: Bson = internalId match {
      case Some(id) =>
        and(
          baseFilter,
          internalIdFilter(id),
          draftDateFilter
        )
      case None =>
        baseFilter
    }
    regFilter
  }

  private[repositories] def registrationSelectorNewApp(regId: String, internalId: Option[String] = None) = {
    internalId
      .map(intId => and(equal("registrationId", regId), equal("internalId", intId)))
      .getOrElse(equal("registrationId", regId))
  }

  // Extract the document for a specific user(internalId) provided if its in submitted status or
  // (any other status & application created on or after the cutOffDate)
  private def internalIdAndDateFilterSelector(internalId: String) = {
    val baseFilter: Bson = internalIdFilter(internalId)

    val regFilter: Bson =
      and (
        baseFilter,
        draftDateFilter
      )
    regFilter
  }

  private def internalIdFilter(internalId: String): Bson = equal(internalIdKey, internalId)
  private def registrationIdFilter(registrationId: String): Bson = equal(registrationIdKey, registrationId)
  private def draftDateFilter: Bson = or(and(exists("status", true), equal("status", "submitted")),
      and(exists("status", true), notEqual ("status", "submitted"), Filters.gte ("createdDate", cutoffDate)))

}