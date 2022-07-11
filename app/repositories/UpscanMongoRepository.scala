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

import com.mongodb.client.model.Indexes.ascending
import config.BackendConfig
import models.api.UpscanDetails
import org.mongodb.scala.model
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model._
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import utils.TimeMachine

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpscanMongoRepository @Inject()(mongo: MongoComponent,
                                      timeMachine: TimeMachine,
                                      backendConfig: BackendConfig)(implicit ec: ExecutionContext)
  extends PlayMongoRepository(
    mongoComponent = mongo,
    collectionName = "upscan",
    domainFormat = UpscanDetails.format,
    indexes = Seq(
      model.IndexModel(
        keys = ascending("reference"),
        indexOptions = IndexOptions()
          .name("reference")
          .unique(true)
      ),
      model.IndexModel(
        keys = ascending("reference", "registrationId"),
        indexOptions = IndexOptions()
          .name("referenceAndRegistrationId")
          .unique(true)
      ),
      model.IndexModel(
        keys = ascending("timestamp"),
        indexOptions = IndexOptions()
          .name("TTL")
          .expireAfter(backendConfig.expiryInSeconds, TimeUnit.SECONDS)
      )
    )
  ) {

  val referenceKey = "reference"
  val registrationIdKey = "registrationId"

  def getUpscanDetails(reference: String): Future[Option[UpscanDetails]] = {
    collection
      .find(filter = equal(referenceKey, reference))
      .toFuture().map(_.headOption)
  }

  def getAllUpscanDetails(registrationId: String): Future[Seq[UpscanDetails]] =
    collection
      .find(filter = equal(registrationIdKey, registrationId))
      .toFuture()

  def deleteUpscanDetails(reference: String): Future[Boolean] = {
    collection
      .deleteOne(filter = equal(referenceKey, reference))
      .toFuture().map(_.wasAcknowledged())
  }

  def deleteAllUpscanDetails(registrationId: String): Future[Boolean] =
    collection
      .deleteMany(filter = equal(registrationIdKey, registrationId))
      .toFuture().map(_.wasAcknowledged())

  def upsertUpscanDetails(upscanDetails: UpscanDetails): Future[UpscanDetails] = {
    val upscanList = Seq(
      Updates.set("timestamp", timeMachine.timestamp)
    ) ++ Json.toJson(upscanDetails).as[JsObject]
      .fields.map { case (key, value) => Updates.set(key, Codecs.toBson(value)) }

    collection.updateOne(
      filter = equal(referenceKey, upscanDetails.reference),
      update = Updates.combine(upscanList: _*),
      options = UpdateOptions().upsert(true)
    ).toFuture().map(_ => upscanDetails).recover { case _ =>
      throw new Exception("Unexpected error when inserting upscan details")
    }
  }

}
