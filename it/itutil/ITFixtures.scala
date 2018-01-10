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

import common.RegistrationId
import enums.VatRegStatus
import models.api._
import uk.gov.hmrc.http.HeaderCarrier

trait ITFixtures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val date: LocalDate = LocalDate.of(2017, 1, 1)
  val startDate = StartDate(Some(date))
  val regId = RegistrationId("regId")
  val vatScheme = VatScheme(regId, status = VatRegStatus.draft)
  val tradingName = TradingName(selection = true, Some("some-trading-name"))
  val oldName = Name(first = Some("Bob Smith"), middle = None, last = None, forename = None, surname = None, title = None, otherForenames = None)
  val changeOfName = ChangeOfName(true, Some(FormerName("", LocalDate.now(), name = oldName, change = LocalDate.now())))

  val vatTradingDetails = VatTradingDetails(
    tradingName = tradingName,
    euTrading = VatEuTrading(
      selection = true,
      eoriApplication = Some(true)
    )
  )
  val tradingDetails = VatTradingDetails(
    tradingName = tradingName,
    euTrading = VatEuTrading(selection = true, eoriApplication = Some(true))
  )
  val returns = Returns(
    reclaimVatOnMostReturns = true,
    frequency = "quarterly",
    staggerStart = Some("jan"),
    start = startDate
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
  val vatFinancials = VatFinancials(
    bankAccount = Some(VatBankAccount("Reddy", "101010", "100000000000")),
    turnoverEstimate = EstimateValue,
    zeroRatedTurnoverEstimate = Some(zeroRatedTurnoverEstimate)
  )

  val scrsAddress               = Address("line1", "line2", None, None, Some("XX XX"), Some("UK"))
  val vatDigitalContact         = VatDigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))
  val vatContact                = VatContact(digitalContact = vatDigitalContact, website = None, ppob = scrsAddress)

  val name                      = Name(first = Some("Forename"),
    middle = None,
    last = Some("Surname"),
    forename = Some("Forename"),
    surname = Some("Surname"),
    title = Some("Title")
  )
  val contact                   = OfficerContactDetails(Some("test@test.com"), None, None)
  val formerName                = FormerName("Bob Smith", date, name = oldName, change = date)
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
}
