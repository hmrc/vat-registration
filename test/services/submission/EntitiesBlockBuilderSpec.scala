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

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.GroupRegistration
import models.api._
import models.submission._
import play.api.libs.json.Json
import play.api.mvc.Request
import play.api.test.FakeRequest

class EntitiesBlockBuilderSpec extends VatRegSpec with VatRegistrationFixture {

  object Builder extends EntitiesBlockBuilder

  implicit val request: Request[_] = FakeRequest()

  val testEntity = testSoleTraderEntity.copy(bpSafeId = Some(testBpSafeId))
  val testEntityNoSafeId = testSoleTraderEntity.copy(bpSafeId = None)
  val testPartner = Entity(
    details = Some(testEntity),
    partyType = Individual,
    isLeadPartner = Some(true),
    address = None,
    email = None,
    telephoneNumber = None
  )
  val testApplicantContact = Contact(
    email = Some(testEmail),
    tel = Some(testTelephone)
  )

  "buildEntitiesBlock" when {
    "the partner was successfully matched by the identity service" should {
      "return a JSON array containing a lead partner with a business partner safe ID" in {
        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = Some(testEligibilitySubmissionData),
          business = Some(testBusiness),
          entities = Some(List(testPartner)),
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
              "postCode" -> "ZZ1 1ZZ",
              "countryCode" -> "GB"
            ),
            "commDetails" -> Json.obj(
              "telephone" -> testTelephone,
              "email" -> testEmail
            )
          )
        )))
      }
      "return a JSON array containing a non-lead partner with a business contact details" in {
        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = Some(testEligibilitySubmissionData),
          business = Some(testBusiness.copy(ppobAddress = None, email = None, telephoneNumber = None)),
          entities = Some(List(testPartner.copy(
            isLeadPartner = Some(false),
            address = Some(testAddress),
            email = Some(testEmail),
            telephoneNumber = Some(testTelephone)))),
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
              "postCode" -> "ZZ1 1ZZ",
              "countryCode" -> "GB"
            ),
            "commDetails" -> Json.obj(
              "telephone" -> testTelephone,
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
            business = Some(testBusiness),
            entities = Some(List(testPartner.copy(details = Some(testEntityNoSafeId)))),
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
                  "postCode" -> "ZZ1 1ZZ",
                  "countryCode" -> "GB"
                ),
                "commDetails" -> Json.obj(
                  "telephone" -> testTelephone,
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
          val testPartner = Entity(
            details = Some(testEntity),
            partyType = Individual,
            isLeadPartner = Some(true),
            address = None,
            email = None,
            telephoneNumber = None
          )
          val vatScheme = testVatScheme.copy(
            eligibilitySubmissionData = Some(testEligibilitySubmissionData),
            business = Some(testBusiness),
            entities = Some(List(testPartner)),
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
                  "postCode" -> "ZZ1 1ZZ",
                  "countryCode" -> "GB"
                ),
                "commDetails" -> Json.obj(
                  "telephone" -> testTelephone,
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
          business = Some(testBusiness),
          entities = None,
          applicantDetails = Some(validApplicantDetails)
        )

        Builder.buildEntitiesBlock(vatScheme) mustBe None
      }
    }
    "there is no partner list but the user is registering a vat group" should {
      "return a JSON array containing a single entity based on the applicants business entity without safeId" in {
        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(registrationReason = GroupRegistration)),
          business = Some(testBusiness),
          entities = None,
          applicantDetails = Some(validApplicantDetails.copy(contact = testApplicantContact))
        )

        Builder.buildEntitiesBlock(vatScheme) mustBe Some(Json.arr(Json.obj(
          "action" -> "1",
          "entityType" -> Json.toJson[EntitiesArrayType](GroupRepMemberEntity),
          "tradersPartyType" -> Json.toJson[PartyType](UkCompany),
          "customerIdentification" -> Json.obj(
            "customerID" -> Json.toJson(testLtdCoEntity.identifiers),
            "shortOrgName" -> testCompanyName,
            "organisationName" -> testCompanyName
          ),
          "businessContactDetails" -> Json.obj(
            "address" -> Json.obj(
              "line1" -> "line1",
              "line2" -> "line2",
              "postCode" -> "ZZ1 1ZZ",
              "countryCode" -> "GB"
            ),
            "commDetails" -> Json.obj(
              "telephone" -> testTelephone,
              "email" -> testEmail
            )
          )
        )))
      }

      "return a JSON array containing a single entity based on the applicants business entity with safeId" in {
        val vatScheme = testVatScheme.copy(
          eligibilitySubmissionData = Some(testEligibilitySubmissionData.copy(registrationReason = GroupRegistration)),
          business = Some(testBusiness),
          entities = None,
          applicantDetails = Some(validApplicantDetails.copy(
            entity = Some(testLtdCoEntity.copy(bpSafeId = Some(testBpSafeId))),
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
              "postCode" -> "ZZ1 1ZZ",
              "countryCode" -> "GB"
            ),
            "commDetails" -> Json.obj(
              "telephone" -> testTelephone,
              "email" -> testEmail
            )
          )
        )))
      }
    }
  }
}
