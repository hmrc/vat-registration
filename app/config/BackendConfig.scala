/*
 * Copyright 2021 HM Revenue & Customs
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
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

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

  lazy val allowUsersFrom: Int = servicesConfig.getInt("traffic-management.hours.from")
  lazy val allowUsersUntil: Int = servicesConfig.getInt("traffic-management.hours.until")

  object DailyQuotas {
    val enrolledUkCompany = servicesConfig.getInt("traffic-management.quotas.uk-company-enrolled")
    val ukCompany = servicesConfig.getInt("traffic-management.quotas.uk-company")
    val enrolledSoleTrader = servicesConfig.getInt("traffic-management.quotas.sole-trader-enrolled")
    val soleTrader = servicesConfig.getInt("traffic-management.quotas.sole-trader")
    val enrolledNetp = servicesConfig.getInt("traffic-management.quotas.netp-enrolled")
    val netp = servicesConfig.getInt("traffic-management.quotas.netp")
  }

  lazy val nonRepudiationSubmissionUrl: String = servicesConfig.baseUrl("non-repudiation") + "/submission"
  lazy val nonRepudiationApiKey: String = servicesConfig.getString("microservice.services.non-repudiation.api-key")

  lazy val expiryInSeconds: String = servicesConfig.getString("cache.expiryInSeconds")
  lazy val dailyQuotaExpiryInSeconds: Int = servicesConfig.getInt("traffic-management.quotas.time-to-live")

}
