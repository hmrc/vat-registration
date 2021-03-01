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

package services.monitoring

import models.api.VatScheme
import models.monitoring.SubmissionAuditModel
import play.api.libs.json.{JsObject, Json}
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionAuditBlockBuilder @Inject()(subscriptionBlockBuilder: SubscriptionBlockBuilder,
                                            declarationBlockBuilder: DeclarationBlockBuilder,
                                            complianceBlockBuilder: ComplianceBlockBuilder,
                                            customerIdentificationBlockBuilder: CustomerIdentificationBlockBuilder,
                                            periodsAuditBlockBuilder: PeriodsAuditBlockBuilder,
                                            bankAuditBlockBuilder: BankAuditBlockBuilder,
                                            contactAuditBlockBuilder: ContactAuditBlockBuilder)
                                           (implicit ec: ExecutionContext) {


  def buildAuditJson(vatScheme: VatScheme,
                     authProviderId: String,
                     affinityGroup: AffinityGroup,
                     optAgentReferenceNumber: Option[String]
                    )(implicit hc: HeaderCarrier): SubmissionAuditModel = {
    val details = Json.obj(
      "outsideEUSales" -> {
        vatScheme.tradingDetails.map(_.eoriRequested) match {
          case Some(euGoods) => euGoods
          case _ => throw new InternalServerException("Could not construct submission audit JSON due to missing EU goods answer")
        }
      },
      "subscription" -> subscriptionBlockBuilder.buildSubscriptionBlock(vatScheme),
      "declaration" -> declarationBlockBuilder.buildDeclarationBlock(vatScheme),
      "compliance" -> complianceBlockBuilder.buildComplianceBlock(vatScheme),
      "customerIdentification" -> customerIdentificationBlockBuilder.buildCustomerIdentificationBlock(vatScheme),
      "periods" -> periodsAuditBlockBuilder.buildPeriodsBlock(vatScheme),
      "bankDetails" -> bankAuditBlockBuilder.buildBankAuditBlock(vatScheme),
      "businessContact" -> contactAuditBlockBuilder.buildContactBlock(vatScheme)
    )

    SubmissionAuditModel(
      userAnswers = details,
      vatScheme = vatScheme,
      authProviderId = authProviderId,
      affinityGroup = affinityGroup,
      optAgentReferenceNumber = optAgentReferenceNumber
    )
  }

}
