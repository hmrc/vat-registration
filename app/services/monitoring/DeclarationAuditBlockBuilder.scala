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

package services.monitoring

import models.api.{Address, FormerName, Name, VatScheme}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils.{jsonObject, _}

import javax.inject.Singleton

@Singleton
class DeclarationAuditBlockBuilder {

  def buildDeclarationBlock(vatScheme: VatScheme): JsObject = {
    (vatScheme.applicantDetails, vatScheme.confirmInformationDeclaration) match {
      case (Some(applicantDetails), Some(declaration)) =>
        jsonObject(
          "declarationSigning" -> Json.obj(
            "confirmInformationDeclaration" -> declaration,
            "declarationCapacity" -> applicantDetails.roleInBusiness.toString
          ),
          "applicant" -> jsonObject(
            "roleInBusiness" -> applicantDetails.roleInBusiness.toString,
            "name" -> formatName(applicantDetails.transactor.name),
            optional("previousName" -> applicantDetails.changeOfName.map(formatFormerName)),
            "currentAddress" -> formatAddress(applicantDetails.currentAddress),
            optional("previousAddress" -> applicantDetails.previousAddress.map(formatAddress)),
            "dateOfBirth" -> applicantDetails.transactor.dateOfBirth,
            "communicationDetails" -> jsonObject(
              optional("emailAddress" -> applicantDetails.contact.email),
              optional("telephone" -> applicantDetails.contact.tel),
              optional("mobileNumber" -> applicantDetails.contact.mobile)
            ),
            "identifiers" -> jsonObject(
              optional("nationalInsuranceNumber" -> applicantDetails.transactor.nino)
            )
          )
        )
      case _ =>
        throw new InternalServerException(
          s"[DeclarationBlockBuilder] Could not construct declaration block because the following are missing:" +
            s"ApplicantDetails found - ${vatScheme.applicantDetails.isDefined}, " +
            s"Declaration found - ${vatScheme.confirmInformationDeclaration.isDefined}."
        )
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
    optional("line2" -> address.line2),
    optional("line3" -> address.line3),
    optional("line4" -> address.line4),
    optional("line5" -> address.line5),
    optional("postcode" -> address.postcode),
    optional("countryCode" -> address.country.flatMap(_.code))
  )

}
