/*
 * Copyright 2017 HM Revenue & Customs
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
package itutil

import java.time.LocalDate

import common.{RegistrationId, TransactionId}
import enums.VatRegStatus
import models.api._
import uk.gov.hmrc.http.HeaderCarrier

trait ITFixtures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val date: LocalDate = LocalDate.of(2017, 1, 1)
  val startDate = StartDate(Some(date))
  val regId = RegistrationId("regId")
  val internalid = "INT-123-456-789"
  val transactionId = "transId"
  val vatScheme = VatScheme(regId, internalId = internalid, status = VatRegStatus.draft)
  val tradingName = TradingName(selection = true, Some("some-trading-name"))
  val oldName = Name(first = Some("Bob Smith"), middle = None, last = None, forename = None, surname = None, title = None, otherForenames = None)
  val changeOfName = ChangeOfName(true, Some(FormerName(None, None, name = Some(oldName), change = Some(LocalDate.now()))))
  val tradingDetails = TradingDetails(Some("test-name"), Some(true))
  val returns = Returns(
    reclaimVatOnMostReturns = true,
    frequency = "quarterly",
    staggerStart = Some("jan"),
    start = startDate
  )
  val frsDetails = FRSDetails(
    businessGoods             = Some(BusinessGoods(12345678L,true)),
    startDate                 = Some(date),
    categoryOfBusiness        = "testCategory",
    percent                   = 15
  )


  val flatRateScheme = FlatRateScheme(
    joinFrs = true,
    Some(frsDetails)
  )
  val compliance =
    VatSicAndCompliance(
      businessDescription = "some-business-description",
      culturalCompliance = Some(VatComplianceCultural(true)),
      labourCompliance = Some(VatComplianceLabour(
        labour = true,
        workers = Some(10),
        temporaryContracts = Some(true),
        skilledWorkers = Some(true))),
      financialCompliance = Some(VatComplianceFinancial(adviceOrConsultancyOnly = true, actAsIntermediary = true)),
      mainBusinessActivity = SicCode("88888888", "description", "displayDetails")
    )

  val EstimateValue: Long = 1000L
  val zeroRatedTurnoverEstimate: Long = 1000L

  val scrsAddress               = Address("line1", "line2", None, None, Some("XX XX"), Some("UK"))
  val digitalContact            = DigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))
  val vatContact                = VatContact(digitalContact = digitalContact, website = None, ppob = scrsAddress)

  val name                      = Name(first = Some("Forename"),
    middle = None,
    last = Some("Surname"),
    forename = Some("Forename"),
    surname = Some("Surname"),
    title = Some("Title")
  )
  val contact                   = OfficerContactDetails(Some("test@test.com"), None, None)
  val formerName                = FormerName(Some("Bob Smith"), Some(date), name = Some(oldName), change = Some(date))
  val currentOrPreviousAddress  = CurrentOrPreviousAddress(false, Some(scrsAddress))
  val vatLodgingOfficer         = LodgingOfficer(
    currentAddress            = Some(scrsAddress),
    dob                       = LocalDate.of(1980, 1, 1),
    nino                      = "NB686868C",
    role                      = "director",
    name                      = name,
    changeOfName              = Some(changeOfName),
    currentOrPreviousAddress  = Some(currentOrPreviousAddress),
    contact                   = Some(contact),
    ivPassed                  = None,
    details                   = None
  )
  val businessContact         = BusinessContact(digitalContact = digitalContact, website = None, ppob = scrsAddress)
  val sicAndCompliance        = SicAndCompliance("businessDesc", Some(ComplianceLabour(1, Some(true), Some(true))), SicCode("12345678","sicDesc","sicDetail"))

  val vatComplicanceLabour    = VatComplianceLabour(true, Some(5), Some(true), Some(true))

  val vatComplianceFinancial  = VatComplianceFinancial(true, true, Some(true), Some(true), Some(true), Some(true), Some(true), Some(true))

  val vatSicAndCompliance     = VatSicAndCompliance("businessDesc", Some(VatComplianceCultural(true)), Some(vatComplicanceLabour), Some(vatComplianceFinancial), SicCode("12345678", "sic", "sic"))

  val vatServiceEligibility   = VatServiceEligibility(Some(true), Some(true), Some(true), Some(true), Some(true), Some(true), Some(VatEligibilityChoice("voluntary", None, None, None)))

  val eligibility             = Eligibility(1, "result")

  val vatTurnoverEstimates       = TurnoverEstimates(12345678L)

  val vatBankAccount             = BankAccount(true, None)

  val threshold               = Threshold(true, None, None, None, None)

  val fullVatScheme =
    VatScheme(
      regId,
      internalid,
      Some(TransactionId(transactionId)),
      Some(tradingDetails),
      Some(vatLodgingOfficer),
      Some(returns),
      Some(vatSicAndCompliance),
      Some(sicAndCompliance),
      Some(vatContact),
      Some(businessContact),
      Some(vatServiceEligibility),
      Some(eligibility),
      Some(vatTurnoverEstimates),
      Some(vatBankAccount),
      Some(threshold),
      Some("ackRef"),
      Some(flatRateScheme),
      VatRegStatus.draft
    )

  def emptyVatScheme(regId: String): VatScheme = VatScheme(
    id = RegistrationId(regId),
    internalId = internalid,
    status = VatRegStatus.draft
  )
}
