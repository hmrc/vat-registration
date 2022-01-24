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

package config

import featureswitch.core.config.{FeatureSwitching, StubSubmission}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

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
    val enrolledNonUkCompany = servicesConfig.getInt("traffic-management.quotas.non-uk-company-enrolled")
    val nonUkCompany = servicesConfig.getInt("traffic-management.quotas.non-uk-company")
    val enrolledRegSociety = servicesConfig.getInt("traffic-management.quotas.reg-society-enrolled")
    val regSociety = servicesConfig.getInt("traffic-management.quotas.reg-society")
    val enrolledCharitableIncorpOrg = servicesConfig.getInt("traffic-management.quotas.charitable-incorp-org-enrolled")
    val charitableIncorpOrg = servicesConfig.getInt("traffic-management.quotas.charitable-incorp-org")
    val enrolledPartnership = servicesConfig.getInt("traffic-management.quotas.partnership-enrolled")
    val partnership = servicesConfig.getInt("traffic-management.quotas.partnership")
    val enrolledLtdPartnership = servicesConfig.getInt("traffic-management.quotas.limited-partnership-enrolled")
    val ltdPartnership = servicesConfig.getInt("traffic-management.quotas.limited-partnership")
    val enrolledScotPartnership = servicesConfig.getInt("traffic-management.quotas.scottish-partnership-enrolled")
    val scotPartnership = servicesConfig.getInt("traffic-management.quotas.scottish-partnership")
    val enrolledScotLtdPartnership = servicesConfig.getInt("traffic-management.quotas.scottish-limited-partnership-enrolled")
    val scotLtdPartnership = servicesConfig.getInt("traffic-management.quotas.scottish-limited-partnership")
    val enrolledLtdLiabilityPartnership = servicesConfig.getInt("traffic-management.quotas.limited-liability-partnership-enrolled")
    val ltdLiabilityPartnership = servicesConfig.getInt("traffic-management.quotas.limited-liability-partnership")
    val enrolledTrust = servicesConfig.getInt("traffic-management.quotas.trust-enrolled")
    val trust = servicesConfig.getInt("traffic-management.quotas.trust")
    val enrolledUnincorpAssoc = servicesConfig.getInt("traffic-management.quotas.unincorp-assoc-enrolled")
    val unincorpAssoc = servicesConfig.getInt("traffic-management.quotas.unincorp-assoc")
  }

  lazy val nonRepudiationSubmissionUrl: String = servicesConfig.baseUrl("non-repudiation") + "/submission"
  lazy val nonRepudiationApiKey: String = servicesConfig.getString("microservice.services.non-repudiation.api-key")

  lazy val expiryInSeconds: Int = servicesConfig.getInt("cache.expiryInSeconds")
  lazy val dailyQuotaExpiryInSeconds: Int = servicesConfig.getInt("traffic-management.quotas.time-to-live")

}
