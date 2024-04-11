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

package repository

import itutil.IntegrationSpecBase
import models.api.{InProgress, PrimaryIdentityEvidence, UpscanDetails}
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext

class UpscanMongoRepositoryISpec extends IntegrationSpecBase {

  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val testRegId = "testRegId"
  val testRegId2 = "testRegId2"
  val testReference1 = "testReference1"
  val testReference2 = "testReference2"
  val testReference3 = "testReference3"
  val testUpscanDetails1: UpscanDetails = UpscanDetails(
    Some(testRegId),
    testReference1,
    Some(PrimaryIdentityEvidence),
    None,
    InProgress,
    None,
    None
  )
  val testUpscanDetails2: UpscanDetails = testUpscanDetails1.copy(reference = testReference2)
  val testUpscanDetails3: UpscanDetails = testUpscanDetails1.copy(registrationId = Some(testRegId2), reference = testReference3)

  "getUpscanDetails" must {
    "return the correct UpscanDetails based on reference" in new SetupHelper {
      await(upscanMongoRepository.collection.insertMany(Seq(testUpscanDetails1, testUpscanDetails2)).toFuture())

      val res: Option[UpscanDetails] = await(upscanMongoRepository.getUpscanDetails(testReference2))

      res mustBe Some(testUpscanDetails2)
    }

    "return None if there is no UpscanDetails with matching reference" in new SetupHelper {
      await(upscanMongoRepository.collection.insertOne(testUpscanDetails1).toFuture())

      val res: Option[UpscanDetails] = await(upscanMongoRepository.getUpscanDetails(testReference2))

      res mustBe None
    }
  }

  "getAllUpscanDetails" must {
    "return a sequence of UpscanDetails based on regId" in new SetupHelper {
      await(upscanMongoRepository.collection.insertMany(Seq(testUpscanDetails1, testUpscanDetails2, testUpscanDetails3)).toFuture())

      val res: Seq[UpscanDetails] = await(upscanMongoRepository.getAllUpscanDetails(testRegId))

      res mustBe Seq(testUpscanDetails1, testUpscanDetails2)
    }

    "return empty list if there is no UpscanDetails with matching regId" in new SetupHelper {
      await(upscanMongoRepository.collection.insertOne(testUpscanDetails3).toFuture())

      val res: Seq[UpscanDetails] = await(upscanMongoRepository.getAllUpscanDetails(testRegId))

      res mustBe Seq()
    }
  }

  "upsertUpscanDetails" must {
    "update UpscanDetails based on reference" in new SetupHelper {
      await(upscanMongoRepository.collection.insertOne(testUpscanDetails1).toFuture())

      val updatedUpscanDetails: UpscanDetails = testUpscanDetails1.copy(downloadUrl = Some("testUrl"))

      await(upscanMongoRepository.upsertUpscanDetails(updatedUpscanDetails))

      val res: Option[UpscanDetails] = await(upscanMongoRepository.getUpscanDetails(testReference1))

      res mustBe Some(updatedUpscanDetails)
    }

    "create a new UpscanDetails record if one doesn't exist" in new SetupHelper {
      await(upscanMongoRepository.upsertUpscanDetails(testUpscanDetails1))

      val res: Option[UpscanDetails] = await(upscanMongoRepository.getUpscanDetails(testReference1))

      res mustBe Some(testUpscanDetails1)
    }
  }
}
