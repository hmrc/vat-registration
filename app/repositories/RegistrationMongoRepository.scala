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

import auth.{AuthorisationResource, CryptoSCRS}
import common.exceptions._
import enums.VatRegStatus
import models.api._
import models.api.returns.Returns
import models.submission.PartyType
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.commands.WriteResult.Message
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID}
import reactivemongo.play.json.ImplicitBSONHandlers._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import utils.JsonErrorUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// scalastyle:off
@Singleton
class RegistrationMongoRepository @Inject()(mongo: ReactiveMongoComponent, crypto: CryptoSCRS)(implicit executionContext: ExecutionContext)
  extends ReactiveRepository[VatScheme, BSONObjectID](
    collectionName = "registration-information",
    mongo = mongo.mongoConnector.db,
    domainFormat = VatScheme.format(Some(crypto))
  ) with ReactiveMongoFormats with AuthorisationResource with JsonErrorUtil {

  startUp

  private val bankAccountCryptoFormatter = BankAccountMongoFormat.encryptedFormat(crypto)
  private val acknowledgementRefPrefix = "VRS"

  def startUp: Future[Unit] = collection.indexesManager.list() map { indexes =>
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

  // Generic methods

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
        logger.warn(s"[${data.getClass.getSimpleName}] updating for regId : $regId - No document found")
        throw MissingRegDocument(regId)
      } else {
        logger.info(s"[${data.getClass.getSimpleName}] updating for regId : $regId - documents modified : ${updateResult.nModified}")
        data
      }
    } recover {
      case e =>
        logger.warn(s"Unable to update ${toCamelCase(data.getClass.getSimpleName)} for regId: $regId, Error: ${e.getMessage}")
        throw e
    }
  }

  def createNewVatScheme(regId: String, intId: String): Future[VatScheme] = {
    val set = Json.obj(
      "registrationId" -> Json.toJson[String](regId),
      "status" -> Json.toJson(VatRegStatus.draft),
      "internalId" -> Json.toJson[String](intId)
    )
    collection.insert.one(set).map { _ =>
      VatScheme(regId, internalId = intId, status = VatRegStatus.draft)
    }.recover {
      case e: Exception =>
        logger.error(s"[RegistrationMongoRepository] [createNewVatScheme] threw an exception when attempting to create a new record with exception: ${e.getMessage} for regId: $regId and internalid: $intId")
        throw InsertFailed(regId, "VatScheme")
    }
  }

  def insertVatScheme(vatScheme: VatScheme): Future[VatScheme] = {
    implicit val vatSchemeWrites: OWrites[VatScheme] = VatScheme.format(Some(crypto))

    collection.update.one(regIdSelector(vatScheme.id), vatScheme, upsert = true).map { writeResult =>
      logger.info(s"[RegistrationMongoRepository] [insertVatScheme] successfully stored a preexisting VatScheme")
      vatScheme
    }.recover {
      case e: Exception =>
        logger.error(s"[RegistrationMongoRepository] [insertVatScheme] failed to store a VatScheme with regId: ${vatScheme.id}")
        throw e
    }
  }

  def retrieveVatScheme(regId: String): Future[Option[VatScheme]] = {
    implicit val format = VatScheme.format(Some(crypto))
    find("registrationId" -> regId).map(_.headOption)
  }

  def retrieveVatSchemeByInternalId(id: String): Future[Option[VatScheme]] = {
    implicit val format = VatScheme.format(Some(crypto))
    collection.find[JsObject, VatScheme](Json.obj("internalId" -> id), None).sort(Json.obj("_id" -> -1)).one[VatScheme]
  }

  def deleteVatScheme(regId: String): Future[Boolean] = {
    collection.delete.one(regIdSelector(regId)) map { wr =>
      if (!wr.ok) logger.error(s"[deleteVatScheme] - Error deleting vat reg doc for regId $regId - Error: ${Message.unapply(wr)}")
      wr.ok
    }
  }

  def lockSubmission(regId: String): Future[Boolean] = {
    val modifier = toBSON(Json.obj(
      "status" -> VatRegStatus.locked
    )).get

    collection.update.one(regIdSelector(regId), BSONDocument("$set" -> modifier)).map(_.ok)
  }

  def finishRegistrationSubmission(regId: String, status: VatRegStatus.Value, formBundleId: String): Future[VatRegStatus.Value] = {
    val modifier = toBSON(Json.obj(
      "status" -> status,
      "acknowledgementReference" -> s"$acknowledgementRefPrefix$formBundleId"
    )).get

    collection.update.one(regIdSelector(regId), BSONDocument("$set" -> modifier)).map(_ => status)
  }

  def fetchReturns(regId: String): Future[Option[Returns]] = {
    val selector = regIdSelector(regId)
    val projection = Some(Json.obj("returns" -> 1))
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
    collection.update.one(selector, update) map { updateResult =>
      logger.info(s"[Returns] updating returns for regId : $regId - documents modified : ${updateResult.nModified}")
      returns
    }
  }

  def fetchBankAccount(regId: String): Future[Option[BankAccount]] = {
    val selector = regIdSelector(regId)
    val projection = Some(Json.obj("bankAccount" -> 1))
    collection.find(selector, projection).one[JsObject].map(
      _.flatMap(js => (js \ "bankAccount").validateOpt(bankAccountCryptoFormatter).get)
    )
  }

  def updateBankAccount(regId: String, bankAccount: BankAccount): Future[BankAccount] = {
    val selector = regIdSelector(regId)
    val update = BSONDocument("$set" -> Json.obj("bankAccount" -> Json.toJson(bankAccount)(bankAccountCryptoFormatter)))
    collection.update.one(selector, update) map { updateResult =>
      logger.info(s"[Returns] updating bank account for regId : $regId - documents modified : ${updateResult.nModified}")
      bankAccount
    }
  }

  def getInternalId(id: String)(implicit hc: HeaderCarrier): Future[Option[String]] = {
    val projection = Some(Json.obj("internalId" -> 1, "_id" -> 0))
    collection.find(regIdSelector(id), projection).one[JsObject].map {
      _.map(js => (js \ "internalId").as[String])
    }
  }

  def getApplicantDetails(regId: String, partyType: PartyType): Future[Option[ApplicantDetails]] = {
    val projection = Json.obj("applicantDetails" -> 1, "_id" -> 0)

    collection.find(regIdSelector(regId), Some(projection)).one[JsObject].map { doc =>
      doc.fold[Option[ApplicantDetails]](throw MissingRegDocument(regId))(json =>
        (json \ "applicantDetails").validateOpt[ApplicantDetails](ApplicantDetails.reads(partyType)) match {
          case JsSuccess(applicantDetails, _) =>
            applicantDetails
          case JsError(errors) =>
            logger.warn(s"[getApplicantDetails] Failed to parse applicant details for regId: $regId, Error: ${errors.mkString(" ")}")
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
        logger.warn(s"[patchApplicantDetails] regId: $regId - No document found or the document does not have applicantDetails defined")
        throw MissingRegDocument(regId)
      } else {
        logger.info(s"[patchApplicantDetails] regId: $regId - documents modified : ${updateResult.nModified}")
        applicantDetails
      }
    } recover {
      case error =>
        logger.warn(s"[ApplicantDetails] [patchApplicantDetails] regId: $regId, Error: ${error.getMessage}")
        throw error
    }
  }

  private[repositories] def regIdSelector(regId: String) = BSONDocument("registrationId" -> regId)

  def removeFlatRateScheme(regId: String): Future[Boolean] = {
    val selector = regIdSelector(regId)
    val update = BSONDocument("$unset" -> BSONDocument("flatRateScheme" -> ""))
    collection.update.one(selector, update) map { updateResult =>
      if (updateResult.n == 0) {
        logger.warn(s"[RegistrationMongoRepository][removeFlatRateScheme] removing for regId : $regId - No document found")
        throw MissingRegDocument(regId)
      } else {
        logger.info(s"[RegistrationMongoRepository][removeFlatRateScheme] removing for regId : $regId - documents modified : ${updateResult.nModified}")
        true
      }
    } recover {
      case e =>
        logger.warn(s"[RegistrationMongoRepository][removeFlatRateScheme] Unable to remove for regId: $regId, Error: ${e.getMessage}")
        throw e
    }
  }

  def fetchNrsSubmissionPayload(regId: String): Future[Option[String]] =
    fetchBlock[String](regId, "nrsSubmissionPayload")

  def updateNrsSubmissionPayload(regId: String, encodedHTML: String): Future[String] =
    updateBlock[String](regId, encodedHTML, "nrsSubmissionPayload")

  def fetchSicAndCompliance(regId: String): Future[Option[SicAndCompliance]] =
    fetchBlock[SicAndCompliance](regId, "sicAndCompliance")(SicAndCompliance.apiFormat)

  def updateSicAndCompliance(regId: String, sicAndCompliance: SicAndCompliance): Future[SicAndCompliance] =
    updateBlock(regId, sicAndCompliance, "sicAndCompliance")(SicAndCompliance.apiFormat)

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

  def storeHonestyDeclaration(regId: String, honestyDeclarationData: Boolean): Future[Boolean] =
    updateBlock(regId, honestyDeclarationData, "confirmInformationDeclaration")

}