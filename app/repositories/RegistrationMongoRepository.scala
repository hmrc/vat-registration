/*
 * Copyright 2017 HM Revenue & Customs
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

import javax.inject.{Inject, Named}

import common.Now
import common.exceptions.{InsertFailed, RetrieveFailed, UpdateFailed}
import models._
import org.joda.time.DateTime
import play.api.Logger
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID, _}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RegistrationRepository {


  def createNewVatScheme(registrationId: String)(implicit now:Now[DateTime]): Future[VatScheme]

  def retrieveVatScheme(registrationId: String): Future[Option[VatScheme]]

  def updateVatChoice(registrationId: String, vatChoice: VatChoice) : Future[VatChoice]
}

// this is here for Guice dependency injection of `() => DB`
class MongoDBProvider extends Function0[DB] with MongoDbConnection {
  def apply: DB = db()
}

class RegistrationMongoRepository @Inject()(mongoProvider: Function0[DB], @Named("collectionName") collectionName: String)
  extends ReactiveRepository[VatScheme, BSONObjectID](
    collectionName = collectionName,
    mongo = mongoProvider,
    domainFormat = VatScheme.format
  ) with RegistrationRepository {

  private[repositories] def registrationIdSelector(registrationID: String) = BSONDocument("ID" -> BSONString(registrationID))

  override def indexes: Seq[Index] = Seq(
    Index(
      name = Some("RegId"),
      key = Seq("registrationId" -> IndexType.Ascending),
      unique = true
    )
  )

  override def createNewVatScheme(registrationId: String)(implicit now:Now[DateTime]): Future[VatScheme] = {
    val newReg = VatScheme.blank(registrationId)
    collection.insert(newReg) map {
      _ => newReg
    } recover {
      case e =>
        Logger.error(s"Unable to insert new VAT Scheme for registration ID $registrationId, Error: ${e.getMessage}")
        throw InsertFailed(registrationId, "VatScheme")
    }
  }

  override def retrieveVatScheme(registrationId: String): Future[Option[VatScheme]] = {
    val selector = registrationIdSelector(registrationId)
    collection.find(selector).one[VatScheme] recover {
      case e: Exception =>
        // $COVERAGE-OFF$
        Logger.error(s"Unable to retrieve VAT Scheme for registration ID $registrationId, Error: ${e.getMessage}")
        throw RetrieveFailed(registrationId)
      // $COVERAGE-ON$
    }
  }

  override def updateVatChoice(registrationId: String, vatChoice: VatChoice): Future[VatChoice] = {
    val selector = registrationIdSelector(registrationId)
    retrieveVatScheme(registrationId) flatMap {
      case Some(vatScheme) =>
        collection.update(selector, vatScheme.copy(vatChoice = vatChoice)) map {
          res => vatChoice
        } recover {
          case e: Exception =>
            // $COVERAGE-OFF$
            Logger.error(s"Unable to update VatChoice for registration ID $registrationId, Error: ${e.getMessage}")
            throw UpdateFailed(registrationId, "VatChoice")
          // $COVERAGE-ON$
        }

    }
  }
}
