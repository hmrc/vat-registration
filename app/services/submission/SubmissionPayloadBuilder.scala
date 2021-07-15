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

import play.api.libs.json.JsObject
import utils.JsonUtils._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubmissionPayloadBuilder @Inject()(adminBlockBuilder: AdminBlockBuilder,
                                         declarationBlockBuilder: DeclarationBlockBuilder,
                                         customerIdentificationBlockBuilder: CustomerIdentificationBlockBuilder,
                                         contactBlockBuilder: ContactBlockBuilder,
                                         periodsBlockBuilder: PeriodsBlockBuilder,
                                         subscriptionBlockBuilder: SubscriptionBlockBuilder,
                                         bankDetailsBlockBuilder: BankDetailsBlockBuilder,
                                         complianceBlockBuilder: ComplianceBlockBuilder,
                                         annualAccountingBlockBuilder: AnnualAccountingBlockBuilder,
                                         entitiesBlockBuilder: EntitiesBlockBuilder
                                        )(implicit ec: ExecutionContext) {

  def buildSubmissionPayload(regId: String): Future[JsObject] = for {
    adminBlock <- adminBlockBuilder.buildAdminBlock(regId)
    declarationBlock <- declarationBlockBuilder.buildDeclarationBlock(regId)
    customerIdentificationBlock <- customerIdentificationBlockBuilder.buildCustomerIdentificationBlock(regId)
    contactBlock <- contactBlockBuilder.buildContactBlock(regId)
    subscriptionBlock <- subscriptionBlockBuilder.buildSubscriptionBlock(regId)
    periodsBlock <- periodsBlockBuilder.buildPeriodsBlock(regId)
    complianceBlock <- complianceBlockBuilder.buildComplianceBlock(regId)
    bankDetailsBlock <- bankDetailsBlockBuilder.buildBankDetailsBlock(regId)
    annualAccountingBlockBuilder <- annualAccountingBlockBuilder.buildAnnualAccountingBlock(regId)
    entities <- entitiesBlockBuilder.buildEntitiesBlock(regId)
  } yield jsonObject(
    "messageType" -> "SubscriptionCreate",
    "admin" -> adminBlock,
    "declaration" -> declarationBlock,
    "customerIdentification" -> customerIdentificationBlock,
    "contact" -> contactBlock,
    "subscription" -> subscriptionBlock,
    "periods" -> periodsBlock,
    "bankDetails" -> bankDetailsBlock,
    optional("joinAA" -> annualAccountingBlockBuilder),
    optional("compliance" -> complianceBlock),
    optional("entities" -> entities)
  )
}