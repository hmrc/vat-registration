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

package models.api

import models._
import models.submission.{NETP, NonUkNonEstablished, PartyType}
import play.api.libs.functional.syntax._
import play.api.libs.json._

import java.time.LocalDate

case class EligibilitySubmissionData(
  threshold: Threshold,
  appliedForException: Option[Boolean],
  partyType: PartyType,
  registrationReason: RegistrationReason,
  togcCole: Option[TogcCole] = None,
  isTransactor: Boolean,
  calculatedDate: Option[LocalDate] = None,
  fixedEstablishmentInManOrUk: Boolean
)

object EligibilitySubmissionData {
  val sellingGoodsAndServices       = "selling-goods-and-services"
  val takingOverBusiness            = "taking-over-business"
  val changingLegalEntityOfBusiness = "changing-legal-entity"
  val settingUpVatGroup             = "setting-up-vat-group"
  val ukEstablishedOverseasExporter = "overseas-exporter"

  val registeringOwnBusiness = "own"
  val registeringSomeoneElse = "someone-else"

  val eligibilityReads: Reads[EligibilitySubmissionData] = Reads { json =>
    (
      json.validate[Threshold](Threshold.eligibilityDataJsonReads) and
        (json \ "vatRegistrationException").validateOpt[Boolean] and
        (json \ "businessEntity").validate[PartyType] and
        (json \ "registrationReason").validate[String] and
        (json \ "registeringBusiness").validate[String] and
        json.validateOpt[TogcCole](TogcCole.eligibilityDataJsonReads).orElse(JsSuccess(None)) and
        (json \ "fixedEstablishment").validate[Boolean]
    )(
      (
        threshold,
        exception,
        businessEntity,
        registrationReason,
        registeringBusiness,
        optTogcCole,
        fixedEstablishmentInManOrUk
      ) =>
        EligibilitySubmissionData(
          threshold,
          exception,
          businessEntity,
          registrationReason match {
            case `sellingGoodsAndServices`                              =>
              threshold match {
                case Threshold(true, _, _, _, Some(_)) =>
                  NonUk
                case Threshold(false, _, _, _, _)      =>
                  Voluntary
                case Threshold(true, forwardLook1, _, forwardLook2, _)
                    if forwardLook1.contains(threshold.earliestDate) || forwardLook2.contains(threshold.earliestDate) =>
                  ForwardLook
                case _                                 =>
                  BackwardLook
              }
            case `takingOverBusiness` | `changingLegalEntityOfBusiness` => TransferOfAGoingConcern
            case `settingUpVatGroup`                                    => GroupRegistration
            case `ukEstablishedOverseasExporter`                        => SuppliesOutsideUk
          },
          optTogcCole,
          registeringBusiness match {
            case `registeringOwnBusiness` => false
            case `registeringSomeoneElse` => true
          },
          (threshold, optTogcCole) match {
            case (_, Some(TogcCole(dateOfTransfer, _, _, _, _)))        =>
              Some(dateOfTransfer)
            case (Threshold(true, _, _, _, Some(thresholdOverseas)), _) =>
              Some(thresholdOverseas)
            case (Threshold(true, optPrevThirtyDays, optNextTwelveMonths, optNextThirtyDays, _), _)
                if List(optPrevThirtyDays, optNextTwelveMonths, optNextThirtyDays).flatten.nonEmpty =>
              Some(threshold.earliestDate)
            case _                                                      =>
              None
          },
          fixedEstablishmentInManOrUk
        )
    )
  }

  implicit val format: Format[EligibilitySubmissionData] = (
    (__ \ "threshold").format[Threshold] and
      (__ \ "appliedForException").formatNullable[Boolean] and
      (__ \ "partyType").format[PartyType] and
      (__ \ "registrationReason").format[RegistrationReason] and
      (__ \ "togcBlock").formatNullable[TogcCole] and
      (__ \ "isTransactor").formatWithDefault[Boolean](false) and
      (__ \ "calculatedDate").formatNullable[LocalDate] and
      OFormat(
        (__ \ "fixedEstablishmentInManOrUk")
          .read[Boolean]
          .orElse(
            (__ \ "partyType").read[PartyType].map(partyType => !Seq(NonUkNonEstablished, NETP).contains(partyType))
          ),
        (__ \ "fixedEstablishmentInManOrUk").write[Boolean]
      ) // TODO replace this explicit format with (__ \ "fixedEstablishmentInManOrUk").format[Boolean] 28 days after this is deployed, this ensures that users with old data are not impacted
  )(EligibilitySubmissionData.apply, unlift(EligibilitySubmissionData.unapply))

}
