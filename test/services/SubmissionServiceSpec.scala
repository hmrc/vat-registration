/*
 * Copyright 2020 HM Revenue & Customs
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

package services

import java.time.LocalDate

import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import common.exceptions._
import common.{RegistrationId, TransactionId}
import connectors.{CompanyRegistrationConnector, DESConnectorImpl, IncorporationInformationConnector}
import enums.VatRegStatus
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api._
import models.external.{IncorpStatusEvent, IncorpSubscription, IncorporationStatus}
import models.submission.{DESSubmission, TopUpSubmission}
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import repositories.{RegistrationRepository, SequenceRepository}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, NotFoundException}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubmissionServiceSpec extends VatRegSpec with VatRegistrationFixture with ApplicativeSyntax with FutureInstances {
  class Setup(useMockSub: Boolean = false) {
    val service = new SubmissionSrv {
      override val vatRegistrationService: VatRegistrationService = mockVatRegistrationService
      override val companyRegistrationConnector: CompanyRegistrationConnector = mockCompanyRegConnector
      override val desConnector: DESConnectorImpl = mockDesConnector
      override val incorporationInformationConnector: IncorporationInformationConnector = mockIIConnector
      override val sequenceRepository: SequenceRepository = mockSequenceRepository
      override val registrationRepository: RegistrationRepository = mockRegistrationRepository

      override private[services] def useMockSubmission: Boolean = useMockSub
    }
  }

  implicit val hc = HeaderCarrier()

  "submitVatRegistration" should {
    val transactionIdJson = Json.obj("confirmationReferences" -> Json.obj("transaction-id" -> "foo"))

    "successfully return a future string when mockSubmission = false" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme.copy(returns = Some(Returns(true,"",Some("foo"),StartDate(Some(LocalDate.now))))))))
      when(mockSequenceRepository.getNext(any())(any())).thenReturn(Future.successful(100))
      when(mockRegistrationRepository.prepareRegistrationSubmission(RegistrationId(anyString()), any(), any())(any())).thenReturn(Future.successful(true))
      when(mockCompanyRegConnector.fetchCompanyRegistrationDocument(RegistrationId(anyString()))(any())).thenReturn(Future.successful(HttpResponse(200, Some(transactionIdJson))))
      when(mockRegistrationRepository.saveTransId(any(),RegistrationId(anyString()))(any())).thenReturn(Future.successful("transID"))
      when(mockIIConnector.retrieveIncorporationStatus(any(), TransactionId(anyString()), any(), any())(any(),any[HttpReads[IncorporationStatus]]()))
        .thenReturn(Future.successful(
          Some(IncorporationStatus(
            IncorpSubscription("transID", "regime", "subscriber", "url"),
            IncorpStatusEvent("status", Some("crn"),Some(LocalDate.now()), Some("description") )))))
      when(mockIIConnector.getCompanyName(RegistrationId(anyString()),TransactionId(anyString()))(any())).thenReturn(Future.successful(HttpResponse(200, Some(Json.obj("company_name" -> "compName123")))))
      when(mockDesConnector.submitToDES(any[DESSubmission],any())(any())).thenReturn(Future.successful(HttpResponse(200)))
      when(mockRegistrationRepository.finishRegistrationSubmission(RegistrationId(anyString()),any())(any())).thenReturn(Future.successful(VatRegStatus.submitted))

      await(service.submitVatRegistration(RegistrationId("foo"))) shouldBe "BRVT00000000100"

    }
    "successfully submit to des using mockSubmission = true" in new Setup(true) {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme.copy(returns = Some(Returns(true,"",Some("foo"),StartDate(Some(LocalDate.now))))))))
      when(mockSequenceRepository.getNext(any())(any())).thenReturn(Future.successful(100))
      when(mockRegistrationRepository.prepareRegistrationSubmission(RegistrationId(anyString()), any(), any())(any())).thenReturn(Future.successful(true))
      when(mockRegistrationRepository.saveTransId(any(),RegistrationId(anyString()))(any())).thenReturn(Future.successful("transID"))
      when(mockDesConnector.submitToDES(any[DESSubmission],any())(any())).thenReturn(Future.successful(HttpResponse(200)))
      when(mockRegistrationRepository.finishRegistrationSubmission(RegistrationId(anyString()),any())(any())).thenReturn(Future.successful(VatRegStatus.submitted))

      await(service.submitVatRegistration(RegistrationId("foo"))) shouldBe "BRVT00000000100"
    }
  }


  "call to getAcknowledgementReference" should {

    val vatScheme = VatScheme(RegistrationId("1"),internalId = internalid, None, None, None, status = VatRegStatus.draft)

    "return Success response " in new Setup {
      when(mockVatRegistrationService.retrieveAcknowledgementReference(regId)).
        thenReturn(ServiceMocks.serviceResult(ackRefNumber))

      service.getAcknowledgementReference(regId) returnsRight ackRefNumber
    }

    "return ResourceNotFound response " in new Setup {
      val resourceNotFound = ResourceNotFound("Resource Not Found for regId 1")
      when(mockVatRegistrationService.retrieveAcknowledgementReference(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(ServiceMocks.serviceError[String](resourceNotFound))

      service.getAcknowledgementReference(regId) returnsLeft resourceNotFound
    }
  }

  "getRegByTxId" should {
    val vatScheme = VatScheme(
      RegistrationId("1"),
      internalId = internalid,
      transactionId = Some(TransactionId("testTransId")),
      acknowledgementReference = Some("testref"),
      status = VatRegStatus.draft
    )

    val foVatScheme = Future.successful(Some(vatScheme))

    "return a vat scheme when provided with a transaction id of a document" in new Setup {
      when(mockRegistrationRepository.fetchRegByTxId(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(foVatScheme)

      await(service.getRegistrationIDByTxId("testTransId")) shouldBe RegistrationId("1")
    }

    "throw a NoVatSchemeWithTransId exception if the vat scheme was not found for the provided transaction id" in new Setup {
      when(mockRegistrationRepository.fetchRegByTxId(ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      intercept[NoVatSchemeWithTransId](await(service.getRegistrationIDByTxId("testRegId")))
    }

  }

  "ensureAcknowledgementReference" should {
    val vatScheme = VatScheme(RegistrationId("1"),internalid, None, None, None, status = VatRegStatus.draft, acknowledgementReference = Some("testref"))
    val sequenceNo = 1
    val formatedRefNumber = f"BRVT$sequenceNo%011d"

    "throw an exception if the document is not available" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      intercept[MissingRegDocument](await(service.ensureAcknowledgementReference(regId, VatRegStatus.draft)))
    }

    "get the acknowledgement references if they are available" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme)))

      await(service.ensureAcknowledgementReference(regId, VatRegStatus.draft)) shouldBe "testref"
    }

    "generate acknowledgment reference if it does not exist" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme.copy(status = VatRegStatus.draft, acknowledgementReference = None))))
      when(mockSequenceRepository.getNext(ArgumentMatchers.eq("AcknowledgementID"))(ArgumentMatchers.any())).thenReturn(sequenceNo.pure)
      when(mockRegistrationRepository.prepareRegistrationSubmission(RegistrationId(anyString()), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))

      await(service.ensureAcknowledgementReference(regId, VatRegStatus.draft)) shouldBe formatedRefNumber
    }
  }

  "getValidDocumentStatus" should {
    val vatScheme = VatScheme(RegistrationId("1"),internalid, None, None, None, status = VatRegStatus.draft)

    "throw an exception if the document is not available" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      intercept[MissingRegDocument](await(service.getValidDocumentStatus(regId)))
    }

    "throw an exception if the document is not locked or draft" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme.copy(status = VatRegStatus.cancelled))))

      intercept[InvalidSubmissionStatus](await(service.getValidDocumentStatus(regId)))
    }

    "return the status as being draft" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme)))

      await(service.getValidDocumentStatus(regId)) shouldBe VatRegStatus.draft
    }
  }

  "fetchCompanyRegistrationTransactionID" should {
    val transId = "transId"
    "on a successful ok response with transID, return the transID" in new Setup {
      val testJson = Json.parse(
        s"""
          |{
          | "confirmationReferences": {
          |   "transaction-id" : "$transId"
          | }
          |}
        """.stripMargin
      )

      val okResponse = new HttpResponse {
        override def status: Int = OK
        override def json: JsValue = testJson
      }

      when(mockCompanyRegConnector.fetchCompanyRegistrationDocument(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(okResponse))

      when(mockRegistrationRepository.saveTransId(anyString(), RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(transId))

      await(service.fetchCompanyRegistrationTransactionID(regId)) shouldBe "transId"
    }

    "on a successful ok response without transID, throw a NoTransactionId exception" in new Setup {
      val testJson = Json.parse(
        """
          |{
          | "confirmationReferences": {
          |   "test" : "test"
          | }
          |}
        """.stripMargin
      )

      val okResponse = new HttpResponse {
        override def status: Int = OK
        override def json: JsValue = testJson
      }

      when(mockCompanyRegConnector.fetchCompanyRegistrationDocument(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(okResponse))

      intercept[NoTransactionId](await(service.fetchCompanyRegistrationTransactionID(regId)))
    }
  }

  "getCompanyName" should {
    "return the company name if it exists" in new Setup {
      val testJson = Json.parse(
        """
          |{
          | "company_name": "companyname"
          |}
        """.stripMargin
      )

      val okResponse = new HttpResponse {
        override def status: Int = OK
        override def json: JsValue = testJson
      }

      when(mockIIConnector.getCompanyName(RegistrationId(anyString()), TransactionId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(okResponse))

      await(service.getCompanyName(regId, txId)) shouldBe "companyname"
    }

    "throw an exception if it does not" in new Setup {
      when(mockIIConnector.getCompanyName(RegistrationId(anyString()), TransactionId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("not found")))

      intercept[NotFoundException](await(service.getCompanyName(regId, txId)))
    }

    "fails to convert the company_name to json" in new Setup {
      val testJson = Json.parse(
        """
          |{
          |}
        """.stripMargin
      )

      val okResponse = new HttpResponse {
        override def status: Int = OK
        override def json: JsValue = testJson
      }

      when(mockIIConnector.getCompanyName(RegistrationId(anyString()), TransactionId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(okResponse))

      intercept[NoCompanyName](await(service.getCompanyName(regId, txId)))
    }
  }

  "registerForInterest" should {
    "return the incorporation status if a transaction ID is provided" in new Setup {
      val incorpstatus: IncorporationStatus = incorporationStatus()

      IIMocks.mockIncorporationStatus(incorpstatus)
      await(service.registerForInterest("transID", "any")) shouldBe Some(incorpstatus)
    }

    "return no incorporation status on a 202 from II" in new Setup {
      IIMocks.mockIncorporationStatusNone()
      await(service.registerForInterest("transID", "any")) shouldBe None
    }
  }

  "updateSubmissionStatus" should {
    "update the submission to submitted if there is an incorp date" in new Setup {
      when(mockRegistrationRepository.finishRegistrationSubmission(RegistrationId(anyString()), ArgumentMatchers.any())
        (ArgumentMatchers.any())).thenReturn(Future.successful(VatRegStatus.submitted))

      await(service.updateSubmissionStatus(regId, Some(LocalDate.now()))) shouldBe VatRegStatus.submitted
    }

    "update the submission to held if there is no incorp date" in new Setup {
      when(mockRegistrationRepository.finishRegistrationSubmission(RegistrationId(anyString()), ArgumentMatchers.any())
        (ArgumentMatchers.any())).thenReturn(Future.successful(VatRegStatus.held))

      await(service.updateSubmissionStatus(regId, None)) shouldBe VatRegStatus.held
    }
  }

  "updateTopUpSubmissionStatus" should {
    "update the submission to submitted if there is an incorp date" in new Setup {
      when(mockRegistrationRepository.finishRegistrationSubmission(RegistrationId(anyString()), ArgumentMatchers.any())
        (ArgumentMatchers.any())).thenReturn(Future.successful(VatRegStatus.submitted))

      await(service.updateTopUpSubmissionStatus(regId, "accepted")) shouldBe VatRegStatus.submitted
    }

    "update the submission to held if there is no incorp date" in new Setup {
      when(mockRegistrationRepository.finishRegistrationSubmission(RegistrationId(anyString()), ArgumentMatchers.any())
        (ArgumentMatchers.any())).thenReturn(Future.successful(VatRegStatus.rejected))

      await(service.updateTopUpSubmissionStatus(regId, "rejected")) shouldBe VatRegStatus.rejected
    }
  }

  "getIncorpDate" should {
    val incorpStatus = incorporationStatus()
    "return the incorp date status if it exists" in new Setup {
      service.getIncorpDate(incorpStatus) shouldBe incorpStatus.statusEvent.incorporationDate.get
    }

    "throw a NoIncorpDate exception if it could not retrieve one" in new Setup {
      val incorpWithoutDate = incorpStatus.copy(
        statusEvent = incorpStatus.statusEvent.copy(incorporationDate = None)
      )
      intercept[NoIncorpDate](service.getIncorpDate(incorpWithoutDate))
    }
  }

  "Calling buildDesSubmission" should {

    val schemeReturns = Returns(true, "monthly", None, StartDate(date = Some(date)))
    val vatScheme = VatScheme(RegistrationId("1"),internalid, Some(TransactionId("1")), returns = Some(schemeReturns), status = VatRegStatus.draft)
    val vatSchemeNoTradingDetails = VatScheme(RegistrationId("1"),internalid, None, None, None, status = VatRegStatus.draft)
    val vatSchemeNoStartDate = VatScheme(RegistrationId("1"),internalid, Some(TransactionId("1")), None, None, status = VatRegStatus.draft)

    val fullDESSubmission = DESSubmission("ackRef", "companyName", Some(date), Some(date))
    val partialDESSubmission = DESSubmission("ackRef", "companyName", None, None)

    val someNow = Some(LocalDate.now())

    "successfully create a full DES submission" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme)))

      await(service.buildDesSubmission(regId, "ackRef", "companyName", Some(date))) shouldBe fullDESSubmission
    }

    "successfully create a partial DES submission" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme)))

      await(service.buildDesSubmission(regId, "ackRef", "companyName", None)) shouldBe partialDESSubmission
    }

    "throw a MissingRegDocument exception when there is no registration in mongo" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))
      intercept[MissingRegDocument](await(service.buildDesSubmission(regId, "ackRef", "companyName", someNow)))
    }

    "throw a NoRetuens exception when the vat scheme doesn't contain returns" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatSchemeNoTradingDetails)))

      intercept[NoReturns](await(service.buildDesSubmission(regId, "ackRef", "companyName", someNow)))
    }
  }

  "Calling buildTopUpDesSubmission" should {

    val someLocalDateNow = Some(LocalDate.now())
    val someDateTimeNow = Some(DateTime.now())

    val schemeReturns = Returns(true, "monthly", None, StartDate(date = someLocalDateNow))
    val vatScheme = VatScheme(RegistrationId("1"),internalid, Some(TransactionId("1")), returns = Some(schemeReturns), status = VatRegStatus.draft)
    val vatSchemeNoTradingDetails = VatScheme(RegistrationId("1"), internalid, None, None, None, status = VatRegStatus.draft)
    val vatSchemeNoStartDate = VatScheme(RegistrationId("1"),internalid, Some(TransactionId("1")), None, None, status = VatRegStatus.draft)

    val topUpAccepted = TopUpSubmission("ackRef", "accepted", someLocalDateNow, someDateTimeNow)
    val topUpRejected = TopUpSubmission("ackRef", "rejected")

    "successfully create an accepted top up DES submission" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme)))

      await(service.buildTopUpSubmission(regId, "ackRef", "accepted", someDateTimeNow)) shouldBe topUpAccepted
    }

    "successfully create a rejected top up DES submission" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme)))

      await(service.buildTopUpSubmission(regId, "ackRef", "rejected", None)) shouldBe topUpRejected
    }

    "throw a UnknownIncorpStatus exception if the status does not match to an valid status" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatScheme)))

      intercept[UnknownIncorpStatus](await(service.buildTopUpSubmission(regId, "ackRef", "unknownStatus", someDateTimeNow)))
    }

    "throw a MissingRegDocument exception when there is no registration in mongo" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(None))

      intercept[MissingRegDocument](await(service.buildTopUpSubmission(regId, "ackRef", "accepted", someDateTimeNow)))
    }

    "throw a NoReturns exception when the vat scheme doesn't contain returns" in new Setup {
      when(mockRegistrationRepository.retrieveVatScheme(RegistrationId(anyString()))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(vatSchemeNoTradingDetails)))

      intercept[NoReturns](await(service.buildTopUpSubmission(regId, "ackRef", "accepted", someDateTimeNow)))
    }
  }

}
