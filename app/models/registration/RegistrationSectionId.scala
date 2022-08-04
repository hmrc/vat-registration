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

package models.registration

import play.api.mvc.PathBindable

trait RegistrationSectionId {
  val key: String
  val repoKey: String
}

case object ApplicantSectionId extends RegistrationSectionId {
  val key = "applicant"
  val repoKey = "applicantDetails"
}

case object AttachmentsSectionId extends RegistrationSectionId {
  val key = "attachments"
  val repoKey = key
}

case object BankAccountSectionId extends RegistrationSectionId {
  val key = "bank-account"
  val repoKey = "bankAccount"
}

case object EligibilitySectionId extends RegistrationSectionId {
  val key = "eligibility"
  val repoKey = "eligibilitySubmissionData"
}

case object EligibilityJsonSectionId extends RegistrationSectionId {
  val key = "eligibilityData"
  val repoKey = "eligibilityData"
}

case object FlatRateSchemeSectionId extends RegistrationSectionId {
  val key = "flat-rate-scheme"
  val repoKey = "flatRateScheme"
}

// TODO Rename to 'entities' in preparedness for VAT groups
case object PartnersSectionId extends RegistrationSectionId {
  val key = "partners"
  val repoKey = key
}

case object TransactorSectionId extends RegistrationSectionId {
  val key = "transactor"
  val repoKey = "transactorDetails"
}

case object BusinessSectionId extends RegistrationSectionId {
  val key = "business"
  val repoKey = "business"
}

case object VatApplicationSectionId extends RegistrationSectionId {
  val key = "vatApplication"
  val repoKey = key
}

case object StatusSectionId extends RegistrationSectionId {
  val key = "status"
  val repoKey = key
}

case object InformationDeclarationSectionId extends RegistrationSectionId {
  val key = "confirm-information-declaration"
  val repoKey = "confirmInformationDeclaration"
}

case object ApplicationReferenceSectionId extends RegistrationSectionId {
  val key = "application-reference"
  val repoKey = "applicationReference"
}

case object AcknowledgementReferenceSectionId extends RegistrationSectionId {
  val key = "acknowledgement-reference"
  val repoKey = "acknowledgementReference"
}

case object NrsSubmissionPayloadSectionId extends RegistrationSectionId {
  val key = "nrs-submission-payload"
  val repoKey = "nrsSubmissionPayload"
}

object RegistrationSectionId {
  // scalastyle:off
  implicit def urlBinder(implicit stringBinder: PathBindable[String]): PathBindable[RegistrationSectionId] =
    new PathBindable[RegistrationSectionId] {
      override def bind(key: String, value: String): Either[String, RegistrationSectionId] = {
        for {
          id <- stringBinder.bind(key, value).right
          section <- (key, id) match {
            case ("section", ApplicantSectionId.key) => Right(ApplicantSectionId)
            case ("section", AttachmentsSectionId.key) => Right(AttachmentsSectionId)
            case ("section", BankAccountSectionId.key) => Right(BankAccountSectionId)
            case ("section", EligibilitySectionId.key) => Right(EligibilitySectionId)
            case ("section", FlatRateSchemeSectionId.key) => Right(FlatRateSchemeSectionId)
            case ("section", PartnersSectionId.key) => Right(PartnersSectionId)
            case ("section", TransactorSectionId.key) => Right(TransactorSectionId)
            case ("section", OtherBusinessInvolvementsSectionId.key) => Right(OtherBusinessInvolvementsSectionId)
            case ("section", BusinessSectionId.key) => Right(BusinessSectionId)
            case ("section", VatApplicationSectionId.key) => Right(VatApplicationSectionId)
            case ("section", StatusSectionId.key) => Right(StatusSectionId)
            case ("section", InformationDeclarationSectionId.key) => Right(InformationDeclarationSectionId)
            case ("section", ApplicationReferenceSectionId.key) => Right(ApplicationReferenceSectionId)
            case ("section", AcknowledgementReferenceSectionId.key) => Right(AcknowledgementReferenceSectionId)
            case ("section", NrsSubmissionPayloadSectionId.key) => Right(NrsSubmissionPayloadSectionId)
            case _ => Left("Invalid registration section")
          }
        } yield section
      }

      override def unbind(key: String, value: RegistrationSectionId): String =
        stringBinder.unbind(key, value.key)
    }
}
