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

import featureswitch.core.config.FeatureSwitching
import models.api.VatScheme
import models.registration.{CollectionSectionId, RegistrationSectionId}
import play.api.libs.json._
import play.api.mvc.Request
import repositories.VatSchemeRepository
import services.RegistrationService.{indexOffset, recordsToReplace}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationService @Inject()(val vatSchemeRepository: VatSchemeRepository,
                                    registrationIdService: RegistrationIdService)
                                   (implicit ec: ExecutionContext) extends FeatureSwitching {

  def newRegistration(internalId: String): Future[VatScheme] = {
    val regId = registrationIdService.generateRegistrationId()
    vatSchemeRepository.createNewVatScheme(regId, internalId)
  }

  def getAllRegistrations[T](internalId: String)(implicit reads: Reads[T]): Future[List[T]] =
    vatSchemeRepository.getAllRegistrations(internalId).map {
      case Nil => Nil
      case registrations: List[JsValue] => registrations.flatMap(_.validate[T].asOpt)
    }

  def getRegistration(internalId: String, regId: String): Future[Option[VatScheme]] =
    vatSchemeRepository.getRegistration(internalId, regId)

  def upsertRegistration(internalId: String, regId: String, scheme: VatScheme): Future[Option[VatScheme]] =
    vatSchemeRepository.upsertRegistration(internalId, regId, scheme)

  def deleteRegistration(internalId: String, regId: String)(implicit request: Request[_]): Future[Boolean] =
    vatSchemeRepository.deleteRegistration(internalId, regId)

  def getSection[T](internalId: String, regId: String, section: RegistrationSectionId)(implicit reads: Reads[T], request: Request[_]): Future[Option[T]] =
    for {
      optFoundSection <- vatSchemeRepository.getSection[JsValue](internalId, regId, section.repoKey)
      optValidatedSection = optFoundSection.flatMap(_.validate[T].asOpt)
    } yield optValidatedSection

  def upsertSection[T](internalId: String, regId: String, section: RegistrationSectionId, data: T)(implicit writes: Writes[T], request: Request[_]): Future[Option[T]] =
    vatSchemeRepository.upsertSection(internalId, regId, section.repoKey, data)

  def deleteSection(internalId: String, regId: String, section: RegistrationSectionId)(implicit request: Request[_]): Future[Boolean] =
    vatSchemeRepository.deleteSection(internalId, regId, section.repoKey)

  def getAnswer[T](internalId: String, regId: String, section: RegistrationSectionId, answer: String)(implicit reads: Reads[T], request: Request[_]): Future[Option[T]] =
    getSection[JsObject](internalId, regId, section) collect {
      case Some(sectionData) =>
        sectionData.transform((JsPath \ answer).json.pick)
          .flatMap(_.validate[T])
          .asOpt
      case None => None
    }

  def getSectionIndex(internalId: String, regId: String, section: CollectionSectionId, index: Int)(implicit request: Request[_]): Future[Option[JsValue]] =
    for {
      optFoundSection <- vatSchemeRepository.getSection[JsArray](internalId, regId, section.repoKey)
      optSectionIndex = optFoundSection.flatMap(_.value.lift(index - indexOffset))
    } yield optSectionIndex

  def upsertSectionIndex(internalId: String, regId: String, section: CollectionSectionId, data: JsValue, index: Int)(implicit request: Request[_]): Future[Option[JsValue]] = {
    for {
      oldSectionData <- vatSchemeRepository.getSection[JsArray](internalId, regId, section.repoKey).map(_.getOrElse(JsArray(Nil)))
      updatedSectionData = oldSectionData.copy(value = oldSectionData.value.patch(index - indexOffset, Seq(data), recordsToReplace))
      result <- vatSchemeRepository.upsertSection[JsArray](internalId, regId, section.repoKey, updatedSectionData)
      resultIndex = result.flatMap(_.value.lift(index - indexOffset))
    } yield resultIndex
  }

  def deleteSectionIndex(internalId: String, regId: String, section: CollectionSectionId, index: Int)(implicit request: Request[_]): Future[Option[JsValue]] = {
    for {
      oldSectionData <- vatSchemeRepository.getSection[JsArray](internalId, regId, section.repoKey).map(_.getOrElse(JsArray(Nil)))
      updatedSectionData = oldSectionData.copy(value = oldSectionData.value.patch(index - indexOffset, Nil, recordsToReplace))
      result <- if (updatedSectionData.value.isEmpty) {
        deleteSection(internalId, regId, section).map(_ => None)
      } else {
        vatSchemeRepository.upsertSection[JsArray](internalId, regId, section.repoKey, updatedSectionData)
      }
    } yield result
  }

}

object RegistrationService {
  val indexOffset = 1
  val recordsToReplace = 1
}