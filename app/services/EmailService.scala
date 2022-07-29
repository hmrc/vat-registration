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

import connectors.{EmailConnector, EmailFailedToSend, EmailResponse}
import models.api.{EmailMethod, Post, VatScheme}
import repositories.VatSchemeRepository
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject()(emailConnector: EmailConnector,
                             registrationMongoRepository: VatSchemeRepository
                            )(implicit ex: ExecutionContext) {

  val basicTemplate = "mtdfb_vatreg_registration_received"
  val emailTemplate = s"${basicTemplate}_email"
  val postalTemplate = s"${basicTemplate}_post"

  private def missingDataLog(missingItem: String, regId: String) = s"Unable to send submission received email for $regId due to missing $missingItem"

  def sendRegistrationReceivedEmail(regId: String)
                                   (implicit hc: HeaderCarrier): Future[EmailResponse] = {
    def resolveEmail(vatScheme: VatScheme): String = {
      val transactorEmail = vatScheme.transactorDetails.map(_.email)
      val applicantEmail = vatScheme.applicantDetails.flatMap(_.contact.email)
        .getOrElse(throw new InternalServerException(missingDataLog("applicant email address", regId)))

      transactorEmail.getOrElse(applicantEmail)
    }

    def submissionParameters(vatScheme: VatScheme): Map[String, String] = {
      val transactorName = vatScheme.transactorDetails.flatMap(_.personalDetails.name.first)
      val applicantName = vatScheme.applicantDetails.flatMap(_.personalDetails.name.first)

      Map(
        "name" -> transactorName.getOrElse(applicantName.getOrElse(throw new InternalServerException(missingDataLog("applicant Name", regId)))),
        "ref" -> vatScheme.acknowledgementReference.getOrElse(throw new InternalServerException(missingDataLog("acknowledgement Reference", regId)))
      )
    }

    (for {
      optVatScheme <- registrationMongoRepository.retrieveVatScheme(regId)
      vatScheme = optVatScheme.getOrElse(throw new InternalServerException(missingDataLog("VAT scheme", regId)))
      template = vatScheme.attachments.map(_.method) match {
        case Some(EmailMethod) => emailTemplate
        case Some(Post) => postalTemplate
        case _ => basicTemplate
      }
      email = resolveEmail(vatScheme)
      params = submissionParameters(vatScheme)
      response <- emailConnector.sendEmail(email, template, params, force = true)
    } yield response).recover {
      case _: Exception => EmailFailedToSend
    }
  }

}
