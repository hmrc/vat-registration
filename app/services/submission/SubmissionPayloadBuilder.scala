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

import models.api.VatScheme
import play.api.libs.json.JsObject
import play.api.mvc.Request
import utils.JsonUtils._

import javax.inject.{Inject, Singleton}

@Singleton
class SubmissionPayloadBuilder @Inject() (
  adminBlockBuilder: AdminBlockBuilder,
  declarationBlockBuilder: DeclarationBlockBuilder,
  customerIdentificationBlockBuilder: CustomerIdentificationBlockBuilder,
  contactBlockBuilder: ContactBlockBuilder,
  periodsBlockBuilder: PeriodsBlockBuilder,
  subscriptionBlockBuilder: SubscriptionBlockBuilder,
  bankDetailsBlockBuilder: BankDetailsBlockBuilder,
  complianceBlockBuilder: ComplianceBlockBuilder,
  annualAccountingBlockBuilder: AnnualAccountingBlockBuilder,
  entitiesBlockBuilder: EntitiesBlockBuilder
) {

  def buildSubmissionPayload(vatScheme: VatScheme)(implicit request: Request[_]): JsObject =
    jsonObject(
      "messageType"            -> "SubscriptionCreate",
      "admin"                  -> adminBlockBuilder.buildAdminBlock(vatScheme),
      "declaration"            -> declarationBlockBuilder.buildDeclarationBlock(vatScheme),
      "customerIdentification" -> customerIdentificationBlockBuilder.buildCustomerIdentificationBlock(vatScheme),
      "contact"                -> contactBlockBuilder.buildContactBlock(vatScheme),
      "subscription"           -> subscriptionBlockBuilder.buildSubscriptionBlock(vatScheme),
      "periods"                -> periodsBlockBuilder.buildPeriodsBlock(vatScheme),
      optional("bankDetails" -> bankDetailsBlockBuilder.buildBankDetailsBlock(vatScheme)),
      optional("joinAA"      -> annualAccountingBlockBuilder.buildAnnualAccountingBlock(vatScheme)),
      optional("compliance"  -> complianceBlockBuilder.buildComplianceBlock(vatScheme)),
      optional("entities"    -> entitiesBlockBuilder.buildEntitiesBlock(vatScheme))
    )
}
