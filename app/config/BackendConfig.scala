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

package config

import featureswitch.core.config.{FeatureSwitching, StubSubmission}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.Retrying

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, FiniteDuration}

@Singleton
class BackendConfig @Inject()(val servicesConfig: ServicesConfig,
                              val runModeConfiguration: Configuration) extends FeatureSwitching {

  def loadConfig(key: String): String = servicesConfig.getString(key)

  lazy val vatRegistrationUrl: String = servicesConfig.baseUrl("vat-registration")
  lazy val integrationFrameworkBaseUrl: String = servicesConfig.getString("microservice.services.integration-framework.url")

  def vatSubmissionUrl: String = {
    val submissionEndpointUri = "/vat/subscription"

    if (isEnabled(StubSubmission)) {
      s"$vatRegistrationUrl/vatreg/test-only$submissionEndpointUri"
    }
    else {
      integrationFrameworkBaseUrl + submissionEndpointUri
    }
  }

  lazy val urlHeaderEnvironment: String = servicesConfig.getString("microservice.services.integration-framework.environment")
  lazy val urlHeaderAuthorization: String = s"Bearer ${servicesConfig.getString("microservice.services.integration-framework.authorization-token")}"
  lazy val nonRepudiationUrl: String = servicesConfig.baseUrl("non-repudiation")

  val nrsConfig = runModeConfiguration.get[Configuration]("microservice.services.non-repudiation")
  lazy val nrsRetries: List[FiniteDuration] =
    Retrying.fibonacciDelays(getFiniteDuration(nrsConfig), nrsConfig.get[Int]("numberOfRetries"))

  private final def getFiniteDuration(config: Configuration, path: String = "initialDelay"): FiniteDuration = {
    val string = config.get[String](path)

    Duration.create(string) match {
      case f: FiniteDuration => f
      case _ => throw new RuntimeException(s"Not a finite duration '$string' for $path")
    }
  }

  def nonRepudiationSubmissionUrl: String = {
    val endpoint = "/submission"

    if (isEnabled(StubSubmission)) {
      s"$vatRegistrationUrl/vatreg/test-only$endpoint"
    }
    else {
      nonRepudiationUrl + endpoint
    }
  }

  def attachmentNonRepudiationSubmissionUrl: String = {
    val endpoint = "/attachment"

    if (isEnabled(StubSubmission)) {
      s"$vatRegistrationUrl/vatreg/test-only$endpoint"
    }
    else {
      nonRepudiationUrl + endpoint
    }
  }

  lazy val nonRepudiationApiKey: String = servicesConfig.getString("microservice.services.non-repudiation.api-key")

  lazy val expiryInSeconds: Int = servicesConfig.getInt("cache.expiryInSeconds")
  lazy val upscanRepositoryExpiryInSeconds: Int = servicesConfig.getInt("cache.upscan.expiryInSeconds")
  lazy val sdesUrl: String = servicesConfig.baseUrl("sdes")

  def sdesNotificationUrl: String = {
    val endpoint = "/notification/fileready"

    if (isEnabled(StubSubmission)) {
      s"$vatRegistrationUrl/vatreg/test-only$endpoint"
    }
    else {
      sdesUrl + endpoint
    }
  }

  lazy val emailBaseUrl = servicesConfig.baseUrl("email")

  def sendEmailUrl: String = s"$emailBaseUrl/hmrc/email"

  lazy val sdesAuthorizationToken: String = servicesConfig.getString("microservice.services.sdes.api-key")
  lazy val sdesInformationType = servicesConfig.getString("microservice.services.sdes.informationType")
  lazy val sdesRecipientOrSender = servicesConfig.getString("microservice.services.sdes.recipientOrSender")

  lazy val emailCheck: List[String] = servicesConfig.getString("constants.emailCheck").split(',').toList
}
