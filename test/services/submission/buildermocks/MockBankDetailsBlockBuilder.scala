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

package services.submission.buildermocks

import models.api.VatScheme
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsObject
import play.api.mvc.Request
import services.submission.BankDetailsBlockBuilder

trait MockBankDetailsBlockBuilder extends MockitoSugar {
  self: Suite =>

  val mockBankDetailsBlockBuilder: BankDetailsBlockBuilder = mock[BankDetailsBlockBuilder]

  def mockBuildBankDetailsBlock(vatScheme: VatScheme)(response: Option[JsObject]): OngoingStubbing[Option[JsObject]] =
    when(
      mockBankDetailsBlockBuilder.buildBankDetailsBlock(ArgumentMatchers.eq(vatScheme))(
        ArgumentMatchers.any[Request[_]]
      )
    )
      .thenReturn(response)

}
