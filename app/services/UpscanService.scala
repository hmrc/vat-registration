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

package services

import models.api.{InProgress, UpscanCreate, UpscanDetails}
import repositories.UpscanMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class UpscanService @Inject()(upscanMongoRepository: UpscanMongoRepository) {

  def getUpscanDetails(reference: String): Future[Option[UpscanDetails]] =
    upscanMongoRepository.getUpscanDetails(reference)

  def getAllUpscanDetails(registrationId: String): Future[Seq[UpscanDetails]] =
    upscanMongoRepository.getAllUpscanDetails(registrationId)

  def createUpscanDetails(registrationId: String, details: UpscanCreate): Future[UpscanDetails] = {
    val newUpscanDetails = UpscanDetails(
      registrationId = Some(registrationId),
      reference = details.reference,
      attachmentType = Some(details.attachmentType),
      fileStatus = InProgress
    )

    upscanMongoRepository.upsertUpscanDetails(newUpscanDetails)
  }

  def upsertUpscanDetails(upscanDetails: UpscanDetails): Future[UpscanDetails] =
    upscanMongoRepository.upsertUpscanDetails(upscanDetails)

  def deleteUpscanDetails(reference: String): Future[Boolean] =
    upscanMongoRepository.deleteUpscanDetails(reference)

  def deleteAllUpscanDetails(registrationId: String): Future[Boolean] =
    upscanMongoRepository.deleteAllUpscanDetails(registrationId)
}
