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

import models.api.{Address, FormerName, Name, VatScheme}
import models.submission.CustomerId
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.{Inject, Singleton}

// scalastyle:off
@Singleton
class DeclarationBlockBuilder @Inject()() {

  def buildDeclarationBlock(vatScheme: VatScheme): JsObject =
    (vatScheme.applicantDetails, vatScheme.confirmInformationDeclaration, vatScheme.transactorDetails) match {
      case (Some(applicantDetails), Some(declaration), optTransactorDetails) =>
        jsonObject(
          "declarationSigning" -> jsonObject(
            "confirmInformationDeclaration" -> declaration,
            "declarationCapacity" -> optTransactorDetails.map(_.declarationCapacity.role).getOrElse(
              applicantDetails.roleInBusiness.toDeclarationCapacity
            ),
            optional("capacityOther" -> optTransactorDetails.flatMap(_.declarationCapacity.otherRole))
          ),
          "applicantDetails" -> jsonObject(
            "roleInBusiness" -> applicantDetails.roleInBusiness,
            "name" -> formatName(applicantDetails.personalDetails.name),
            conditional(applicantDetails.changeOfName.exists(_.hasFormerName.contains(true)))(
              "prevName" -> applicantDetails.changeOfName.map(formatFormerName)
            ),
            optionalRequiredIf(applicantDetails.personalDetails.arn.isEmpty)("dateOfBirth" -> applicantDetails.personalDetails.dateOfBirth),
            "currAddress" -> formatAddress(applicantDetails.currentAddress),
            optional("prevAddress" -> applicantDetails.previousAddress.map(formatAddress)),
            "commDetails" -> jsonObject(
              optional("email" -> applicantDetails.contact.email),
              optional("telephone" -> applicantDetails.contact.tel),
              optional("mobileNumber" -> applicantDetails.contact.mobile)
            ),
            conditional(applicantDetails.personalDetails.personalIdentifiers.nonEmpty)(
              "identifiers" -> applicantDetails.personalDetails.personalIdentifiers
            )
          ),
          optional("agentOrCapacitor" -> optTransactorDetails.map { transactorDetails =>
            jsonObject(
              "individualName" -> formatName(transactorDetails.personalDetails.name),
              optionalRequiredIf(transactorDetails.isPartOfOrganisation.contains(true))("organisationName" -> transactorDetails.organisationName),
              "commDetails" -> jsonObject(
                "telephone" -> transactorDetails.telephone,
                "email" -> transactorDetails.email
              ),
              optionalRequiredIf(transactorDetails.personalDetails.arn.isEmpty)("address" -> transactorDetails.address.map(formatAddress)),
              conditional(transactorDetails.personalDetails.personalIdentifiers.nonEmpty)(
                "identification" -> transactorDetails.personalDetails.personalIdentifiers.map(identifier =>
                  Json.toJson(identifier)(CustomerId.transactorWrites)
                )
              )
            )
          })
        )
      case _ =>
        val appDetailsMissing = vatScheme.applicantDetails.fold(Option("applicantDetails"))(_ => None)
        val declarationMissing = vatScheme.confirmInformationDeclaration.fold(Option("declaration"))(_ => None)
        val message = Seq(appDetailsMissing, declarationMissing).flatten.mkString(", ")
        throw new InternalServerException(s"Could not construct declaration block because the following are missing: $message")
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
    optional("postCode" -> address.postcode),
    optional("countryCode" -> address.country.flatMap(_.code)),
    optional("addressValidated" -> address.addressValidated)
  )

}
