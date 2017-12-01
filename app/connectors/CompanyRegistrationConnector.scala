/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors

import javax.inject.{Inject, Singleton}

import common.RegistrationId
import config.WSHttp
import play.api.Logger
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.http._

@Singleton
class CompanyRegistrationConnector @Inject()() extends CompanyRegistrationConnect with ServicesConfig{
  val compRegUrl = baseUrl("company-registration")
  val http: CoreGet = WSHttp
}

trait CompanyRegistrationConnect {
  val compRegUrl: String
  val http: CoreGet

  def fetchCompanyRegistrationDocument(regId: RegistrationId)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val prefix = "[CompanyRegistrationConnector] - [fetchCompanyRegistrationDocument] :"
    http.GET[HttpResponse](s"$compRegUrl/company-registration/corporation-tax-registration/$regId/corporation-tax-registration") recover {
      case e: NotFoundException =>
        Logger.error(s"$prefix Received a NotFound status code when expecting reg document from Company-Registration for regId: $regId")
        throw e
      case e: ForbiddenException =>
        Logger.error(s"$prefix Received a Forbidden status code when expecting reg document from Company-Registration for regId: $regId")
        throw e
      case e: Exception =>
        Logger.error(s"$prefix Received error when expecting reg document from Company-Registration for regId: $regId - Error ${e.getMessage}")
        throw e
    }
  }
}
