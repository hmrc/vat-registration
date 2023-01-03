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

import config.BackendConfig
import play.api.libs.json.JsValue
import play.api.mvc.Request
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AuditService @Inject()(appConfig: BackendConfig,
                             auditConnector: AuditConnector) {

  private lazy val appName: String = appConfig.loadConfig("appName")

  def audit(dataSource: AuditModel)(implicit hc: HeaderCarrier, ec: ExecutionContext, request: Request[_]): Unit =
    auditConnector.sendExtendedEvent(toDataEvent(appName, dataSource, request.path))

  def toDataEvent(appName: String, auditModel: AuditModel, path: String)(implicit hc: HeaderCarrier): ExtendedDataEvent = {
    val auditType: String = auditModel.auditType
    val transactionName: String = auditModel.transactionName
    val detail: JsValue = auditModel.detail
    val tags: Map[String, String] = Map.empty[String, String]

    ExtendedDataEvent(
      auditSource = appName,
      auditType = auditType,
      tags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags(transactionName, path) ++ tags,
      detail = detail
    )
  }
}

trait AuditModel {
  val auditType: String
  val transactionName: String
  val detail: JsValue
}