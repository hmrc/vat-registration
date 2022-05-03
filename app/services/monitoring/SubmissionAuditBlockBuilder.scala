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

import models.api.{AttachmentType, VatScheme}
import models.monitoring.SubmissionAuditModel
import services.AttachmentsService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.InternalServerException
import utils.JsonUtils._

import javax.inject.{Inject, Singleton}

@Singleton
class SubmissionAuditBlockBuilder @Inject()(subscriptionBlockBuilder: SubscriptionAuditBlockBuilder,
                                            declarationBlockBuilder: DeclarationAuditBlockBuilder,
                                            complianceBlockBuilder: ComplianceAuditBlockBuilder,
                                            customerIdentificationBlockBuilder: CustomerIdentificationAuditBlockBuilder,
                                            periodsAuditBlockBuilder: PeriodsAuditBlockBuilder,
                                            bankAuditBlockBuilder: BankAuditBlockBuilder,
                                            contactAuditBlockBuilder: ContactAuditBlockBuilder,
                                            annualAccountingAuditBlockBuilder: AnnualAccountingAuditBlockBuilder,
                                            attachmentsService: AttachmentsService,
                                            entitiesAuditBlockBuilder: EntitiesAuditBlockBuilder) {


  def buildAuditJson(vatScheme: VatScheme,
                     authProviderId: String,
                     affinityGroup: AffinityGroup,
                     optAgentReferenceNumber: Option[String],
                     formBundleId: String
                    ): SubmissionAuditModel = {
    val attachmentList = attachmentsService.attachmentList(vatScheme)
    val details = jsonObject(
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
      "businessContact" -> contactAuditBlockBuilder.buildContactBlock(vatScheme),
      "bankDetails" -> bankAuditBlockBuilder.buildBankAuditBlock(vatScheme),
      "periods" -> periodsAuditBlockBuilder.buildPeriodsBlock(vatScheme),
      optional("joinAA" -> annualAccountingAuditBlockBuilder.buildAnnualAccountingAuditBlock(vatScheme)),
      optionalRequiredIf(attachmentList.nonEmpty)("attachments" -> vatScheme.attachments.map(attachments =>
        AttachmentType.submissionWrites(attachments.method).writes(attachmentList)
      )),
      optional("entities" -> entitiesAuditBlockBuilder.buildEntitiesAuditBlock(vatScheme))
    )

    SubmissionAuditModel(
      userAnswers = details,
      vatScheme = vatScheme,
      authProviderId = authProviderId,
      affinityGroup = affinityGroup,
      optAgentReferenceNumber = optAgentReferenceNumber,
      formBundleId = formBundleId
    )
  }

}
