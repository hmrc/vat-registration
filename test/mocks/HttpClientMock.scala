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

package mocks

import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.mockito.{ArgumentMatchers => AM}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads}
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

trait HttpClientMock {
  this: MockitoSugar =>

  lazy val mockHttpClient: HttpClient = mock[HttpClient]

  def mockHttpGet[T](url: String, thenReturn: T): OngoingStubbing[Future[T]] = {
    when(mockHttpClient.GET[T](AM.anyString())(AM.any[HttpReads[T]](), AM.any[HeaderCarrier](), AM.any[ExecutionContext]()))
      .thenReturn(Future.successful(thenReturn))
  }

  def mockHttpGet[T](url: String, thenReturn: Future[T]): OngoingStubbing[Future[T]] = {
    when(mockHttpClient.GET[T](AM.anyString())(AM.any[HttpReads[T]](), AM.any[HeaderCarrier](), AM.any[ExecutionContext]()))
      .thenReturn(thenReturn)
  }

  def mockHttpPOST[I, O](url: String, thenReturn: O, mockWSHttp: HttpClient = mockHttpClient): OngoingStubbing[Future[O]] = {
    when(mockWSHttp.POST[I, O](AM.anyString(), AM.any[I](), AM.any())(AM.any[Writes[I]](), AM.any[HttpReads[O]](),
      AM.any[HeaderCarrier](), AM.any[ExecutionContext]()))
      .thenReturn(Future.successful(thenReturn))
  }

  def mockHttpPUT[I, O](url: String, thenReturn: O, mockWSHttp: HttpClient = mockHttpClient): OngoingStubbing[Future[O]] = {
    when(mockWSHttp.PUT[I, O](AM.anyString(), AM.any[I]())(AM.any[Writes[I]](), AM.any[HttpReads[O]](), AM.any[HeaderCarrier](), AM.any[ExecutionContext]()))
      .thenReturn(Future.successful(thenReturn))
  }

  def mockHttpPATCH[I, O](url: String, thenReturn: O, mockWSHttp: HttpClient = mockHttpClient): OngoingStubbing[Future[O]] = {
    when(mockWSHttp.PATCH[I, O](AM.anyString(), AM.any[I]())(AM.any[Writes[I]](), AM.any[HttpReads[O]](), AM.any[HeaderCarrier](), AM.any[ExecutionContext]()))
      .thenReturn(Future.successful(thenReturn))
  }


  def mockHttpFailedGET[T](url: String, exception: Exception): OngoingStubbing[Future[T]] = {
    when(mockHttpClient.GET[T](AM.anyString())(AM.any[HttpReads[T]](), AM.any[HeaderCarrier](), AM.any[ExecutionContext]()))
      .thenReturn(Future.failed(exception))
  }

  def mockHttpFailedPOST[I, O](url: String, exception: Exception, mockWSHttp: HttpClient = mockHttpClient): OngoingStubbing[Future[O]] = {
    when(mockWSHttp.POST[I, O](AM.anyString(), AM.any[I](), AM.any())(AM.any[Writes[I]](), AM.any[HttpReads[O]](),
      AM.any[HeaderCarrier](), AM.any[ExecutionContext]()))
      .thenReturn(Future.failed(exception))
  }

  def mockHttpFailedPATCH[I, O](url: String, exception: Exception, mockWSHttp: HttpClient = mockHttpClient): OngoingStubbing[Future[O]] = {
    when(mockWSHttp.PATCH[I, O](AM.anyString(), AM.any[I]())(AM.any[Writes[I]](), AM.any[HttpReads[O]](), AM.any[HeaderCarrier](), AM.any[ExecutionContext]()))
      .thenReturn(Future.failed(exception))
  }
}
