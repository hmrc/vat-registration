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

import com.mongodb.client.model.Indexes.ascending
import config.BackendConfig
import models.api.DailyQuota
import models.submission.PartyType
import org.mongodb.scala.model
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Updates.inc
import org.mongodb.scala.model._
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import utils.TimeMachine

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DailyQuotaRepository @Inject()(mongo: MongoComponent,
                                     timeMachine: TimeMachine)
                                    (implicit ec: ExecutionContext, backendConfig: BackendConfig)
  extends PlayMongoRepository[DailyQuota](
    mongoComponent = mongo,
    collectionName = "daily-quota",
    domainFormat = DailyQuota.format,
    indexes = Seq(
      model.IndexModel(
        keys = ascending("date", "partyType", "isEnrolled"),
        indexOptions = IndexOptions()
          .name("compoundQuotaIndex")
          .unique(true)
      ),
      model.IndexModel(
        keys = ascending("date"),
        indexOptions = IndexOptions()
          .name("lastModified")
          .expireAfter(backendConfig.dailyQuotaExpiryInSeconds, TimeUnit.SECONDS)
      )
    )
  ) {

  private val defaultQuota = 0

  def currentTotal(partyType: PartyType, isEnrolled: Boolean): Future[Int] = {
      collection.find(filter = and (
        equal("date", timeMachine.today.toString),
        equal("partyType", Codecs.toBson(partyType)),
        equal("isEnrolled", isEnrolled)
      )).toFuture().map(_.headOption.map(_.currentTotal).getOrElse(defaultQuota))
    }

  def incrementTotal(partyType: PartyType, isEnrolled: Boolean): Future[Int] = {
    collection.findOneAndUpdate(
      filter = and (
        equal("date", timeMachine.today.toString),
        equal("partyType", Codecs.toBson(partyType)),
        equal("isEnrolled", isEnrolled)),
      update = inc(fieldName = "currentTotal", number = 1),
      options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER).upsert(true)
    ).toFutureOption().map(_.getOrElse(
      throw new Exception("Unexpected exception while trying to update daily quota")
    )).map(_.currentTotal)
  }

}
