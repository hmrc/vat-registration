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

package services

import helpers.VatRegSpec
import mocks.MockUpscanMongoRepository
import models.api.{InProgress, PrimaryIdentityEvidence, UpscanCreate, UpscanDetails}
import play.api.test.Helpers._

import scala.concurrent.Future

class UpscanServiceSpec extends VatRegSpec with MockUpscanMongoRepository {

  val testReference = "testReference"
  val testUpscanDetails: UpscanDetails = UpscanDetails(
    Some(testRegId),
    testReference,
    Some(PrimaryIdentityEvidence),
    None,
    InProgress,
    None,
    None
  )

  object TestService extends UpscanService(mockUpscanMongoRepository)

  "getUpscanDetails" must {
    "return UpscanDetails if they are found" in {
      mockGetUpscanDetails(testReference)(Future.successful(Some(testUpscanDetails)))

      val res = await(TestService.getUpscanDetails(testReference))

      res mustBe Some(testUpscanDetails)
    }
    "return None if no details were found" in {
      mockGetUpscanDetails(testReference)(Future.successful(None))

      val res = await(TestService.getUpscanDetails(testReference))

      res mustBe None
    }
  }

  "getAllUpscanDetails" must {
    "return a list of UpscanDetails if they are found" in {
      mockGetAllUpscanDetails(testRegId)(Future.successful(Seq(testUpscanDetails)))

      val res = await(TestService.getAllUpscanDetails(testRegId))

      res mustBe Seq(testUpscanDetails)
    }
    "return an empty list if no details were found" in {
      mockGetAllUpscanDetails(testRegId)(Future.successful(Seq()))

      val res = await(TestService.getAllUpscanDetails(testRegId))

      res mustBe Seq()
    }
  }

  "createUpscanDetails" must {
    "return the inserted object" in {
      mockUpsertUpscanDetails(testUpscanDetails)(Future.successful(testUpscanDetails))

      val res = await(TestService.createUpscanDetails(testRegId, UpscanCreate(testReference, PrimaryIdentityEvidence)))

      res mustBe testUpscanDetails
    }
  }

  "upsertUpscanDetails" must {
    "return the inserted object" in {
      mockUpsertUpscanDetails(testUpscanDetails)(Future.successful(testUpscanDetails))

      val res = await(TestService.upsertUpscanDetails(testUpscanDetails))

      res mustBe testUpscanDetails
    }
  }

  "deleteUpscanDetails" must {
    "return a boolean after deleting" in {
      mockDeleteUpscanDetails(testReference)(Future.successful(true))

      val res = await(TestService.deleteUpscanDetails(testReference))

      res mustBe true
    }
  }

  "deleteAllUpscanDetails" must {
    "return a boolean after deleting" in {
      mockDeleteAllUpscanDetails(testRegId)(Future.successful(true))

      val res = await(TestService.deleteAllUpscanDetails(testRegId))

      res mustBe true
    }
  }
}
