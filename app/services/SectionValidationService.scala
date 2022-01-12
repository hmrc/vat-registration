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

import models.api.returns.Returns
import models.api._
import models.registration._
import models.registration.sections.PartnersSection
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
      case BusinessContactSectionId => Future(validate[BusinessContact](json))
      case ComplianceSectionId => Future(validate[SicAndCompliance](json))
      case EligibilitySectionId => Future(validate[EligibilitySubmissionData](json))
      case FlatRateSchemeSectionId => Future(validate[FlatRateScheme](json))
      case PartnersSectionId => Future(validate[PartnersSection](json))
      case ReturnsSectionId => Future(validate[Returns](json))
      case TransactorSectionId => Future(validate[TransactorDetails](json))
      case TradingDetailsSectionId => Future(validate[TradingDetails](json))
    }

  private def validate[A <: RegistrationSection[A]](json: JsValue)(implicit format: Format[A]): Either[InvalidSection, ValidSection] =
    json.validate[A] match {
      case JsSuccess(value, _) =>
        Right(ValidSection(json, value.isComplete(value)))
      case JsError(errors) =>
        Left(InvalidSection(errors.map(_._1.toString())))
    }

}

sealed trait InvalidSectionResponse

case class InvalidSection(errors: Seq[String]) {
  def asString: String = errors.mkString(", ")
}

sealed trait ValidSectionResponse

case class ValidSection(validatedModel: JsValue, isComplete: Boolean)
