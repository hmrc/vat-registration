/*
 * Copyright 2020 HM Revenue & Customs
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
import cats.data.OptionT
import cats.instances.future._
import common.exceptions._
import enums.VatRegStatus
import javax.inject.{Inject, Singleton}
import models._
import models.api.{Threshold, _}
import play.api.Logger
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult.Message
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import utils.{EligibilityDataJsonUtils, JsonErrorUtil}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

// scalastyle:off
@Singleton
class RegistrationMongoRepository @Inject()(mongo: ReactiveMongoComponent, crypto: CryptoSCRS)
  extends ReactiveRepository[VatScheme, BSONObjectID](
    collectionName = "registration-information",
    mongo = mongo.mongoConnector.db,
    domainFormat = VatScheme.mongoFormat(crypto)
  ) with ReactiveMongoFormats with AuthorisationResource with JsonErrorUtil {

  startUp

  private val bankAccountCryptoFormatter = BankAccountMongoFormat.encryptedFormat(crypto)

  def startUp = collection.indexesManager.list() map { indexes =>
    logger.info("[Startup] Outputting current indexes")
    indexes foreach { index =>
      val name = index.name.getOrElse("<no-name>")
      val keys = (index.key map { case (k, a) => s"$k -> ${a.value}" }) mkString (",")
      logger.info(s"[Index] name: $name keys: $keys unique: ${index.unique} sparse: ${index.sparse}")
    }
    logger.info("[Startup] Finished outputting current indexes")
  }

  override def indexes: Seq[Index] = Seq(
    Index(
      name = Some("RegId"),
      key = Seq("registrationId" -> IndexType.Ascending),
      unique = true
    ),
    Index(
      name = Some("RegIdAndInternalId"),
      key = Seq(
        "registrationId" -> IndexType.Ascending,
        "internalId" -> IndexType.Ascending
      ),
      unique = true
    )
  )

  //TODO: should be written with $set to not use vatscheme writes
  def createNewVatScheme(regId: String, intId: String)(implicit hc: HeaderCarrier): Future[VatScheme] = {
    val set = Json.obj(
      "registrationId" -> Json.toJson[String](regId),
      "status" -> Json.toJson(VatRegStatus.draft),
      "internalId" -> Json.toJson[String](intId)
    )
    collection.insert(set).map { _ =>
      VatScheme(regId, internalId = intId, status = VatRegStatus.draft)
    }.recover {
      case e: Exception =>
        Logger.error(s"[RegistrationMongoRepository] [createNewVatScheme] threw an exception when attempting to create a new record with exception: ${e.getMessage} for regId: $regId and internalid: $intId")
        throw new InsertFailed(regId, "VatScheme")
    }
  }

  def retrieveVatScheme(regId: String)(implicit hc: HeaderCarrier): Future[Option[VatScheme]] = {
    collection.find(regIdSelector(regId)).one[VatScheme]
  }

  def retrieveVatSchemeByInternalId(id: String)(implicit hc: HeaderCarrier): Future[Option[VatScheme]] = {
    collection.find(Json.obj("internalId" -> id)).sort(Json.obj("_id" -> -1)).one[VatScheme]
  }

  def deleteVatScheme(regId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    collection.delete.one(regIdSelector(regId)) map { wr =>
      wr
      if (!wr.ok) logger.error(s"[deleteVatScheme] - Error deleting vat reg doc for regId $regId - Error: ${Message.unapply(wr)}")
      wr.ok
    }
  }

  def updateByElement(regId: String, elementPath: ElementPath, value: String)(implicit hc: HeaderCarrier): Future[String] =
    setElement(regId, elementPath.path, value)

  private def setElement(regId: String, element: String, value: String)(implicit hc: HeaderCarrier): Future[String] =
    OptionT(collection.findAndUpdate(regIdSelector(regId), BSONDocument("$set" -> BSONDocument(element -> value)))
      .map(_.value)).map(_ => value).getOrElse {
      logger.error(s"[setElement] - There was a problem setting element $element for regId ${regId}")
      throw UpdateFailed(regId, element)
    }

  def prepareRegistrationSubmission(regId: String, ackRef: String, status: VatRegStatus.Value)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val modifier = toBSON(Json.obj(
      AcknowledgementReferencePath.path -> ackRef,
      VatStatusPath.path -> (if (status == VatRegStatus.draft) VatRegStatus.locked else status)
    )).get

    collection.update(regIdSelector(regId), BSONDocument("$set" -> modifier)).map(_.ok)
  }

  def finishRegistrationSubmission(regId: String, status: VatRegStatus.Value)(implicit hc: HeaderCarrier): Future[VatRegStatus.Value] = {
    val modifier = toBSON(Json.obj(
      VatStatusPath.path -> status
    )).get

    collection.update(regIdSelector(regId), BSONDocument("$set" -> modifier)).map(_ => status)
  }

  def saveTransId(transId: String, regId: String)(implicit hc: HeaderCarrier): Future[String] = {
    val modifier = toBSON(Json.obj(
      VatTransIdPath.path -> transId
    )).get

    collection.update(regIdSelector(regId), BSONDocument("$set" -> modifier)).map(_ => transId)
  }

  def fetchRegByTxId(transId: String)(implicit hc: HeaderCarrier): Future[Option[VatScheme]] = {
    collection.find(tidSelector(transId)).one[VatScheme]
  }

  private[repositories] def tidSelector(id: String) = BSONDocument("transactionId" -> id)

  def updateIVStatus(regId: String, ivStatus: Boolean): Future[Boolean] = {
    val querySelect = Json.obj("registrationId" -> regId, "applicantDetails" -> Json.obj("$exists" -> true))
    val setDoc = Json.obj("$set" -> Json.obj("applicantDetails.ivPassed" -> ivStatus))

    collection.update(querySelect, setDoc) map { updateResult =>
      if (updateResult.n == 0) {
        Logger.warn(s"[ApplicantDetails] updating ivPassed for regId : $regId - No document found or the document does not have applicantDetails defined")
        throw new MissingRegDocument(regId)
      } else {
        Logger.info(s"[ApplicantDetails] updating ivPassed for regId : $regId - documents modified : ${updateResult.nModified}")
        ivStatus
      }
    } recover {
      case e =>
        Logger.warn(s"Unable to update ivPassed in ApplicantDetails for regId: $regId, Error: ${e.getMessage}")
        throw e
    }
  }

  def fetchReturns(regId: String)(implicit hc: HeaderCarrier): Future[Option[Returns]] = {
    val selector = regIdSelector(regId)
    val projection = Json.obj("returns" -> 1)
    collection.find(selector, projection).one[JsObject].map { doc =>
      doc.flatMap { js =>
        (js \ "returns").validateOpt[Returns].get
      }
    }
  }

  def retrieveTradingDetails(regId: String): Future[Option[TradingDetails]] = {
    fetchBlock[TradingDetails](regId, "tradingDetails")
  }

  def updateTradingDetails(regId: String, tradingDetails: TradingDetails): Future[TradingDetails] = {
    updateBlock(regId, tradingDetails, "tradingDetails")
  }

  def updateReturns(regId: String, returns: Returns): Future[Returns] = {
    val selector = regIdSelector(regId)
    val update = BSONDocument("$set" -> BSONDocument("returns" -> Json.toJson(returns)))
    collection.update(selector, update) map { updateResult =>
      Logger.info(s"[Returns] updating returns for regId : $regId - documents modified : ${updateResult.nModified}")
      returns
    }
  }

  def fetchBankAccount(regId: String): Future[Option[BankAccount]] = {
    val selector = regIdSelector(regId)
    val projection = Json.obj("bankAccount" -> 1)
    collection.find(selector, projection).one[JsObject].map(
      _.flatMap(js => (js \ "bankAccount").validateOpt(bankAccountCryptoFormatter).get)
    )
  }

  def updateBankAccount(regId: String, bankAccount: BankAccount): Future[BankAccount] = {
    val selector = regIdSelector(regId)
    val update = BSONDocument("$set" -> Json.obj("bankAccount" -> Json.toJson(bankAccount)(bankAccountCryptoFormatter)))
    collection.update(selector, update) map { updateResult =>
      Logger.info(s"[Returns] updating bank account for regId : $regId - documents modified : ${updateResult.nModified}")
      bankAccount
    }
  }

  def getInternalId(id: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val projection = Json.obj("internalId" -> 1, "_id" -> 0)
    collection.find(regIdSelector(id), projection).one[JsObject].map {
      _.map(js => (js \ "internalId").as[String])
    }
  }

  def getApplicantDetails(regId: String): Future[Option[ApplicantDetails]] = {
    val projection = Json.obj("applicantDetails" -> 1, "_id" -> 0)

    collection.find(regIdSelector(regId), Some(projection)).one[JsObject].map { doc =>
      doc.fold[Option[ApplicantDetails]](throw new MissingRegDocument(regId)) (json =>
        (json \ "applicantDetails").validateOpt[ApplicantDetails] match {
          case JsSuccess(applicantDetails, _) =>
            applicantDetails
          case JsError(errors) =>
            Logger.warn(s"[getApplicantDetails] Failed to parse applicant details for regId: $regId, Error: ${errors.mkString(" ")}")
            None
        }
      )
    }
  }

  def patchApplicantDetails(regId: String, applicantDetails: ApplicantDetails): Future[ApplicantDetails] = {
    val query = Json.obj("registrationId" -> regId)
    val updateData = Json.obj("$set" -> Json.obj("applicantDetails" -> applicantDetails))

    collection.update.one(query, updateData, upsert = true) map { updateResult =>
      if (updateResult.n == 0) {
        Logger.warn(s"[patchApplicantDetails] regId: $regId - No document found or the document does not have applicantDetails defined")
        throw new MissingRegDocument(regId)
      } else {
        Logger.info(s"[patchApplicantDetails] regId: $regId - documents modified : ${updateResult.nModified}")
        applicantDetails
      }
    } recover {
      case error =>
        Logger.warn(s"[ApplicantDetails] [patchApplicantDetails] regId: $regId, Error: ${error.getMessage}")
        throw error
    }
  }

  private[repositories] def regIdSelector(regId: String) = BSONDocument("registrationId" -> regId)

  def removeFlatRateScheme(regId: String): Future[Boolean] = {
    val selector = regIdSelector(regId)
    val update = BSONDocument("$unset" -> BSONDocument("flatRateScheme" -> ""))
    collection.update(selector, update) map { updateResult =>
      if (updateResult.n == 0) {
        Logger.warn(s"[RegistrationMongoRepository][removeFlatRateScheme] removing for regId : $regId - No document found")
        throw MissingRegDocument(regId)
      } else {
        Logger.info(s"[RegistrationMongoRepository][removeFlatRateScheme] removing for regId : $regId - documents modified : ${updateResult.nModified}")
        true
      }
    } recover {
      case e =>
        Logger.warn(s"[RegistrationMongoRepository][removeFlatRateScheme] Unable to remove for regId: $regId, Error: ${e.getMessage}")
        throw e
    }
  }

  def clearDownDocument(transId: String): Future[Boolean] = {
    collection.remove(tidSelector(transId)) map { wr =>
      if (!wr.ok) logger.error(s"[clearDownDocument] - Error deleting vat reg doc for txId $transId - Error: ${Message.unapply(wr)}")
      wr.ok
    }
  }

  def fetchBlock[T](regId: String, key: String)(implicit rds: Reads[T]): Future[Option[T]] = {
    val projection = Some(Json.obj(key -> 1))
    collection.find(regIdSelector(regId), projection).one[JsObject].map { doc =>
      doc.fold(throw MissingRegDocument(regId)) { js =>
        (js \ key).validateOpt[T].get
      }
    }
  }

  def updateBlock[T](regId: String, data: T, key: String = "")(implicit writes: Writes[T]): Future[T] = {
    def toCamelCase(str: String): String = str.head.toLower + str.tail

    val selectorKey = if (key == "") toCamelCase(data.getClass.getSimpleName) else key

    val setDoc = Json.obj("$set" -> Json.obj(selectorKey -> Json.toJson(data)))
    collection.update.one(regIdSelector(regId), setDoc) map { updateResult =>
      if (updateResult.n == 0) {
        Logger.warn(s"[${data.getClass.getSimpleName}] updating for regId : $regId - No document found")
        throw MissingRegDocument(regId)
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

  def fetchSicAndCompliance(regId: String): Future[Option[SicAndCompliance]] =
    fetchBlock[SicAndCompliance](regId, "sicAndCompliance")(SicAndCompliance.mongoFormats)

  def updateSicAndCompliance(regId: String, sicAndCompliance: SicAndCompliance): Future[SicAndCompliance] =
    updateBlock(regId, sicAndCompliance, "sicAndCompliance")(SicAndCompliance.mongoFormats)

  def fetchBusinessContact(regId: String): Future[Option[BusinessContact]] =
    fetchBlock[BusinessContact](regId, "businessContact")

  def updateBusinessContact(regId: String, businessCont: BusinessContact): Future[BusinessContact] =
    updateBlock(regId, businessCont, "businessContact")

  def fetchFlatRateScheme(regId: String): Future[Option[FlatRateScheme]] =
    fetchBlock[FlatRateScheme](regId, "flatRateScheme")

  def updateFlatRateScheme(regId: String, flatRateScheme: FlatRateScheme): Future[FlatRateScheme] =
    updateBlock(regId, flatRateScheme, "flatRateScheme")

  def fetchEligibilityData(regId: String): Future[Option[JsObject]] =
    fetchBlock[JsObject](regId, "eligibilityData")

  def updateEligibilityData(regId: String, eligibilityData: JsObject): Future[JsObject] =
    updateBlock(regId, eligibilityData, "eligibilityData")

  def fetchEligibilitySubmissionData(regId: String): Future[Option[EligibilitySubmissionData]] =
    fetchBlock[EligibilitySubmissionData](regId, "eligibilitySubmissionData")

  def updateEligibilitySubmissionData(regId: String, eligibilitySubmissionData: EligibilitySubmissionData): Future[EligibilitySubmissionData] =
    updateBlock(regId, eligibilitySubmissionData, "eligibilitySubmissionData")

}