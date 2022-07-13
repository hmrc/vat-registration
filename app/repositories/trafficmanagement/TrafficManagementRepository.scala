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

package repositories.trafficmanagement

import auth.AuthorisationResource
import com.mongodb.client.model.Indexes.ascending
import config.BackendConfig
import models.api.{RegistrationChannel, RegistrationInformation, RegistrationStatus}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model._
import org.mongodb.scala.{Document, model}
import play.api.Logging
import play.api.libs.functional.syntax._
import play.api.libs.json.{Format, JsError, JsSuccess, Json, Reads, __}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZoneOffset}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TrafficManagementRepository @Inject()(mongo: MongoComponent,
                                            backendConfig: BackendConfig
                                           )(implicit ec: ExecutionContext)
  extends PlayMongoRepository(
    mongoComponent = mongo,
    collectionName = "traffic-management",
    domainFormat = RegistrationInformation.format,
    indexes = Seq(
      model.IndexModel(
        keys = ascending("registrationId", "internalId"),
        indexOptions = IndexOptions()
          .name("intAndRegIdCompositeKey")
          .unique(true)
      ),
      model.IndexModel(
        keys = ascending("lastModified"),
        indexOptions = IndexOptions()
          .name("lastModified")
          .expireAfter(backendConfig.expiryInSeconds, TimeUnit.SECONDS)
      )
    )
  ) with AuthorisationResource with Logging {

  def getRegInfoById(internalId: String, registrationId: String): Future[Option[RegistrationInformation]] = {
    collection.find(filter = and(
      equal("internalId", internalId),
      equal("registrationId", registrationId)
    )).toFuture().map(_.headOption)
  }

  def upsertRegInfoById(internalId: String,
                        regId: String,
                        status: RegistrationStatus,
                        regStartDate: LocalDate,
                        channel: RegistrationChannel,
                        lastModified: LocalDateTime): Future[RegistrationInformation] = {

    val newRecord = RegistrationInformation(
      internalId = internalId,
      registrationId = regId,
      status = status,
      regStartDate = regStartDate,
      channel = channel,
      lastModified = lastModified
    )

    collection.replaceOne(
      filter = and(
        equal("internalId", internalId),
        equal("registrationId", regId)),
      replacement = newRecord,
      options = ReplaceOptions().upsert(true)
    ).toFuture().map { result =>
      if (!result.wasAcknowledged()) {
        throw new Exception("Unexpected error when inserting registration information")
      } else {
        newRecord
      }
    }
  }

  override def getInternalId(id: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    collection.find(filter = equal("internalId", id))
      .toFuture().map(_.headOption.map(_.internalId))
  }

  def deleteRegInfoById(internalId: String, registrationId: String): Future[Boolean] = {
    collection.deleteOne(filter = and(
      equal("internalId", internalId),
      equal("registrationId", registrationId)
    )).toFuture().map { result =>
      if (!result.wasAcknowledged()) {
        logger.error(s"[clearDocument] - Error deleting traffic management doc for internalId $internalId")
      }
      result.wasAcknowledged()
    }
  }

  def runOnce: Unit = {
    collection.find[Document]().subscribe { regInfo =>
      val regInfoJson = Json.parse(regInfo.toJson())

      try {
        (regInfoJson \ "lastModified").validateOpt[LocalDate] match {
          case JsSuccess(Some(localDate), _) => updateOldDate(LocalDateTime.of(localDate, LocalTime.now()))
          case JsSuccess(None, _) => updateOldDate(LocalDateTime.now())
          case JsError(_) =>
            logger.info(s"[TrafficManagementRepository][runOnce] skipped updating Registration Information")
        }

        def updateOldDate(localDateTime: LocalDateTime) = {
          val updatedRegInfo: RegistrationInformation = (
            (regInfoJson \ "internalId").validate[String] and
              (regInfoJson \ "registrationId").validate[String] and
              (regInfoJson \ "status").validate[RegistrationStatus] and
              (regInfoJson \ "regStartDate").validate[LocalDate] and
              (regInfoJson \ "channel").validate[RegistrationChannel]
            ) ((internalId, regId, status, startDate, channel) =>
            RegistrationInformation(
              internalId,
              regId,
              status,
              startDate,
              channel,
              localDateTime
            )
          ).getOrElse(throw new InternalServerException(""))

          collection.replaceOne(
            filter = and(
              equal("internalId", updatedRegInfo.internalId),
              equal("registrationId", updatedRegInfo.registrationId)
            ),
            replacement = updatedRegInfo,
            options = ReplaceOptions().upsert(true)
          ).toFuture().map { _ =>
            logger.info(s"[TrafficManagementRepository][runOnce] successfully updated Registration Information")
          }
        }
      } catch {
        case _ =>
          logger.error(s"[TrafficManagementRepository][runOnce] unexpected error occurred while updating Registration Information")
      }
    }
  }

  runOnce

}
