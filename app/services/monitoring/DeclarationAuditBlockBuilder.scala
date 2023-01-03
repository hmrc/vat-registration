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

import models.api.{Address, FormerName, Name, VatScheme}
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils.{jsonObject, _}

import javax.inject.Singleton

// scalastyle:off
@Singleton
class DeclarationAuditBlockBuilder {

  def buildDeclarationBlock(vatScheme: VatScheme): JsObject = {
    (vatScheme.applicantDetails, vatScheme.confirmInformationDeclaration, vatScheme.transactorDetails) match {
      case (Some(applicantDetails), Some(declaration), optTransactorDetails) =>
        jsonObject(
          "declarationSigning" -> jsonObject(
            "confirmInformationDeclaration" -> declaration,
            required("declarationCapacity" -> {
              if (optTransactorDetails.isDefined) {
                optTransactorDetails.flatMap(_.declarationCapacity.map(_.role))
              } else {
                applicantDetails.roleInTheBusiness.map(_.toDeclarationCapacity)
              }
            }.map(_.toString)),
            optional("capacityOther" -> optTransactorDetails.flatMap(_.declarationCapacity.flatMap(_.otherRole)))
          ),
          "applicant" -> jsonObject(
            "roleInBusiness" -> applicantDetails.roleInTheBusiness.map(_.toString),
            required("name" -> applicantDetails.personalDetails.map(details => formatName(details.name))),
            optionalRequiredIf(applicantDetails.changeOfName.hasFormerName.contains(true))(
              "previousName" -> formatFormerName(applicantDetails.changeOfName)
            ),
            "currentAddress" -> applicantDetails.currentAddress.map(formatAddress),
            optional("previousAddress" -> applicantDetails.previousAddress.map(formatAddress)),
            optionalRequiredIf(applicantDetails.personalDetails.exists(_.arn.isEmpty))(
              "dateOfBirth" -> applicantDetails.personalDetails.flatMap(_.dateOfBirth)
            ),
            "communicationDetails" -> jsonObject(
              optional("emailAddress" -> applicantDetails.contact.email),
              optional("telephone" -> applicantDetails.contact.tel)
            ),
            "identifiers" -> jsonObject(
              optional("nationalInsuranceNumber" -> applicantDetails.personalDetails.map(_.nino))
            )
          ),
          optional("agentOrCapacitor" -> optTransactorDetails.map { transactorDetails =>
            jsonObject(
              required("individualName" -> transactorDetails.personalDetails.map(details => formatName(details.name))),
              optionalRequiredIf(transactorDetails.isPartOfOrganisation.contains(true))(
                "organisationName" -> transactorDetails.organisationName
              ),
              "commDetails" -> jsonObject(
                "telephone" -> transactorDetails.telephone,
                "email" -> transactorDetails.email
              ),
              optionalRequiredIf(transactorDetails.personalDetails.exists(_.arn.isEmpty))(
                "address" -> transactorDetails.address.map(formatAddress)
              ),
              optionalRequiredIf(transactorDetails.personalDetails.exists(_.personalIdentifiers.nonEmpty))(
                "identification" -> transactorDetails.personalDetails.map(_.personalIdentifiers)
              )
            )
          })
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
