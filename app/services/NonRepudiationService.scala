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

import connectors.NonRepudiationConnector
import models.nonrepudiation.NonRepudiationAuditing.{NonRepudiationSubmissionFailureAudit, NonRepudiationSubmissionSuccessAudit}
import models.nonrepudiation.{IdentityData, NonRepudiationMetadata, NonRepudiationSubmissionAccepted, NonRepudiationSubmissionFailed}
import java.time.LocalDate
import play.api.mvc.Request
import repositories.UpscanMongoRepository
import services.NonRepudiationService._
import services.monitoring.AuditService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, InternalServerException}

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.Base64
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NonRepudiationService @Inject()(nonRepudiationConnector: NonRepudiationConnector,
                                      upscanMongoRepository: UpscanMongoRepository,
                                      auditService: AuditService,
                                      val authConnector: AuthConnector)(implicit ec: ExecutionContext) extends AuthorisedFunctions {

  def submitNonRepudiation(registrationId: String,
                           payloadString: String,
                           submissionTimestamp: LocalDateTime,
                           formBundleId: String,
                           userHeaders: Map[String, String],
                           digitalAttachments: Boolean = false
                          )(implicit hc: HeaderCarrier, request: Request[_]): Future[Option[String]] = for {
    identityData <- retrieveIdentityData()
    payloadChecksum = MessageDigest.getInstance("SHA-256")
      .digest(payloadString.getBytes(StandardCharsets.UTF_8))
      .map("%02x".format(_)).mkString
    userAuthToken = hc.authorization match {
      case Some(Authorization(authToken)) => authToken
      case _ => throw new InternalServerException("No auth token available for NRS")
    }
    digitalAttachmentIds <- if (digitalAttachments) {
      upscanMongoRepository.getAllUpscanDetails(registrationId).map(_.map(_.reference))
    } else {
      Future.successful(Nil)
    }
    nonRepudiationMetadata = NonRepudiationMetadata(
      "vrs",
      "vat-registration",
      "application/json",
      payloadChecksum,
      submissionTimestamp,
      identityData,
      userAuthToken,
      userHeaders,
      Map("formBundleId" -> formBundleId)
    )
    encodedPayloadString = Base64.getEncoder.encodeToString(payloadString.getBytes(StandardCharsets.UTF_8))
    nonRepudiationSubmissionResponse <- nonRepudiationConnector.submitNonRepudiation(encodedPayloadString, nonRepudiationMetadata, digitalAttachmentIds).map {
      case NonRepudiationSubmissionAccepted(submissionId) =>
        auditService.audit(NonRepudiationSubmissionSuccessAudit(registrationId, submissionId))
        Some(submissionId)
      case NonRepudiationSubmissionFailed(body, status) =>
        auditService.audit(NonRepudiationSubmissionFailureAudit(registrationId, status, body))
        None
    }
  } yield nonRepudiationSubmissionResponse

  private def retrieveIdentityData()(implicit headerCarrier: HeaderCarrier): Future[IdentityData] = {
    authConnector.authorise(EmptyPredicate, nonRepudiationIdentityRetrievals).map {
      case affinityGroup ~ internalId ~
        externalId ~ agentCode ~
        credentials ~ confidenceLevel ~
        nino ~ saUtr ~
        name ~ dateOfBirth ~
        email ~ agentInfo ~
        groupId ~ credentialRole ~
        mdtpInfo ~ itmpName ~
        itmpDateOfBirth ~ itmpAddress ~
        credentialStrength ~ loginTimes =>

        IdentityData(
          internalId = internalId,
          externalId = externalId,
          agentCode = agentCode,
          optionalCredentials = credentials,
          confidenceLevel = confidenceLevel,
          nino = nino,
          saUtr = saUtr,
          optionalName = name,
          dateOfBirth = dateOfBirth,
          email = email,
          agentInformation = agentInfo,
          groupIdentifier = groupId,
          credentialRole = credentialRole,
          mdtpInformation = mdtpInfo,
          optionalItmpName = itmpName,
          itmpDateOfBirth = itmpDateOfBirth,
          optionalItmpAddress = itmpAddress,
          affinityGroup = affinityGroup,
          credentialStrength = credentialStrength,
          loginTimes = loginTimes
        )
    }
  }
}

object NonRepudiationService {
  type NonRepudiationIdentityRetrievals =
    (Option[AffinityGroup] ~ Option[String]
      ~ Option[String] ~ Option[String]
      ~ Option[Credentials] ~ ConfidenceLevel
      ~ Option[String] ~ Option[String]
      ~ Option[Name] ~ Option[LocalDate]
      ~ Option[String] ~ AgentInformation
      ~ Option[String] ~ Option[CredentialRole]
      ~ Option[MdtpInformation] ~ Option[ItmpName]
      ~ Option[LocalDate] ~ Option[ItmpAddress]
      ~ Option[String] ~ LoginTimes)


  val nonRepudiationIdentityRetrievals: Retrieval[NonRepudiationIdentityRetrievals] =
    (Retrievals.affinityGroup and Retrievals.internalId and
      Retrievals.externalId and Retrievals.agentCode and
      Retrievals.credentials and Retrievals.confidenceLevel and
      Retrievals.nino and Retrievals.saUtr and
      Retrievals.name and Retrievals.dateOfBirth and
      Retrievals.email and Retrievals.agentInformation and
      Retrievals.groupIdentifier and Retrievals.credentialRole and
      Retrievals.mdtpInformation and Retrievals.itmpName and
      Retrievals.itmpDateOfBirth and Retrievals.itmpAddress and
      Retrievals.credentialStrength and Retrievals.loginTimes)
      .asInstanceOf[Retrieval[NonRepudiationIdentityRetrievals]]

}
