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

import models.api.{Address, FormerName, Name, VatScheme}
import models.submission.{CustomerId, Other}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Request
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._
import utils.LoggingUtils

import javax.inject.{Inject, Singleton}

// scalastyle:off
@Singleton
class DeclarationBlockBuilder @Inject() () extends LoggingUtils {

  def buildDeclarationBlock(vatScheme: VatScheme)(implicit request: Request[_]): JsObject =
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
            }),
            optionalRequiredIf(
              optTransactorDetails.exists(_.declarationCapacity.exists(_.role.equals(Other))) ||
                (optTransactorDetails.isEmpty && applicantDetails.roleInTheBusiness.contains(Other))
            )(
              "capacityOther"              -> {
                if (optTransactorDetails.isDefined) {
                  optTransactorDetails.flatMap(_.declarationCapacity.flatMap(_.otherRole))
                } else {
                  applicantDetails.otherRoleInTheBusiness
                }
              }
            )
          ),
          "applicantDetails"   -> jsonObject(
            required("roleInBusiness" -> applicantDetails.roleInTheBusiness),
            optionalRequiredIf(applicantDetails.roleInTheBusiness.contains(Other))(
              "otherRole"             -> applicantDetails.otherRoleInTheBusiness
            ),
            required("name"           -> applicantDetails.personalDetails.map(details => formatName(details.name))),
            optionalRequiredIf(applicantDetails.changeOfName.hasFormerName.contains(true))(
              "prevName"              -> formatFormerName(applicantDetails.changeOfName)
            ),
            optionalRequiredIf(applicantDetails.personalDetails.exists(_.arn.isEmpty))(
              "dateOfBirth"           -> applicantDetails.personalDetails.flatMap(_.dateOfBirth)
            ),
            required("currAddress"    -> applicantDetails.currentAddress.map(formatAddress)),
            optional("prevAddress"    -> applicantDetails.previousAddress.map(formatAddress)),
            "commDetails" -> jsonObject(
              optional("email"     -> applicantDetails.contact.email),
              optional("telephone" -> applicantDetails.contact.tel)
            ),
            optionalRequiredIf(applicantDetails.personalDetails.exists(_.personalIdentifiers.nonEmpty))(
              "identifiers"           -> applicantDetails.personalDetails.map(_.personalIdentifiers)
            )
          ),
          optional("agentOrCapacitor" -> optTransactorDetails.map { transactorDetails =>
            jsonObject(
              required("individualName" -> transactorDetails.personalDetails.map(details => formatName(details.name))),
              optionalRequiredIf(transactorDetails.isPartOfOrganisation.contains(true))(
                "organisationName"      -> transactorDetails.organisationName
              ),
              "commDetails" -> jsonObject(
                "telephone" -> transactorDetails.telephone,
                "email"     -> transactorDetails.email
              ),
              optionalRequiredIf(transactorDetails.personalDetails.exists(_.arn.isEmpty))(
                "address"               -> transactorDetails.address.map(formatAddress)
              ),
              optionalRequiredIf(transactorDetails.personalDetails.exists(_.personalIdentifiers.nonEmpty))(
                "identification"        -> transactorDetails.personalDetails.map(
                  _.personalIdentifiers.map(identifier => Json.toJson(identifier)(CustomerId.transactorWrites))
                )
              )
            )
          })
        )
      case _                                                                 =>
        val appDetailsMissing  = vatScheme.applicantDetails.fold(Option("applicantDetails"))(_ => None)
        val declarationMissing = vatScheme.confirmInformationDeclaration.fold(Option("declaration"))(_ => None)
        val message            = Seq(appDetailsMissing, declarationMissing).flatten.mkString(", ")
        errorLog(
          s"[DeclarationBlockBuilder][buildDeclarationBlock] - Could not construct declaration block because the following are missing: $message"
        )
        throw new InternalServerException(
          s"Could not construct declaration block because the following are missing: $message"
        )
    }

  private def formatName(name: Name): JsObject = jsonObject(
    optional("firstName"  -> name.first),
    optional("middleName" -> name.middle),
    "lastName" -> name.last
  )

  private def formatFormerName(formerName: FormerName): Option[JsObject] =
    formerName.name.map(name => formatName(name) ++ jsonObject(optional("nameChangeDate" -> formerName.change)))

  private def formatAddress(address: Address): JsObject = jsonObject(
    "line1" -> address.line1,
    optional("line2"            -> address.line2),
    optional("line3"            -> address.line3),
    optional("line4"            -> address.line4),
    optional("line5"            -> address.line5),
    optional("postCode"         -> address.postcode),
    optional("countryCode"      -> address.country.flatMap(_.code)),
    optional("addressValidated" -> address.addressValidated)
  )

}
