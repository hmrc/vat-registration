/*
 * Copyright 2021 HM Revenue & Customs
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

import models.api.UpscanDetails
import play.api.libs.json.{JsString, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import uk.gov.hmrc.mongo.ReactiveRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class UpscanMongoRepository @Inject()(mongo: ReactiveMongoComponent)(implicit ec: ExecutionContext)
  extends ReactiveRepository(
    collectionName = "upscan",
    mongo = mongo.mongoConnector.db,
    domainFormat = UpscanDetails.format
  ) {

  override def indexes: Seq[Index] = Seq(
    Index(
      name = Some("reference"),
      key = Seq("reference" -> IndexType.Ascending),
      unique = true
    ),
    Index(
      name = Some("referenceAndRegistrationId"),
      key = Seq(
        "reference" -> IndexType.Ascending,
        "registrationId" -> IndexType.Ascending
      ),
      unique = true
    )
  )

  def getUpscanDetails(reference: String): Future[Option[UpscanDetails]] =
    find("reference" -> JsString(reference))
      .map(_.headOption)

  def getAllUpscanDetails(registrationId: String): Future[Seq[UpscanDetails]] =
    find("registrationId" -> JsString(registrationId))

  def upsertUpscanDetails(upscanDetails: UpscanDetails): Future[UpscanDetails] = {

    val selector = Json.obj("reference" -> upscanDetails.reference)
    val modifier = Json.obj("$set" -> Json.toJson(upscanDetails))

    findAndUpdate(selector, modifier, fetchNewObject = true, upsert = true)
      .map(_.result[UpscanDetails] match {
        case Some(details) => details
        case _ => throw new Exception("Unexpected error when inserting upscan details")
      })
  }

}
