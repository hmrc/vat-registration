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

package services.submission

import models.api.{Address, FormerName, Name}
import play.api.libs.json.{JsObject, Json}
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils.{jsonObject, _}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeclarationBlockBuilder @Inject()(registrationMongoRepository: RegistrationMongoRepository)
                                       (implicit ec: ExecutionContext) {

  def buildDeclarationBlock(regId: String): Future[JsObject] = {
    for {
      vatScheme <- registrationMongoRepository.retrieveVatScheme(regId)
    } yield vatScheme match {
      case Some(scheme) =>
        (scheme.applicantDetails, scheme.confirmInformationDeclaration) match {
          case (Some(applicantDetails), Some(declaration)) =>
            jsonObject(
              "declarationSigning" -> Json.obj(
                "confirmInformationDeclaration" -> declaration,
                "declarationCapacity" -> applicantDetails.roleInBusiness
              ),
              "applicantDetails" -> jsonObject(
                "roleInBusiness" -> applicantDetails.roleInBusiness,
                "name" -> formatName(applicantDetails.transactor.name),
                optional("prevName" -> applicantDetails.changeOfName.map(formatFormerName)),
                "dateOfBirth" -> applicantDetails.transactor.dateOfBirth,
                "currAddress" -> formatAddress(applicantDetails.currentAddress),
                optional("prevAddress" -> applicantDetails.previousAddress.map(formatAddress)),
                "commDetails" -> jsonObject(
                  optional("email" -> applicantDetails.contact.email),
                  optional("telephone" -> applicantDetails.contact.tel),
                  optional("mobileNumber" -> applicantDetails.contact.mobile)
                ),
                "identifiers" -> applicantDetails.personalIdentifiers
              )
            )
          case _ =>
            val appDetailsMissing = scheme.applicantDetails.map(_ => "applicantDetails")
            val declarationMissing = scheme.confirmInformationDeclaration.map(_ => "declaration")
            val message = Seq(appDetailsMissing, declarationMissing).flatten.mkString(", ")
            throw new InternalServerException(s"Could not construct declaration block because the following are missing: $message")
        }
      case _ =>
        throw new InternalServerException("Could not construct declaration block due to missing VatScheme")
    }
  }

  private def formatName(name: Name): JsObject = jsonObject(
    optional("firstName" -> name.first),
    optional("middleName" -> name.middle),
    "lastName" -> name.last
  )

  private def formatFormerName(formerName: FormerName): Option[JsObject] =
    formerName.name.map(name =>
      formatName(name) ++ jsonObject(optional("nameChangeDate" -> formerName.change))
    )

  private def formatAddress(address: Address): JsObject = jsonObject(
    "line1" -> address.line1,
    "line2" -> address.line2,
    optional("line3" -> address.line3),
    optional("line4" -> address.line4),
    optional("postCode" -> address.postcode),
    optional("countryCode" -> address.country.flatMap(_.code)),
    optional("addressValidated" -> address.addressValidated)
  )

}
