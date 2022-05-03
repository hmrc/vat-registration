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

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.GroupRegistration
import models.api._
import models.registration.sections.PartnersSection
import models.submission._
import play.api.libs.json.Json

class EntitiesBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  object Builder extends EntitiesBlockBuilder

  val testPhone = "01234 567890"
  val testContact = DigitalContact(
    email = testEmail,
    tel = Some(testPhone),
    mobile = Some(testPhone)
  )
  val businessContact = BusinessContact(
    digitalContact = testContact,
    website = None,
    ppob = testAddress,
    commsPreference = Email
  )
  val testEntity = testSoleTraderEntity.copy(bpSafeId = Some(testBpSafeId))
  val testEntityNoSafeId = testSoleTraderEntity.copy(bpSafeId = None)
  val testPartner = Partner(details = testEntity, partyType = Individual, isLeadPartner = true)
  val testApplicantContact = DigitalContactOptional(
    email = Some(testEmail),
    tel = Some(testPhone),
    mobile = Some(testPhone)
  )

  "buildEntitiesBlock" when {
    "the partner was successfully matched by the identity service" should {
      "return a JSON array containing a single partner with a business partner safe ID" in {
        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = Some(testEligibilitySubmissionData),
          businessContact = Some(businessContact),
          partners = Some(PartnersSection(List(testPartner))),
          applicantDetails = Some(validApplicantDetails)
        )

        Builder.buildEntitiesBlock(vatScheme) mustBe Some(Json.arr(Json.obj(
          "action" -> "1",
          "entityType" -> Json.toJson[EntitiesArrayType](PartnerEntity),
          "tradersPartyType" -> Json.toJson[PartyType](Individual),
          "customerIdentification" -> Json.obj(
            "primeBPSafeID" -> testBpSafeId
          ),
          "businessContactDetails" -> Json.obj(
            "address" -> Json.obj(
              "line1" -> "line1",
              "line2" -> "line2",
              "postCode" -> "XX XX",
              "countryCode" -> "GB"
            ),
            "commDetails" -> Json.obj(
              "mobileNumber" -> testPhone,
              "telephone" -> testPhone,
              "email" -> testEmail
            )
          )
        )))
      }
    }
    "the partner was not matched" when {
      "an SA UTR was provided" should {
        "return a JSON array containing a single partner with a list of identifiers" in {
          val vatScheme = testVatScheme.copy(
            eligibilitySubmissionData = Some(testEligibilitySubmissionData),
            businessContact = Some(businessContact),
            partners = Some(PartnersSection(List(testPartner.copy(details = testEntityNoSafeId)))),
            applicantDetails = Some(validApplicantDetails)
          )

          Builder.buildEntitiesBlock(vatScheme) mustBe Some(Json.arr(
            Json.obj(
              "action" -> "1",
              "entityType" -> Json.toJson[EntitiesArrayType](PartnerEntity),
              "tradersPartyType" -> Json.toJson[PartyType](Individual),
              "customerIdentification" -> Json.obj(
                "customerID" -> Json.toJson(testEntity.identifiers),
                "name" -> Json.obj(
                  "firstName" -> testFirstName,
                  "lastName" -> testLastName
                ),
                "dateOfBirth" -> testDate
              ),
              "businessContactDetails" -> Json.obj(
                "address" -> Json.obj(
                  "line1" -> "line1",
                  "line2" -> "line2",
                  "postCode" -> "XX XX",
                  "countryCode" -> "GB"
                ),
                "commDetails" -> Json.obj(
                  "mobileNumber" -> testPhone,
                  "telephone" -> testPhone,
                  "email" -> testEmail
                )
              )
            )
          ))
        }
      }
      "an SA UTR was not provided" should {
        "return a JSON array containing a single partner without identifiers" in {
          val testEntity = testSoleTraderEntity.copy(bpSafeId = None, sautr = None)
          val testPartner = Partner(details = testEntity, partyType = Individual, isLeadPartner = true)
          val vatScheme = testVatScheme.copy(
            eligibilitySubmissionData = Some(testEligibilitySubmissionData),
            businessContact = Some(businessContact),
            partners = Some(PartnersSection(List(testPartner))),
            applicantDetails = Some(validApplicantDetails)
          )

          Builder.buildEntitiesBlock(vatScheme) mustBe Some(Json.arr(
            Json.obj(
              "action" -> "1",
              "entityType" -> Json.toJson[EntitiesArrayType](PartnerEntity),
              "tradersPartyType" -> Json.toJson[PartyType](Individual),
              "customerIdentification" -> Json.obj(
                "customerID" -> Json.toJson(testEntity.identifiers),
                "name" -> Json.obj(
                  "firstName" -> testFirstName,
                  "lastName" -> testLastName
                ),
                "dateOfBirth" -> testDate
              ),
              "businessContactDetails" -> Json.obj(
                "address" -> Json.obj(
                  "line1" -> "line1",
                  "line2" -> "line2",
                  "postCode" -> "XX XX",
                  "countryCode" -> "GB"
                ),
                "commDetails" -> Json.obj(
                  "mobileNumber" -> testPhone,
                  "telephone" -> testPhone,
                  "email" -> testEmail
                )
              )
            )
          ))
        }
      }
    }
    "there are no partner details" should {
      "return None" in {
        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = Some(testEligibilitySubmissionData),
          businessContact = Some(businessContact),
          partners = None,
          applicantDetails = Some(validApplicantDetails)
        )

        Builder.buildEntitiesBlock(vatScheme) mustBe None
      }
    }
    "there is no telephone number" should {
      "return the correct JSON without the phone number" in {
        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = Some(testEligibilitySubmissionData),
          businessContact = Some(businessContact.copy(digitalContact = testContact.copy(tel = None))),
          partners = Some(PartnersSection(List(testPartner))),
          applicantDetails = Some(validApplicantDetails)
        )

        Builder.buildEntitiesBlock(vatScheme) mustBe Some(Json.arr(
          Json.obj(
            "action" -> "1",
            "entityType" -> Json.toJson[EntitiesArrayType](PartnerEntity),
            "tradersPartyType" -> Json.toJson[PartyType](Individual),
            "customerIdentification" -> Json.obj(
              "primeBPSafeID" -> testBpSafeId
            ),
            "businessContactDetails" -> Json.obj(
              "address" -> Json.obj(
                "line1" -> "line1",
                "line2" -> "line2",
                "postCode" -> "XX XX",
                "countryCode" -> "GB"
              ),
              "commDetails" -> Json.obj(
                "mobileNumber" -> testPhone,
                "email" -> testEmail
              )
            )
          )
        ))
      }
    }

    "there is no partner list but the user is registering a vat group" should {
      "return a JSON array containing a single entity based on the applicants business entity without safeId" in {
        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(registrationReason = GroupRegistration)),
          businessContact = Some(businessContact),
          partners = None,
          applicantDetails = Some(validApplicantDetails.copy(contact = testApplicantContact))
        )

        Builder.buildEntitiesBlock(vatScheme) mustBe Some(Json.arr(Json.obj(
          "action" -> "1",
          "entityType" -> Json.toJson[EntitiesArrayType](GroupRepMemberEntity),
          "tradersPartyType" -> Json.toJson[PartyType](UkCompany),
          "customerIdentification" -> Json.obj(
            "customerID" -> Json.toJson(testLtdCoEntity.identifiers),
            "shortOrgName" -> testCompanyName
          ),
          "businessContactDetails" -> Json.obj(
            "address" -> Json.obj(
              "line1" -> "line1",
              "line2" -> "line2",
              "postCode" -> "XX XX",
              "countryCode" -> "GB"
            ),
            "commDetails" -> Json.obj(
              "mobileNumber" -> testPhone,
              "telephone" -> testPhone,
              "email" -> testEmail
            )
          )
        )))
      }

      "return a JSON array containing a single entity based on the applicants business entity with safeId" in {
        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(registrationReason = GroupRegistration)),
          businessContact = Some(businessContact),
          partners = None,
          applicantDetails = Some(validApplicantDetails.copy(
            entity = testLtdCoEntity.copy(bpSafeId = Some(testBpSafeId)),
            contact = testApplicantContact
          ))
        )

        Builder.buildEntitiesBlock(vatScheme) mustBe Some(Json.arr(Json.obj(
          "action" -> "1",
          "entityType" -> Json.toJson[EntitiesArrayType](GroupRepMemberEntity),
          "tradersPartyType" -> Json.toJson[PartyType](UkCompany),
          "customerIdentification" -> Json.obj(
            "primeBPSafeID" -> testBpSafeId
          ),
          "businessContactDetails" -> Json.obj(
            "address" -> Json.obj(
              "line1" -> "line1",
              "line2" -> "line2",
              "postCode" -> "XX XX",
              "countryCode" -> "GB"
            ),
            "commDetails" -> Json.obj(
              "mobileNumber" -> testPhone,
              "telephone" -> testPhone,
              "email" -> testEmail
            )
          )
        )))
      }
    }
  }
}
