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

import java.time.LocalDate
import auth.AuthorisationResource
import config.BackendConfig

import javax.inject.{Inject, Singleton}
import models.api.{RegistrationChannel, RegistrationInformation, RegistrationStatus}
import play.api.libs.json.{JsString, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult.Message
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONInteger}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TrafficManagementRepository @Inject()(mongo: ReactiveMongoComponent,
                                            backendConfig: BackendConfig
                                           )(implicit ec: ExecutionContext)
  extends ReactiveRepository(
    collectionName = "traffic-management",
    mongo = mongo.mongoConnector.db,
    domainFormat = RegistrationInformation.format
  ) with AuthorisationResource {

  val lastModifiedIndex = Index(
    name = Some("lastModified"),
    key = Seq("lastModified" -> IndexType.Ascending),
    options = BSONDocument("expireAfterSeconds" -> BSONInteger(backendConfig.expiryInSeconds))
  )

  override def indexes: Seq[Index] = {
    Seq(
      Index(
        name = Some("intAndRegIdCompositeKey"),
        key = Seq(
          "registrationId" -> IndexType.Ascending,
          "internalId" -> IndexType.Ascending
        ),
        unique = true
      ),
      lastModifiedIndex
    )
  }

  def getRegInfoById(internalId: String, registrationId: String): Future[Option[RegistrationInformation]] =
    find(
      "internalId" -> JsString(internalId),
      "registrationId" -> JsString(registrationId)
    ).map(_.headOption)

  def upsertRegInfoById(internalId: String,
                        regId: String,
                        status: RegistrationStatus,
                        regStartDate: LocalDate,
                        channel: RegistrationChannel,
                        lastModified: LocalDate): Future[RegistrationInformation] = {

    val newRecord = RegistrationInformation(
      internalId = internalId,
      registrationId = regId,
      status = status,
      regStartDate = regStartDate,
      channel = channel,
      lastModified = lastModified
    )

    val selector = Json.obj("internalId" -> internalId, "registrationId" -> regId)
    val modifier = Json.obj("$set" -> Json.toJson(newRecord))

    findAndUpdate(selector, modifier, fetchNewObject = true, upsert = true)
      .map(_.result[RegistrationInformation] match {
        case Some(regInfo) => regInfo
        case _ => throw new Exception("Unexpected error when inserting registration information")
      })
  }

  override def getInternalId(id: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    find("internalId" -> JsString(id))
      .map(_.headOption.map(_.internalId))

  def deleteRegInfoById(internalId: String, registrationId: String): Future[Boolean] = {
    collection.delete.one(BSONDocument(
      "internalId" -> internalId,
      "registrationId" -> registrationId
    )).map { res =>
      if (!res.ok) logger.error(s"[clearDocument] - Error deleting traffic management doc for internalId $internalId - Error: ${Message.unapply(res)}")
      res.ok
    }
  }
}
