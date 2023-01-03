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

import enums.VatRegStatus
import models.api._
import models.api.vatapplication.VatApplication
import models.registration._
import models.submission.PartyType
import play.api.libs.json._
import uk.gov.hmrc.http.InternalServerException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SectionValidationService @Inject()(registrationService: RegistrationService)
                                        (implicit ec: ExecutionContext) {

  val partyTypeKey = "partyType"

  // scalastyle:off
  def validate(internalId: String, regId: String, section: RegistrationSectionId, json: JsValue): Future[Either[InvalidSection, ValidSection]] =
    section match {
      case ApplicantSectionId =>
        registrationService.getAnswer[PartyType](internalId, regId, EligibilitySectionId, partyTypeKey).collect {
          case Some(partyType) =>
            validate[ApplicantDetails](json)(Format[ApplicantDetails](ApplicantDetails.reads(partyType), ApplicantDetails.writes))
          case _ =>
            throw new InternalServerException("[SectionValidationService] Couldn't parse Applicant section due to missing party type")
        }
      case AttachmentsSectionId => Future(validate[Attachments](json))
      case BankAccountSectionId => Future(validate[BankAccount](json))
      case EligibilitySectionId => Future(validate[EligibilitySubmissionData](json))
      case FlatRateSchemeSectionId => Future(validate[FlatRateScheme](json))
      case EntitiesSectionId => Future(validate[List[Entity]](json))
      case TransactorSectionId => Future(validate[TransactorDetails](json))
      case OtherBusinessInvolvementsSectionId => Future(validate[List[OtherBusinessInvolvement]](json))
      case BusinessSectionId => Future(validate[Business](json))
      case VatApplicationSectionId => Future(validate[VatApplication](json))
      case StatusSectionId => Future(validate[VatRegStatus.Value](json))
      case InformationDeclarationSectionId => Future(validate[Boolean](json))
      case ApplicationReferenceSectionId => Future(validate[String](json))
      case AcknowledgementReferenceSectionId => Future(validate[String](json))
      case NrsSubmissionPayloadSectionId => Future(validate[String](json))
      case unknown => throw new InternalServerException(s"[SectionValidationService] Attempted to validate an unsupported section: ${unknown.toString}")
    }

  def validateIndex(section: CollectionSectionId, json: JsValue): Future[Either[InvalidSection, ValidSection]] =
    section match {
      case OtherBusinessInvolvementsSectionId => Future(validate[OtherBusinessInvolvement](json))
      case EntitiesSectionId => Future(validate[Entity](json))
      case unknown => throw new InternalServerException(s"[SectionValidationService] Attempted to validate an unsupported collection section: ${unknown.toString}")
    }

  private def validate[A](json: JsValue)(implicit format: Format[A]): Either[InvalidSection, ValidSection] =
    json.validate[A] match {
      case JsSuccess(value, _) =>
        Right(ValidSection(Json.toJson(value)))
      case JsError(errors) =>
        Left(InvalidSection(errors.map(_._1.toString())))
    }

}

sealed trait InvalidSectionResponse

case class InvalidSection(errors: Seq[String]) {
  def asString: String = errors.mkString(", ")
}

sealed trait ValidSectionResponse

case class ValidSection(validatedModel: JsValue)
