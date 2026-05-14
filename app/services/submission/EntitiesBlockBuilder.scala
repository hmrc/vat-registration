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

import cats.implicits.toTraverseOps
import models._
import models.api._
import models.submission._
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.JsonUtils.{jsonObject, optional, required}
import utils.StringNormaliser

object EntitiesBlockBuilder {

  private val addPartnerAction = "1"

  def buildEntitiesBlock(vatScheme: VatScheme): Either[BuildFailure, Option[JsValue]] = {

    val getBusiness: Either[BuildFailure, Business] =
      vatScheme.business.toRight(BuildFailure("VatScheme.business is empty"))
    val getApplicantDetails: Either[BuildFailure, ApplicantDetails] =
      vatScheme.applicantDetails.toRight(BuildFailure("VatScheme.applicantDetails is empty"))
    val getRegistrationReason: Either[BuildFailure, RegistrationReason] =
      vatScheme.eligibilitySubmissionData
        .map(_.registrationReason)
        .toRight(BuildFailure("VatScheme.eligibilitySubmissionData is empty when fetching registrationReason"))

    for {
      business           <- getBusiness
      applicantDetails   <- getApplicantDetails
      registrationReason <- getRegistrationReason
      entities = getEntities(vatScheme, registrationReason, applicantDetails)
      entitiesBlock <- convertEntitiesListToSingleJson(entities, business, applicantDetails, registrationReason)
    } yield entitiesBlock
  }

  def convertEntitiesListToSingleJson(entities: List[Entity],
                                      business: Business,
                                      applicantDetails: ApplicantDetails,
                                      registrationReason: RegistrationReason): Either[BuildFailure, Option[JsValue]] =
    if (entities.isEmpty) {
      Right(None)
    } else {
      val listOfEntityJsObjects: Either[BuildFailure, List[JsObject]] =
        entities.traverse(buildEntityObject(_, business, applicantDetails, registrationReason))

      listOfEntityJsObjects match {
        case Left(buildFailure)     => Left(buildFailure)
        case Right(listOfJsObjects) => Right(Some(Json.toJson(listOfJsObjects)))
      }
    }

  def buildEntityObject(entity: Entity,
                        business: Business,
                        applicantDetails: ApplicantDetails,
                        registrationReason: RegistrationReason): Either[BuildFailure, JsObject] = {
    val customerInformationEither: Either[BuildFailure, JsObject] = buildCustomerInformation(entity)

    customerInformationEither match {
      case Left(buildFailure) => Left(buildFailure)
      case Right(customerInformation) =>
        Right(
          jsonObject(
            "action" -> addPartnerAction,
            "entityType" -> Json.toJson[EntitiesArrayType](registrationReason match {
              case GroupRegistration => GroupRepMemberEntity
              case _                 => PartnerEntity
            }),
            "tradersPartyType" -> Json.toJson[PartyType](entity.partyType match {
              case NETP      => Individual
              case partyType => partyType
            }),
            "customerIdentification" -> customerInformation,
            "businessContactDetails" -> buildBusinessContactDetails(entity, business, applicantDetails, registrationReason)
          ))
    }
  }

  private def getEntities(vatScheme: VatScheme, registrationReason: RegistrationReason, applicantDetails: ApplicantDetails): List[Entity] =
    vatScheme.entities match {
      case Some(entityList) => entityList.filter(entity => entity.details.isDefined)
      case None if registrationReason.equals(GroupRegistration) =>
        List(
          Entity(
            details = applicantDetails.entity,
            partyType = UkCompany,
            isLeadPartner = Some(true),
            address = None,
            email = None,
            telephoneNumber = None
          )
        )
      case None => List.empty[Entity]
    }

  private def buildCustomerInformation(entity: Entity): Either[BuildFailure, JsObject] =
    entity.details match {
      case Some(details) if details.bpSafeId.isDefined =>
        Right(jsonObject("primeBPSafeID" -> details.bpSafeId))
      case Some(details) =>
        val customerId: Either[BuildFailure, JsObject] = Right(jsonObject("customerID" -> Json.toJson(details.identifiers)))
        val nameDetails: Either[BuildFailure, JsObject] = details match {
          case SoleTraderIdEntity(firstName, lastName, dateOfBirth, _, _, _, _, _, _, _, _) =>
            Right(
              jsonObject(
                "name" -> jsonObject(
                  "firstName" -> firstName,
                  "lastName"  -> lastName
                ),
                "dateOfBirth" -> dateOfBirth
              ))
          case IncorporatedEntity(companyName, _, _, _, _, _, _, _, _, _)  => buildOrganisationNameJson(companyName)
          case MinorEntity(companyName, _, _, _, _, _, _, _, _, _, _)      => buildOrganisationNameJson(companyName)
          case PartnershipIdEntity(_, _, companyName, _, _, _, _, _, _, _) => buildOrganisationNameJson(companyName)
        }
        customerId ++ nameDetails
    }

  private def buildBusinessContactDetails(entity: Entity,
                                          business: Business,
                                          applicantDetails: ApplicantDetails,
                                          registrationReason: RegistrationReason): JsObject =
    (entity.isLeadPartner, registrationReason) match {
      case (Some(true), GroupRegistration) =>
        jsonObject(
          "address" -> business.ppobAddress.map(buildAddressJson),
          "commDetails" -> jsonObject(
            optional("telephone" -> applicantDetails.contact.tel),
            optional("email"     -> applicantDetails.contact.email)
          )
        )
      case (Some(false), _) =>
        jsonObject(
          "address" -> business.ppobAddress.map(buildAddressJson),
          "commDetails" -> jsonObject(
            required("telephone" -> business.telephoneNumber),
            required("email"     -> business.email)
          )
        )
      case _ =>
        jsonObject(
          "address" -> entity.address.map(buildAddressJson),
          "commDetails" -> jsonObject(
            optional("telephone" -> entity.telephoneNumber),
            optional("email"     -> entity.email)
          )
        )
    }

  private def buildAddressJson(address: Address): JsObject = jsonObject(
    "line1" -> address.line1,
    optional("line2"       -> address.line2),
    optional("line3"       -> address.line3),
    optional("line4"       -> address.line4),
    optional("line5"       -> address.line5),
    optional("postCode"    -> address.postcode),
    optional("countryCode" -> address.country.flatMap(_.code))
  )

  private def buildOrganisationNameJson(orgName: Option[String]): Either[BuildFailure, JsObject] =
    orgName.fold {
      Left(BuildFailure("Company name is missing for a partyType that requires it"))
    } { organisationName =>
      val formattedOrganisationName = StringNormaliser.normaliseString(organisationName)
      Right(
        jsonObject(
          "shortOrgName"     -> formattedOrganisationName, // IncorporatedEntity / MinorEntity / PartnershipIdEntity never contain shortOrgName
          "organisationName" -> formattedOrganisationName
        ))
    }

}
