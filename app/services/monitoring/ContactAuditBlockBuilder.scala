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

package services.monitoring

import models.api.{ContactPreference, Email, Letter, VatScheme}
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.Singleton

@Singleton
class ContactAuditBlockBuilder {

  def buildContactBlock(vatScheme: VatScheme): JsObject =
    (vatScheme.businessContact, vatScheme.applicantDetails) match {
      case (Some(businessContact), Some(applicantDetails)) =>
        jsonObject(
          "address" -> jsonObject(
            "line1" -> businessContact.ppob.line1,
            optional("line2" -> businessContact.ppob.line2),
            optional("line3" -> businessContact.ppob.line3),
            optional("line4" -> businessContact.ppob.line4),
            optional("line5" -> businessContact.ppob.line5),
            optional("postcode" -> businessContact.ppob.postcode),
            optional("countryCode" -> businessContact.ppob.country.flatMap(_.code))
          ),
          "businessCommunicationDetails" -> jsonObject(
            optional("telephone" -> businessContact.telephoneNumber),
            "emailAddress" -> businessContact.email,
            "emailVerified" -> (
              if (applicantDetails.contact.email.exists(businessContact.email.contains(_)) && applicantDetails.contact.emailVerified.contains(true)) {
                true
              } else {
                false
              }),
            optional("webAddress" -> businessContact.website),
            "preference" -> (businessContact.commsPreference match {
              case Email => ContactPreference.electronic
              case Letter => ContactPreference.paper
            })
          ))
      case _ =>
        throw new InternalServerException("[ContactAuditBlockBuilder]: Could not build contact block for submission due to missing data")
    }

}
