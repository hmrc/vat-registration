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

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.{BusinessContact, DigitalContact, Email, Partner}
import models.submission.{EntitiesArrayType, Individual, PartnerEntity, PartyType}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.Json
import play.api.test.Helpers._
import services.submission.buildermocks.MockPartnersService

import scala.concurrent.Future

class EntitiesBlockBuilderSpec extends VatRegSpec with MockPartnersService with VatRegistrationFixture {

  object Builder extends EntitiesBlockBuilder(
    mockPartnersService,
    mockBusinessContactService
  )

  private def mockGetBusinessContact(regId: String)(response: Future[Option[BusinessContact]]): OngoingStubbing[Future[Option[BusinessContact]]] =
    when(mockBusinessContactService.getBusinessContact(
      ArgumentMatchers.eq(regId)
    )).thenReturn(response)


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

  "buildEntitiesBlock" when {
    "the partner was successfully matched by the identity service" should {
      "return a JSON array containing a single partner with a business partner safe ID" in {
        mockGetPartners(testRegId)(Future.successful(Some(List(testPartner))))
        mockGetBusinessContact(testRegId)(Future.successful(Some(businessContact)))

        await(Builder.buildEntitiesBlock(testRegId)) mustBe Some(Json.arr(Json.obj(
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
              "telephone" -> testPhone
            )
          )
        )))
      }
    }
    "the partner was not matched" when {
      "an SA UTR was provided" should {
        "return a JSON array containing a single partner with a list of identifiers" in {
          mockGetPartners(testRegId)(Future.successful(Some(List(testPartner.copy(details = testEntityNoSafeId)))))
          mockGetBusinessContact(testRegId)(Future.successful(Some(businessContact)))

          await(Builder.buildEntitiesBlock(testRegId)) mustBe Some(Json.arr(
            Json.obj(
              "action" -> "1",
              "entityType" -> Json.toJson[EntitiesArrayType](PartnerEntity),
              "tradersPartyType" -> Json.toJson[PartyType](Individual),
              "customerIdentification" -> Json.obj(
                "customerID" -> Json.toJson(testEntity.identifiers)
              ),
              "businessContactDetails" -> Json.obj(
                "address" -> Json.obj(
                  "line1" -> "line1",
                  "line2" -> "line2",
                  "postCode" -> "XX XX",
                  "countryCode" -> "GB"
                ),
                "commDetails" -> Json.obj(
                  "telephone" -> testPhone
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
          mockGetPartners(testRegId)(Future.successful(Some(List(testPartner))))
          mockGetBusinessContact(testRegId)(Future.successful(Some(businessContact)))

          await(Builder.buildEntitiesBlock(testRegId)) mustBe Some(Json.arr(
            Json.obj(
              "action" -> "1",
              "entityType" -> Json.toJson[EntitiesArrayType](PartnerEntity),
              "tradersPartyType" -> Json.toJson[PartyType](Individual),
              "customerIdentification" -> Json.obj(
                "customerID" -> Json.toJson(testEntity.identifiers)
              ),
              "businessContactDetails" -> Json.obj(
                "address" -> Json.obj(
                  "line1" -> "line1",
                  "line2" -> "line2",
                  "postCode" -> "XX XX",
                  "countryCode" -> "GB"
                ),
                "commDetails" -> Json.obj(
                  "telephone" -> testPhone
                )
              )
            )
          ))
        }
      }
    }
    "there are no partner details" should {
      "return None" in {
        mockGetPartners(testRegId)(Future.successful(None))
        mockGetBusinessContact(testRegId)(Future.successful(Some(businessContact)))

        await(Builder.buildEntitiesBlock(testRegId)) mustBe None
      }
    }
    "there is no telephone number" should {
      "return the correct JSON without the phone number" in {
        mockGetPartners(testRegId)(Future.successful(Some(List(testPartner))))
        mockGetBusinessContact(testRegId)(Future.successful(Some(businessContact.copy(digitalContact = testContact.copy(tel = None)))))

        await(Builder.buildEntitiesBlock(testRegId)) mustBe Some(Json.arr(
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
              )
            )
          )
        ))
      }
    }
  }
}
