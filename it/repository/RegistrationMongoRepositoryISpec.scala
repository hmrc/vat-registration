/*
 * Copyright 2016 HM Revenue & Customs
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

import common.Now
import common.exceptions.InsertFailed
import models.{VatChoice, VatScheme}
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import repositories.{MongoDBProvider, RegistrationMongoRepository}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationMongoRepositoryISpec
  extends UnitSpec with MongoSpecSupport with BeforeAndAfterEach with ScalaFutures with Eventually with WithFakeApplication {

  private val fixedDate = Now(new DateTime(2017, 1, 31, 13, 53))
  private val regId = "AC234321"
  private val vatScheme: VatScheme = VatScheme.blank(regId)(fixedDate)
  private val vatChoice: VatChoice = VatChoice.blank(new DateTime())

  class Setup {
    val repository = new RegistrationMongoRepository(new MongoDBProvider(), "integration-testing")
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  "Calling createNewVatScheme" should {

    "create a new, blank VatScheme with the correct ID" in new Setup {
      val actual = await(repository.createNewVatScheme(regId)(fixedDate))
      actual shouldBe vatScheme
    }

    "throw an InsertFailed exception when creating a new VAT scheme when one already exists" in new Setup {
      await(repository.createNewVatScheme(vatScheme.id)(fixedDate))
      an[InsertFailed] shouldBe thrownBy(await(repository.createNewVatScheme(vatScheme.id)))
    }
  }

  "Calling retrieveVatScheme" should {

    "retrieve a VatScheme object" in new Setup {
      await(repository.insert(vatScheme))
      val actual = await(repository.retrieveVatScheme(vatScheme.id))
      actual shouldBe Some(vatScheme)
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      await(repository.insert(vatScheme))
      await(repository.retrieveVatScheme("NOT_THERE")) shouldBe None
    }
  }


  "Calling updateVatChoice" should {

    "should update to VatChoice success" in new Setup {
      await(repository.insert(vatScheme))
      val result = await(repository.updateVatChoice(regId, vatChoice))
      result shouldBe vatChoice
    }
  }

}
