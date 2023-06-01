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

package services.monitoring

import models.api.{ContactPreference, Email, Letter, VatScheme}
import play.api.libs.json.JsObject
import play.api.mvc.Request
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._
import utils.LoggingUtils

import javax.inject.Singleton

@Singleton
class ContactAuditBlockBuilder extends LoggingUtils{

  def buildContactBlock(vatScheme: VatScheme)(implicit request: Request[_]): JsObject =
    (vatScheme.business, vatScheme.applicantDetails) match {
      case (Some(business), Some(applicantDetails)) =>
        jsonObject(
          "address" -> jsonObject(
            required("line1" -> business.ppobAddress.map(_.line1)),
            optional("line2" -> business.ppobAddress.flatMap(_.line2)),
            optional("line3" -> business.ppobAddress.flatMap(_.line3)),
            optional("line4" -> business.ppobAddress.flatMap(_.line4)),
            optional("line5" -> business.ppobAddress.flatMap(_.line5)),
            optional("postcode" -> business.ppobAddress.flatMap(_.postcode)),
            optional("countryCode" -> business.ppobAddress.flatMap(_.country.flatMap(_.code)))
          ),
          "businessCommunicationDetails" -> jsonObject(
            required("telephone" -> business.telephoneNumber),
            required("emailAddress" -> business.email),
            "emailVerified" -> (
              if (applicantDetails.contact.email.exists(business.email.contains(_)) && applicantDetails.contact.emailVerified.contains(true)) {
                true
              } else {
                false
              }),
            conditional(business.hasWebsite.contains(true))("webAddress" -> business.website),
            "preference" -> (business.contactPreference match {
              case Some(Email) => ContactPreference.electronic
              case Some(Letter) => ContactPreference.paper
            })
          ))
      case _ =>
        errorLog("[ContactAuditBlockBuilder][buildContactBlock] - Could not build contact block for submission due to missing data")
        throw new InternalServerException("[ContactAuditBlockBuilder]: Could not build contact block for submission due to missing data")
    }

}
