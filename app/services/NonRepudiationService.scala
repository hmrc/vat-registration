/*
 * Copyright 2020 HM Revenue & Customs
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

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.Base64

import connectors.NonRepudiationConnector
import javax.inject.{Inject, Singleton}
import models.nonrepudiation.NonRepudiationAuditing.{NonRepudiationSubmissionFailureAudit, NonRepudiationSubmissionSuccessAudit}
import models.nonrepudiation.{IdentityData, NonRepudiationMetadata, NonRepudiationSubmissionAccepted}
import org.joda.time.LocalDate
import play.api.mvc.Request
import services.NonRepudiationService._
import services.monitoring.AuditService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.http.{HeaderCarrier, HttpException, InternalServerException}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NonRepudiationService @Inject()(nonRepudiationConnector: NonRepudiationConnector,
                                      auditService: AuditService,
                                      val authConnector: AuthConnector)(implicit ec: ExecutionContext) extends AuthorisedFunctions {
  def submitNonRepudiation(registrationId: String,
                           payloadString: String,
                           submissionTimestamp: LocalDateTime,
                           postCode: String
                          )(implicit hc: HeaderCarrier, request: Request[_]): Future[NonRepudiationSubmissionAccepted] = for {
    identityData <- retrieveIdentityData()
    payloadChecksum = MessageDigest.getInstance("SHA-256")
      .digest(payloadString.getBytes(StandardCharsets.UTF_8))
      .map("%02x".format(_)).mkString
    userAuthToken = hc.authorization match {
      case Some(Authorization(authToken)) => authToken
      case _ => throw new InternalServerException("No auth token available for NRS")
    }
    headerData = request.headers.toSimpleMap
    nonRepudiationMetadata = NonRepudiationMetadata(
      "vrs",
      "vat-registration",
      "application/json",
      payloadChecksum,
      submissionTimestamp,
      identityData,
      userAuthToken,
      headerData,
      Map("postCode" -> postCode)
    )
    encodedPayloadString = Base64.getEncoder.encodeToString(payloadString.getBytes(StandardCharsets.UTF_8))
    nonRepudiationSubmissionResponse <- nonRepudiationConnector.submitNonRepudiation(encodedPayloadString, nonRepudiationMetadata).map {
      case response@NonRepudiationSubmissionAccepted(submissionId) =>
        auditService.audit(NonRepudiationSubmissionSuccessAudit(registrationId, submissionId))
        response
    }.recoverWith {
      case exception: HttpException =>
        auditService.audit(NonRepudiationSubmissionFailureAudit(registrationId, exception.responseCode, exception.message))
        Future.failed(exception)
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
          credentials = credentials,
          confidenceLevel = confidenceLevel,
          nino = nino,
          saUtr = saUtr,
          name = name,
          dateOfBirth = dateOfBirth,
          email = email,
          agentInformation = agentInfo,
          groupIdentifier = groupId,
          credentialRole = credentialRole,
          mdtpInformation = mdtpInfo,
          itmpName = itmpName,
          itmpDateOfBirth = itmpDateOfBirth,
          itmpAddress = itmpAddress,
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
    Retrievals.affinityGroup and Retrievals.internalId and
      Retrievals.externalId and Retrievals.agentCode and
      Retrievals.credentials and Retrievals.confidenceLevel and
      Retrievals.nino and Retrievals.saUtr and
      Retrievals.name and Retrievals.dateOfBirth and
      Retrievals.email and Retrievals.agentInformation and
      Retrievals.groupIdentifier and Retrievals.credentialRole and
      Retrievals.mdtpInformation and Retrievals.itmpName and
      Retrievals.itmpDateOfBirth and Retrievals.itmpAddress and
      Retrievals.credentialStrength and Retrievals.loginTimes

}
