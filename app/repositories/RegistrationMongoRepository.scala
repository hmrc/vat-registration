/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import cats.data.OptionT
import common.exceptions._
import common.{LogicalGroup, RegistrationId}
import enums.VatRegStatus
import models._
import models.api._
import play.api.Logger
import play.api.libs.json.{JsObject, Json, OFormat, Reads, Writes}
import play.modules.reactivemongo.{MongoDbConnection, ReactiveMongoComponent}
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._
import reactivemongo.json.BSONFormats
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NoStackTrace

class RegistrationMongo @Inject()(mongo: ReactiveMongoComponent) extends ReactiveMongoFormats {
  lazy val store = new RegistrationMongoRepository(mongo.mongoConnector.db)
}

trait RegistrationRepository {
  def createNewVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): Future[VatScheme]
  def retrieveVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): Future[Option[VatScheme]]
  def updateLogicalGroup[G](id: RegistrationId, group: G)(implicit w: Writes[G], logicalGroup: LogicalGroup[G], ec: ExecutionContext): Future[G]
  def deleteVatScheme(regId: String)(implicit hc: HeaderCarrier): Future[Boolean]
  def updateByElement(id: RegistrationId, elementPath: ElementPath, value: String)(implicit hc: HeaderCarrier): Future[String]
  def prepareRegistrationSubmission(id: RegistrationId, ackRef : String)(implicit hc: HeaderCarrier): Future[Boolean]
  def finishRegistrationSubmission(id : RegistrationId, status : VatRegStatus.Value)(implicit hc : HeaderCarrier) : Future[VatRegStatus.Value]
  def deleteByElement(id: RegistrationId, elementPath: ElementPath)(implicit ex: ExecutionContext): Future[Boolean]
  def updateIVStatus(regId: String, ivStatus: Boolean)(implicit ex: ExecutionContext): Future[Boolean]
  def saveTransId(transId: String, regId: RegistrationId)(implicit hc: HeaderCarrier): Future[String]
  def fetchRegByTxId(transId: String)(implicit hc: HeaderCarrier): Future[Option[VatScheme]]
  def retrieveTradingDetails(regId: String)(implicit hc: HeaderCarrier): Future[Option[TradingDetails]]
  def updateTradingDetails(regId: String, tradingDetails: TradingDetails)(implicit ex: ExecutionContext): Future[TradingDetails]
  def fetchReturns(regId: String)(implicit hc: HeaderCarrier): Future[Option[Returns]]
  def updateReturns(regId: String, returns: Returns)(implicit ex: ExecutionContext): Future[Returns]
  def fetchBankAccount(regId: String)(implicit ex: ExecutionContext): Future[Option[BankAccount]]
  def updateBankAccount(regId: String, bankAcount: BankAccount)(implicit ex: ExecutionContext): Future[BankAccount]
  def fetchTurnoverEstimates(regId: String)(implicit ec: ExecutionContext): Future[Option[TurnoverEstimates]]
  def updateTurnoverEstimates(regId: String, turnoverEstimate: TurnoverEstimates)(implicit ex: ExecutionContext): Future[TurnoverEstimates]
}


object RegistrationMongoFormats extends ReactiveMongoFormats {
  val encryptedFinancials: OFormat[VatFinancials] = VatFinancials.format(VatBankAccountMongoFormat.encryptedFormat)
  val vatSchemeFormat: OFormat[VatScheme] = OFormat(VatScheme.mongoReads, VatScheme.writes(encryptedFinancials))
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
  private[repositories] def regIdSelector(regId: String)                  = BSONDocument("registrationId" -> regId)

  override def indexes: Seq[Index] = Seq(
    Index(
      name    = Some("RegId"),
      key     = Seq("registrationId" -> IndexType.Ascending),
      unique  = true
    )
  )

  override def createNewVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): Future[VatScheme] = {
    val newReg = VatScheme(id, status = VatRegStatus.draft)
    collection.insert(newReg) map (_ => newReg) recover {
      case e =>
        logger.error(s"[createNewVatScheme] - Unable to insert new VAT Scheme for registration ID $id, Error: ${e.getMessage}")
        throw InsertFailed(id, "VatScheme")
    }
  }

  override def retrieveVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): Future[Option[VatScheme]] = {
    collection.find(ridSelector(id)).one[VatScheme]
  }

  override def retrieveTradingDetails(regId: String)(implicit hc: HeaderCarrier): Future[Option[TradingDetails]] = {
    val tradingDetailsProj = BSONDocument("tradingDetails" -> 1, "_id" -> 0)
    collection.find(regIdSelector(regId), tradingDetailsProj).one[TradingDetails]
  }

  override def updateLogicalGroup[G](id: RegistrationId, group: G)(implicit w: Writes[G], logicalGroup: LogicalGroup[G], ec: ExecutionContext): Future[G] =
    OptionT(collection.findAndUpdate(ridSelector(id), BSONDocument("$set" -> BSONDocument(logicalGroup.name -> w.writes(group))))
      .map(_.value)).map(_ => group).getOrElse {
      logger.error(s"[updateLogicalGroup] - There was a problem updating logical group ${logicalGroup.name} for regId ${id.value}")
      throw UpdateFailed(id, logicalGroup.name)
    }

  private def unsetElement(id: RegistrationId, element: String)(implicit ex: ExecutionContext): Future[Boolean] =
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

  override def deleteByElement(id: RegistrationId, elementPath: ElementPath)(implicit ex: ExecutionContext): Future[Boolean] =
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

  private def fetchBlock[T](regId: String, key: String)(implicit ec: ExecutionContext, rds: Reads[T]): Future[Option[T]] = {
    val projection = Json.obj(key -> 1)
    collection.find(regIdSelector(regId), projection).one[JsObject].map { doc =>
      doc.fold(throw new MissingRegDocument(RegistrationId(regId))) { js =>
        (js \ key).validateOpt[T].get
      }
    }
  }

  private[repositories] def updateBlock[T](regId: String, data: T, key: String = "")(implicit ec: ExecutionContext, writes: Writes[T]): Future[T] = {
    def toCamelCase(str: String): String = str.head.toLower + str.tail

    val selectorKey = if(key=="") toCamelCase(data.getClass.getSimpleName) else key

    val setDoc = Json.obj("$set" -> Json.obj(selectorKey -> Json.toJson(data)))
    collection.update(regIdSelector(regId), setDoc) map { updateResult =>
      if (updateResult.n == 0) {
        Logger.warn(s"[${data.getClass.getSimpleName}] updating for regId : $regId - No document found")
        throw MissingRegDocument(RegistrationId(regId))
      } else {
        Logger.info(s"[${data.getClass.getSimpleName}] updating for regId : $regId - documents modified : ${updateResult.nModified}")
        data
      }
    } recover {
      case e =>
        Logger.warn(s"Unable to update ${toCamelCase(data.getClass.getSimpleName)} for regId: $regId, Error: ${e.getMessage}")
        throw e
    }
  }

  override def updateIVStatus(regId: String, ivStatus: Boolean)(implicit ex: ExecutionContext): Future[Boolean] = {
    val querySelect = Json.obj("registrationId" -> regId, "lodgingOfficer" -> Json.obj("$exists" -> true))
    val setDoc = Json.obj("$set" -> Json.obj("lodgingOfficer.ivPassed" -> ivStatus))

    collection.update(querySelect, setDoc) map { updateResult =>
      if (updateResult.n == 0) {
        Logger.warn(s"[LodgingOfficer] updating ivPassed for regId : $regId - No document found or the document does not have lodgingOfficer defined")
        throw new MissingRegDocument(RegistrationId(regId))
      } else {
        Logger.info(s"[LodgingOfficer] updating ivPassed for regId : $regId - documents modified : ${updateResult.nModified}")
        ivStatus
      }
    } recover {
      case e =>
        Logger.warn(s"Unable to update ivPassed in LodgingOfficer for regId: $regId, Error: ${e.getMessage}")
        throw e
    }
  }

  override def updateTradingDetails(regId: String, tradingDetails: TradingDetails)(implicit ex: ExecutionContext): Future[TradingDetails] = {
    val selector = regIdSelector(regId)
    val update = BSONDocument("$set" -> Json.toJson(tradingDetails))
    collection.update(selector, update) map { updateResult =>
      Logger.info(s"[TradingDetails] updating trading details for regId : $regId - documents modified : ${updateResult.nModified}")
      tradingDetails
    }
  }

  override def fetchReturns(regId: String)(implicit hc: HeaderCarrier): Future[Option[Returns]] = {
    val selector = regIdSelector(regId)
    val projection = Json.obj("returns" -> 1)
    collection.find(selector, projection).one[JsObject].map { doc =>
      doc.flatMap { js =>
        (js \ "returns").validateOpt[Returns].get
      }
    }
  }

  override def updateReturns(regId: String, returns: Returns)(implicit ex: ExecutionContext): Future[Returns] = {
    val selector = regIdSelector(regId)
    val update = BSONDocument("$set" -> BSONDocument("returns" -> Json.toJson(returns)))
    collection.update(selector, update) map { updateResult =>
      Logger.info(s"[Returns] updating returns for regId : $regId - documents modified : ${updateResult.nModified}")
      returns
    }
  }

  override def fetchBankAccount(regId: String)(implicit ex: ExecutionContext): Future[Option[BankAccount]] = {
    val selector = regIdSelector(regId)
    val projection = Json.obj("bankAccount" -> 1)
    collection.find(selector, projection).one[JsObject].map(
      _.flatMap (js => (js \ "bankAccount").validateOpt(BankAccountMongoFormat.encryptedFormat).get)
    )
  }

  override def updateBankAccount(regId: String, bankAccount: BankAccount)(implicit ex: ExecutionContext): Future[BankAccount] = {
    val selector = regIdSelector(regId)
    val update = BSONDocument("$set" -> Json.obj("bankAccount" -> Json.toJson(bankAccount)(BankAccountMongoFormat.encryptedFormat)))
    collection.update(selector, update) map { updateResult =>
      Logger.info(s"[Returns] updating bank account for regId : $regId - documents modified : ${updateResult.nModified}")
      bankAccount
    }
  }

  override def fetchTurnoverEstimates(regId: String)(implicit ec: ExecutionContext): Future[Option[TurnoverEstimates]] = {
    val selector = regIdSelector(regId)
    collection.find(selector).one[JsObject].map(
      _.fold(throw MissingRegDocument(RegistrationId(regId)))(js => (js \ "turnoverEstimates").validateOpt[TurnoverEstimates].get)
    )
  }

  override def updateTurnoverEstimates(regId: String, turnoverEstimate: TurnoverEstimates)(implicit ex: ExecutionContext): Future[TurnoverEstimates] = {
    val selector = regIdSelector(regId)
    val update = BSONDocument("$set" -> BSONDocument("turnoverEstimates" -> Json.toJson(turnoverEstimate)))
    collection.update(selector, update) map { updateResult =>
      Logger.info(s"[TurnoverEstimate] updating turnover estimate for regId : $regId - documents modified : ${updateResult.nModified}")
      turnoverEstimate
    }
  }

  def getEligibility(regId: String)(implicit ec: ExecutionContext): Future[Option[Eligibility]] =
    fetchBlock[Eligibility](regId, "eligibility")

  def updateEligibility(regId: String, eligibility: Eligibility)(implicit ec: ExecutionContext): Future[Eligibility] = {
    updateBlock(regId, eligibility)
  }

  def getThreshold(regId: String)(implicit ec: ExecutionContext): Future[Option[Threshold]] =
    fetchBlock[Threshold](regId, "threshold")

  def updateThreshold(regId: String, threshold: Threshold)(implicit ec: ExecutionContext): Future[Threshold] = {
    //TODO - Need to maintain old model VatServiceEligibility to not break frontend service - TO BE REMOVE once frontend service use Eligibility model
    val necessity = if (threshold.mandatoryRegistration) "obligatory" else "voluntary"
    val overThreshold = threshold.overThresholdDate.fold(VatThresholdPostIncorp(false, None))(date => VatThresholdPostIncorp(true, Some(date)))
    val expectedOverThreshold = threshold.expectedOverThresholdDate.fold(VatExpectedThresholdPostIncorp(false, None))(date => VatExpectedThresholdPostIncorp(true, Some(date)))
    val eligibilityChoice = VatEligibilityChoice(necessity = necessity, vatThresholdPostIncorp = Some(overThreshold), vatExpectedThresholdPostIncorp = Some(expectedOverThreshold))
    updateLogicalGroup(RegistrationId(regId), VatServiceEligibility(vatEligibilityChoice = Some(eligibilityChoice)))

    updateBlock(regId, threshold)
  }

  def getLodgingOfficer(regId: String)(implicit ec: ExecutionContext): Future[Option[LodgingOfficer]] =
    fetchBlock[LodgingOfficer](regId, "lodgingOfficer")

  def updateLodgingOfficer(regId: String, lodgingOfficer: LodgingOfficer)(implicit ec: ExecutionContext): Future[LodgingOfficer] =
    updateBlock(regId, lodgingOfficer)
}
