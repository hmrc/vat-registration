/*
 * Copyright 2023 HM Revenue & Customs
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

package services

import models.api.{InProgress, UpscanCreate, UpscanDetails}
import play.api.mvc.Request
import repositories.UpscanMongoRepository
import utils.LoggingUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class UpscanService @Inject() (upscanMongoRepository: UpscanMongoRepository) extends LoggingUtils {

  def getUpscanDetails(reference: String)(implicit request: Request[_]): Future[Option[UpscanDetails]] = {
    infoLog(s"[UpscanService][getUpscanDetails] attempting to get upscan details from mongo for reference: $reference")
    upscanMongoRepository.getUpscanDetails(reference)
  }

  def getAllUpscanDetails(registrationId: String)(implicit request: Request[_]): Future[Seq[UpscanDetails]] = {
    infoLog(
      s"[UpscanService][getAllUpscanDetails] attempting to get all upscan details from mongo for regId: $registrationId"
    )
    upscanMongoRepository.getAllUpscanDetails(registrationId)
  }

  def createUpscanDetails(registrationId: String, details: UpscanCreate)(implicit
    request: Request[_]
  ): Future[UpscanDetails] = {
    val newUpscanDetails = UpscanDetails(
      registrationId = Some(registrationId),
      reference = details.reference,
      attachmentType = Some(details.attachmentType),
      fileStatus = InProgress
    )

    infoLog(
      s"[UpscanService][createUpscanDetails] attempting to create upscan details record in mongo:" +
        s"\n regId: $registrationId" +
        s"\n reference: ${details.reference}" +
        s"\n attachmentType: ${details.attachmentType}" +
        s"\n fileStatus: ${newUpscanDetails.fileStatus}"
    )

    upscanMongoRepository.upsertUpscanDetails(newUpscanDetails)
  }

  def upsertUpscanDetails(upscanDetails: UpscanDetails)(implicit request: Request[_]): Future[UpscanDetails] = {
    infoLog(
      s"[UpscanService][upsertUpscanDetails] attempting to create upscan details record in mongo:" +
        s"\n regId: ${upscanDetails.registrationId}" +
        s"\n reference: ${upscanDetails.reference}" +
        s"\n attachmentType: ${upscanDetails.attachmentType}" +
        s"\n fileStatus: ${upscanDetails.fileStatus}"
    )

    upscanMongoRepository.upsertUpscanDetails(upscanDetails)
  }

  def deleteUpscanDetails(reference: String)(implicit request: Request[_]): Future[Boolean] = {
    infoLog(s"[UpscanService][deleteUpscanDetails] attempting to delete upscan details with reference $reference")
    upscanMongoRepository.deleteUpscanDetails(reference)
  }

  def deleteAllUpscanDetails(registrationId: String)(implicit request: Request[_]): Future[Boolean] = {
    infoLog(
      s"[UpscanService][deleteAllUpscanDetails] attempting to delete all upscan details for regId $registrationId"
    )
    upscanMongoRepository.deleteAllUpscanDetails(registrationId)
  }
}
