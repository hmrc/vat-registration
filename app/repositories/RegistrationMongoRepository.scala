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

import java.time.LocalDate
import javax.inject.{Inject, Named}

import cats.data.OptionT
import common.exceptions._
import common.{LogicalGroup, RegistrationId}
import enums.VatRegStatus
import models._
import models.api._
import play.api.Logger
import play.api.libs.json.{JsObject, Json, OFormat, Writes}
import play.modules.reactivemongo.{MongoDbConnection, ReactiveMongoComponent}
import reactivemongo.api.DB
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._
import reactivemongo.json.BSONFormats
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class RegistrationMongo @Inject()(mongo: ReactiveMongoComponent) extends ReactiveMongoFormats {
  lazy val store = new RegistrationMongoRepository(mongo.mongoConnector.db)
}

trait RegistrationRepository {
  def createNewVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): Future[VatScheme]
  def retrieveVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): Future[Option[VatScheme]]
  def updateLogicalGroup[G](id: RegistrationId, group: G)(implicit w: Writes[G], logicalGroup: LogicalGroup[G], hc: HeaderCarrier): Future[G]
  def deleteVatScheme(regId: String)(implicit hc: HeaderCarrier): Future[Boolean]
  def updateByElement(id: RegistrationId, elementPath: ElementPath, value: String)(implicit hc: HeaderCarrier): Future[String]
  def prepareRegistrationSubmission(id: RegistrationId, ackRef : String)(implicit hc: HeaderCarrier): Future[Boolean]
  def finishRegistrationSubmission(id : RegistrationId, status : VatRegStatus.Value)(implicit hc : HeaderCarrier) : Future[VatRegStatus.Value]
  def deleteByElement(id: RegistrationId, elementPath: ElementPath)(implicit hc: HeaderCarrier): Future[Boolean]
  def updateIVStatus(regId: String, ivStatus: Boolean)(implicit hc: HeaderCarrier): Future[Boolean]
  def saveTransId(transId: String, regId: RegistrationId)(implicit hc: HeaderCarrier): Future[String]
  def fetchRegByTxId(transId: String)(implicit hc: HeaderCarrier): Future[Option[VatScheme]]
  def updateEligibility(regId: String, eligibility: Eligibility)(implicit hc: HeaderCarrier): Future[Eligibility]
  def updateThreshold(regId: String, threshold: Threshold)(implicit hc: HeaderCarrier): Future[Threshold]
}


object RegistrationMongoFormats extends ReactiveMongoFormats {
  val encryptedFinancials: OFormat[VatFinancials] = VatFinancials.format(VatBankAccountMongoFormat.encryptedFormat)
  val vatSchemeFormat: OFormat[VatScheme] = OFormat(VatScheme.reads(encryptedFinancials), VatScheme.writes(encryptedFinancials))
}

class RegistrationMongoRepository (mongo: () => DB)
  extends ReactiveRepository[VatScheme, BSONObjectID](
    collectionName = "registration-information",
    mongo = mongo,
    domainFormat = RegistrationMongoFormats.vatSchemeFormat
  ) with RegistrationRepository {

  import cats.instances.future._

  private[repositories] def ridSelector(id: RegistrationId) = BSONDocument("registrationId" -> BSONString(id.value))
  private[repositories] def tidSelector(id: String) = BSONDocument("transactionId" -> id)
  private def regIdSelector(regId: String)                  = BSONDocument("registrationId" -> regId)

  override def indexes: Seq[Index] = Seq(
    Index(
      name    = Some("RegId"),
      key     = Seq("registrationId" -> IndexType.Ascending),
      unique  = true
    )
  )

  override def createNewVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): Future[VatScheme] = {
    val newReg = VatScheme(id, None, None, None, None, status = VatRegStatus.draft)
    collection.insert(newReg) map (_ => newReg) recover {
      case e =>
        logger.error(s"[createNewVatScheme] - Unable to insert new VAT Scheme for registration ID $id, Error: ${e.getMessage}")
        throw InsertFailed(id, "VatScheme")
    }
  }

  override def retrieveVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): Future[Option[VatScheme]] = {
    collection.find(ridSelector(id)).one[VatScheme]
  }

  override def updateLogicalGroup[G](id: RegistrationId, group: G)(implicit w: Writes[G], logicalGroup: LogicalGroup[G], hc: HeaderCarrier): Future[G] =
    OptionT(collection.findAndUpdate(ridSelector(id), BSONDocument("$set" -> BSONDocument(logicalGroup.name -> w.writes(group))))
      .map(_.value)).map(_ => group).getOrElse {
      logger.error(s"[updateLogicalGroup] - There was a problem updating logical group ${logicalGroup.name} for regId ${id.value}")
      throw UpdateFailed(id, logicalGroup.name)
    }

  private def unsetElement(id: RegistrationId, element: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    OptionT(collection.findAndUpdate(ridSelector(id), BSONDocument("$unset" -> BSONDocument(element -> "")))
      .map(_.value)).map(_ => true).getOrElse {
      logger.error(s"[unsetElement] - There was a problem unsetting element $element for regId ${id.value}")
      throw UpdateFailed(id, element)
    }

  private def setElement(id: RegistrationId, element: String, value: String)(implicit hc: HeaderCarrier): Future[String] =
    OptionT(collection.findAndUpdate(ridSelector(id), BSONDocument("$set" -> BSONDocument(element -> value)))
      .map(_.value)).map(_ => value).getOrElse {
      logger.error(s"[setElement] - There was a problem setting element $element for regId ${id.value}")
      throw UpdateFailed(id, element)
    }

  override def deleteVatScheme(regId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    collection.remove(regIdSelector(regId)) map { wr =>
      if(!wr.ok) logger.error(s"[deleteVatScheme] - Error deleting vat reg doc for regId $regId - Error: ${wr.message}")
      wr.ok
    }
  }

  override def deleteByElement(id: RegistrationId, elementPath: ElementPath)(implicit hc: HeaderCarrier): Future[Boolean] =
    unsetElement(id, elementPath.path)

  override def updateByElement(id: RegistrationId, elementPath: ElementPath, value: String)(implicit hc: HeaderCarrier): Future[String] =
    setElement(id, elementPath.path, value)

  override def prepareRegistrationSubmission(id : RegistrationId, ackRef : String)(implicit hc: HeaderCarrier) : Future[Boolean] = {
    val modifier = BSONFormats.toBSON(Json.obj(
      AcknowledgementReferencePath.path -> ackRef
    )).get

    collection.update(ridSelector(id), BSONDocument("$set" -> modifier)).map(_.ok)
  }

  override def finishRegistrationSubmission(id : RegistrationId, status: VatRegStatus.Value)(implicit hc: HeaderCarrier) : Future[VatRegStatus.Value] = {
    val modifier = BSONFormats.toBSON(Json.obj(
      VatStatusPath.path -> status
    )).get

    collection.update(ridSelector(id), BSONDocument("$set" -> modifier)).map(_ => status)
  }

  override def saveTransId(transId: String, regId: RegistrationId)(implicit hc: HeaderCarrier) : Future[String] = {
    val modifier = BSONFormats.toBSON(Json.obj(
      VatTransIdPath.path -> transId
    )).get

    collection.update(ridSelector(regId), BSONDocument("$set" -> modifier)).map(_ => transId)
  }

  override def fetchRegByTxId(transId: String)(implicit hc: HeaderCarrier): Future[Option[VatScheme]] = {
    collection.find(tidSelector(transId)).one[VatScheme]
  }

  override def updateIVStatus(regId: String, ivStatus: Boolean)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val updateDocument = BSONDocument("$set" -> BSONDocument("lodgingOfficer.ivPassed" -> BSONBoolean(ivStatus)))
    collection.find(regIdSelector(regId)).one[VatScheme] flatMap {
      case Some(_) => collection.update(regIdSelector(regId), updateDocument) map { wr =>
        if(wr.ok) {
          ivStatus
        } else {
          logger.error(s"[updateIVStatus] - There was a problem setting the IV status for regId $regId")
          throw UpdateFailed(RegistrationId(regId), "lodgingOfficer.ivPassed")
        }
      }
      case None =>
        logger.error(s"[updateIVStatus] - No VAT registration could be found for regId ${regId}")
        throw new MissingRegDocument(RegistrationId(regId))
    }
  }


  def updateTradingDetails(regId: String, tradingDetails: TradingDetails)(implicit ex: ExecutionContext): Future[TradingDetails] = {
    val selector = regIdSelector(regId)
    val update = BSONDocument("$set" -> BSONDocument("tradingDetails" -> Json.toJson(tradingDetails)))
    collection.update(selector, update) map { updateResult =>
      Logger.info(s"[TradingDetails] updating trading details for regId : $regId - documents modified : ${updateResult.nModified}")
      tradingDetails
    }
  }

  def getEligibility(regId: String)(implicit hc: HeaderCarrier): Future[Option[Eligibility]] = {
    val projection = BSONDocument("eligibility" -> 1)
    collection.find(regIdSelector(regId), projection).one[JsObject].map { doc =>
      doc.flatMap { js =>
        (js \ "eligibility").validateOpt[Eligibility].get
      }
    }
  }

  def updateEligibility(regId: String, eligibility: Eligibility)(implicit hc: HeaderCarrier): Future[Eligibility] = {
    val setDoc = BSONDocument("$set" -> BSONDocument("eligibility" -> BSONFormats.toBSON(Json.toJson(eligibility)).get))
    collection.update(regIdSelector(regId), setDoc) map { updateResult =>
      if (updateResult.n == 0) {
        Logger.warn(s"[Eligibility] updating eligibility for regId : $regId - No document found")
        throw MissingRegDocument(RegistrationId(regId))
      } else {
        Logger.info(s"[Eligibility] updating eligibility for regId : $regId - documents modified : ${updateResult.nModified}")
        eligibility
      }
    } recover {
      case e =>
        Logger.warn(s"Unable to update eligibility for regId: $regId, Error: ${e.getMessage}")
        throw e
    }
  }

  def getThreshold(regId: String)(implicit hc: HeaderCarrier): Future[Option[Threshold]] = {
    val projection = BSONDocument("threshold" -> 1)
    collection.find(regIdSelector(regId), projection).one[JsObject].map { doc =>
      doc.flatMap { js =>
        (js \ "threshold").validateOpt[Threshold].get
      }
    }
  }

  def updateThreshold(regId: String, threshold: Threshold)(implicit hc: HeaderCarrier): Future[Threshold] = {
    val setDoc = BSONDocument("$set" -> BSONDocument("threshold" -> BSONFormats.toBSON(Json.toJson(threshold)).get))
    collection.update(regIdSelector(regId), setDoc) map { updateResult =>
      if (updateResult.n == 0) {
        Logger.warn(s"[Threshold] updating threshold for regId : $regId - No document found")
        throw MissingRegDocument(RegistrationId(regId))
      } else {
        Logger.info(s"[Threshold] updating threshold for regId : $regId - documents modified : ${updateResult.nModified}")
        threshold
      }
    } recover {
      case e =>
        Logger.warn(s"Unable to update threshold for regId: $regId, Error: ${e.getMessage}")
        throw e
    }
  }
}
