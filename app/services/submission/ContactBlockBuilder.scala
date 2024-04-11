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

package services.submission

import models.api.{ContactPreference, Email, Letter, VatScheme}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._
import utils.LoggingUtils

import javax.inject.{Inject, Singleton}

@Singleton
class ContactBlockBuilder @Inject() () extends LoggingUtils {

  def buildContactBlock(vatScheme: VatScheme)(implicit request: Request[_]): JsObject =
    (vatScheme.business, vatScheme.applicantDetails) match {
      case (Some(business), Some(applicantDetails)) =>
        Json.obj(
          "address"     -> jsonObject(
            required("line1"            -> business.ppobAddress.map(_.line1.replaceAll(",", " ").replaceAll("\\s+", " "))),
            optional(
              "line2"                   -> business.ppobAddress.flatMap(_.line2.map(_.replaceAll(",", " ").replaceAll("\\s+", " ")))
            ),
            optional(
              "line3"                   -> business.ppobAddress.flatMap(_.line3.map(_.replaceAll(",", " ").replaceAll("\\s+", " ")))
            ),
            optional(
              "line4"                   -> business.ppobAddress.flatMap(_.line4.map(_.replaceAll(",", " ").replaceAll("\\s+", " ")))
            ),
            optional(
              "line5"                   -> business.ppobAddress.flatMap(_.line5.map(_.replaceAll(",", " ").replaceAll("\\s+", " ")))
            ),
            optional("postCode"         -> business.ppobAddress.flatMap(_.postcode)),
            optional("countryCode"      -> business.ppobAddress.flatMap(_.country.flatMap(_.code))),
            optional("addressValidated" -> business.ppobAddress.flatMap(_.addressValidated))
          ),
          "commDetails" -> jsonObject(
            required("telephone"                                         -> business.telephoneNumber),
            required("email"                                             -> business.email),
            "emailVerified"   -> (if (
                                  applicantDetails.contact.email.exists(
                                    business.email.contains(_)
                                  ) && applicantDetails.contact.emailVerified.contains(true)
                                ) {
                                  true
                                } else {
                                  false
                                }),
            conditional(business.hasWebsite.contains(true))("webAddress" -> business.website),
            "commsPreference" -> (business.contactPreference match {
              case Some(Email)  => ContactPreference.electronic
              case Some(Letter) => ContactPreference.paper
            })
          )
        )
      case _                                        =>
        errorLog(
          "[ContactBlockBuilder][buildContactBlock] - Could not build contact block for submission due to missing data"
        )
        throw new InternalServerException("Could not build contact block for submission due to missing data")
    }

}
