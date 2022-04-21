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

package services.submission

import models.api.{ContactPreference, Email, Letter}
import play.api.libs.json.{JsObject, Json}
import repositories.VatSchemeRepository
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContactBlockBuilder @Inject()(registrationMongoRepository: VatSchemeRepository)
                                   (implicit ec: ExecutionContext) {

  def buildContactBlock(regId: String): Future[JsObject] = for {
    optVatScheme <- registrationMongoRepository.retrieveVatScheme(regId)
  } yield optVatScheme match {
    case Some(vatScheme) =>
      (vatScheme.businessContact, vatScheme.applicantDetails) match {
        case (Some(businessContact), Some(applicantDetails)) =>
          Json.obj(
            "address" -> jsonObject(
              "line1" -> businessContact.ppob.line1,
              optional("line2" -> businessContact.ppob.line2),
              optional("line3" -> businessContact.ppob.line3),
              optional("line4" -> businessContact.ppob.line4),
              optional("line5" -> businessContact.ppob.line5),
              optional("postCode" -> businessContact.ppob.postcode),
              optional("countryCode" -> businessContact.ppob.country.flatMap(_.code)),
              optional("addressValidated" -> businessContact.ppob.addressValidated)
            ),
            "commDetails" -> jsonObject(
              optional("telephone" -> businessContact.digitalContact.tel),
              optional("mobileNumber" -> businessContact.digitalContact.mobile),
              "email" -> businessContact.digitalContact.email,
              "emailVerified" -> (
                if (applicantDetails.contact.email.contains(businessContact.digitalContact.email) && applicantDetails.contact.emailVerified.contains(true)) {
                  true
                } else {
                  false
                }),
              "commsPreference" -> (businessContact.commsPreference match {
                case Email => ContactPreference.electronic
                case Letter => ContactPreference.paper
              })
            )
          )
      }
    case _ =>
      throw new InternalServerException("Could not build contact block for submission due to missing data")
  }

}
