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

import connectors.{EmailConnector, EmailFailedToSend, EmailResponse}
import models.api.{EmailMethod, Post, VatScheme}
import play.api.mvc.Request
import repositories.VatSchemeRepository
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import utils.LoggingUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject()(emailConnector: EmailConnector,
                             registrationMongoRepository: VatSchemeRepository
                            )(implicit ex: ExecutionContext)  extends LoggingUtils{

  val basicTemplate = "mtdfb_vatreg_registration_received"
  val emailTemplate = s"${basicTemplate}_email"
  val postalTemplate = s"${basicTemplate}_post"
  val basicCyTemplate = s"${basicTemplate}_cy"
  val emailCyTemplate = s"${emailTemplate}_cy"
  val postalCyTemplate = s"${postalTemplate}_cy"

  private def missingDataLog(missingItem: String, regId: String) = s"Unable to send submission received email for $regId due to missing $missingItem"

  // scalastyle:off
  def sendRegistrationReceivedEmail(internalId: String, regId: String, lang: String)
                                   (implicit hc: HeaderCarrier, request: Request[_]): Future[EmailResponse] = {
    def resolveEmail(vatScheme: VatScheme): String = {
      val transactorEmail = vatScheme.transactorDetails.flatMap(_.email)
      val applicantEmail = vatScheme.applicantDetails.flatMap(_.contact.email).getOrElse{
        errorLog("[EmailService][sendRegistrationReceivedEmail] - applicant email address")
        throw new InternalServerException(missingDataLog("applicant email address", regId))
      }

      transactorEmail.getOrElse(applicantEmail)
    }

    def submissionParameters(vatScheme: VatScheme): Map[String, String] = {
      val transactorName = vatScheme.transactorDetails.flatMap(_.personalDetails.flatMap(_.name.first))
      val applicantName = vatScheme.applicantDetails.flatMap(_.personalDetails.flatMap(_.name.first))

      Map(
        "name" -> transactorName.getOrElse(applicantName.getOrElse{
          errorLog("[EmailService][sendRegistrationReceivedEmail][submissionParameters] - applicant Name")
          throw new InternalServerException(missingDataLog("applicant Name", regId))
        }),
        "ref" -> vatScheme.acknowledgementReference.getOrElse{
          errorLog("[EmailService][sendRegistrationReceivedEmail][submissionParameters] - acknowledgement Reference")
          throw new InternalServerException(missingDataLog("acknowledgement Reference", regId))
        }
      )
    }

    (for {
      optVatScheme <- registrationMongoRepository.getRegistration(internalId, regId)
      vatScheme = optVatScheme.getOrElse{
        errorLog("[EmailServiceEmailService][sendRegistrationReceivedEmail][submissionParameters] - VAT scheme")
        throw new InternalServerException(missingDataLog("VAT scheme", regId))
      }
      template = vatScheme.attachments.flatMap(_.method) match {
        case Some(EmailMethod) if lang.equals("cy") => emailCyTemplate
        case Some(EmailMethod) => emailTemplate
        case Some(Post) if lang.equals("cy") => postalCyTemplate
        case Some(Post) => postalTemplate
        case _ if lang.equals("cy") => basicCyTemplate
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
