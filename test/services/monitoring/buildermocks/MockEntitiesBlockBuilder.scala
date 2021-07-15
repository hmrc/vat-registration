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

package services.monitoring.buildermocks

import org.mockito.ArgumentMatchers
import org.scalatest.Suite
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.JsValue
import services.submission.EntitiesBlockBuilder

import scala.concurrent.Future

trait MockEntitiesBlockBuilder extends MockitoSugar {
  self: Suite =>

  val mockEntitiesBlockBuilder = mock[EntitiesBlockBuilder]

  def mockBuildEntitiesBlock(regId: String)(response: Future[Option[JsValue]]): OngoingStubbing[Future[Option[JsValue]]] =
    when(mockEntitiesBlockBuilder.buildEntitiesBlock(
      ArgumentMatchers.eq(regId)
    )).thenReturn(response)

}
