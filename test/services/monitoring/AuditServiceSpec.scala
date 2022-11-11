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

package services.monitoring

import config.BackendConfig
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.ExecutionContext

class AuditServiceSpec extends VatRegSpec with VatRegistrationFixture {

  val mockAuditConnector: AuditConnector = mock[AuditConnector]
  val mockAppConfig: BackendConfig = mock[BackendConfig]
  val testAppName = "app"
  val testUrl = "testUrl"

  val testAuditService = new AuditService(mockAppConfig, mockAuditConnector)

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("POST", "testUrl")

  val testAuditModel: AuditModel = new AuditModel{
    override val auditType = "testAuditType"
    override val transactionName = "testTransactionName"
    override val detail: JsValue = Json.obj()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditConnector, mockAppConfig)

    when(mockAppConfig.loadConfig("appName")).thenReturn(testAppName)
  }

  "audit" when {
    "given a auditable data type" should {
      "extract the data and pass it into the AuditConnector" in {

        val expectedData = testAuditService.toDataEvent(testAppName, testAuditModel, "testUrl")

        testAuditService.audit(testAuditModel)

        verify(mockAuditConnector)
          .sendExtendedEvent(
            ArgumentMatchers.refEq(expectedData, "eventId", "generatedAt")
          )(
            ArgumentMatchers.any[HeaderCarrier],
            ArgumentMatchers.any[ExecutionContext]
          )
      }
    }
  }
}
