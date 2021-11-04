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

package models.api

import models._
import models.submission.PartyType
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.http.InternalServerException

case class EligibilitySubmissionData(threshold: Threshold,
                                     exceptionOrExemption: String,
                                     estimates: TurnoverEstimates,
                                     customerStatus: CustomerStatus,
                                     partyType: PartyType,
                                     registrationReason: RegistrationReason,
                                     isTransactor: Boolean)

object EligibilitySubmissionData {
  val exceptionKey = "2"
  val exemptionKey = "1"
  val nonExceptionOrExemptionKey = "0"

  val sellingGoodsAndServices = "selling-goods-and-services"
  val takingOverBusiness = "taking-over-business"
  val changingLegalEntityOfBusiness = "changing-legal-entity"
  val settingUpVatGroup = "setting-up-vat-group"
  val ukEstablishedOverseasExporter = "overseas-exporter"

  val registeringOwnBusiness = "own"
  val registeringSomeoneElse = "someone-else"

  val eligibilityReads: Reads[EligibilitySubmissionData] = Reads { json =>
    (
      json.validate[Threshold](Threshold.eligibilityDataJsonReads) and
        (
          (json \ "vatRegistrationException").validateOpt[Boolean] and
            (json \ "vatExemption").validateOpt[Boolean]
          ) ((exception, exemption) =>
          (exception.contains(true), exemption.contains(true)) match {
            case (excepted, exempt) if !excepted && !exempt => nonExceptionOrExemptionKey
            case (excepted, _) if excepted => exceptionKey
            case (_, exempt) if exempt => exemptionKey
            case (_, _) =>
              throw new InternalServerException("[EligibilitySubmissionData][eligibilityReads] eligibility returned invalid exception/exemption data")
          }
        ) and
        json.validate[TurnoverEstimates](TurnoverEstimates.eligibilityDataJsonReads) and
        json.validate[CustomerStatus](CustomerStatus.eligibilityDataJsonReads) and
        (json \ "businessEntity-value").validate[PartyType] and
        (json \ "registrationReason-value").validateOpt[String] and
        (json \ "registeringBusiness-value").validate[String]
      ) ((threshold, exceptionOrException, turnoverEstimates, customerStatus, businessEntity, registrationReason, registeringBusiness) =>
      EligibilitySubmissionData(
        threshold,
        exceptionOrException,
        turnoverEstimates,
        customerStatus,
        businessEntity,
        registrationReason match {
          case Some(`sellingGoodsAndServices`) | None => threshold match {
            case Threshold(true, _, _, _, Some(thresholdOverseas)) =>
              NonUk
            case Threshold(false, _, _, _, _) =>
              Voluntary
            case Threshold(true, forwardLook1, _, forwardLook2, _)
              if forwardLook1.contains(threshold.earliestDate) || forwardLook2.contains(threshold.earliestDate) =>
              ForwardLook
            case _ =>
              BackwardLook
          }
          case Some(`takingOverBusiness`) | Some(`changingLegalEntityOfBusiness`) => TransferOfAGoingConcern
          case Some(`settingUpVatGroup`) => GroupRegistration
          case Some(`ukEstablishedOverseasExporter`) => SuppliesOutsideUk
        },
        registeringBusiness match {
          case `registeringOwnBusiness` => false
          case `registeringSomeoneElse` => true
        }
      )
    )
  }

  implicit val format: Format[EligibilitySubmissionData] = (
    (__ \ "threshold").format[Threshold] and
      (__ \ "exceptionOrExemption").format[String] and
      (__ \ "estimates").format[TurnoverEstimates] and
      (__ \ "customerStatus").format[CustomerStatus] and
      (__ \ "partyType").format[PartyType] and
      (__ \ "registrationReason").format[RegistrationReason] and
      (__ \ "isTransactor").formatWithDefault[Boolean](false)
    ) (EligibilitySubmissionData.apply, unlift(EligibilitySubmissionData.unapply))

}