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

package mocks

import models.api.schemas.ApiSchema
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalatestplus.mockito.MockitoSugar.mock
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import services.SchemaValidationService

trait MockSchemaValidationService {

  val mockSchemaValidationService = mock[SchemaValidationService]

  def mockValidate(body: String)(errors: Map[String, List[String]]): OngoingStubbing[Map[String, List[String]]] =
    when(mockSchemaValidationService.validate(any[ApiSchema], ArgumentMatchers.eq(body)))
      .thenReturn(errors)

}
