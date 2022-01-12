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

import featureswitch.core.config.FeatureSwitching
import models.api.VatScheme
import models.registration.RegistrationSectionId
import play.api.libs.json._
import repositories.VatSchemeRepository

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

  @deprecated("use upsertRegistration instead")
  def insertVatScheme(vatScheme: VatScheme): Future[VatScheme] =
    vatSchemeRepository.insertVatScheme(vatScheme)


  def getAllRegistrations[T](internalId: String)(implicit reads: Reads[T]): Future[List[T]] =
    vatSchemeRepository.getAllRegistrations(internalId).map {
      case Nil => Nil
      case registrations: List[JsValue] => registrations.flatMap(_.validate[T].asOpt)
    }

  def getRegistration[T](internalId: String, regId: String)(implicit reads: Reads[T]): Future[Option[T]] =
    vatSchemeRepository.getRegistration(internalId, regId).map {
      case Some(registration) => registration.validate[T].asOpt
      case None => None
    }

  def upsertRegistration[T](internalId: String, regId: String, data: T)(implicit writes: Format[T]): Future[Option[T]] =
    vatSchemeRepository.upsertRegistration(internalId, regId, Json.toJson(data)).collect {
      case Some(updatedRegistration) => Json.fromJson[T](updatedRegistration).asOpt
    }

  def deleteRegistration(internalId: String, regId: String): Future[Boolean] =
    vatSchemeRepository.deleteRegistration(internalId, regId)

  def getSection[T](internalId: String, regId: String, section: RegistrationSectionId)(implicit reads: Reads[T]): Future[Option[T]] =
    for {
      optFoundSection <- vatSchemeRepository.getSection[JsValue](internalId, regId, section.repoKey)
      optValidatedSection = optFoundSection.flatMap(_.validate[T].asOpt)
    } yield optValidatedSection

  def upsertSection[T](internalId: String, regId: String, section: RegistrationSectionId, data: T)(implicit writes: Writes[T]): Future[Option[T]] =
    vatSchemeRepository.upsertSection(internalId, regId, section.repoKey, data)

  def getAnswer[T](internalId: String, regId: String, section: RegistrationSectionId, answer: String)(implicit reads: Reads[T]): Future[Option[T]] =
    getSection[JsObject](internalId, regId, section) collect {
      case Some(sectionData) =>
        sectionData.transform((JsPath \ answer).json.pick)
          .flatMap(_.validate[T])
          .asOpt
      case None => None
    }

}
