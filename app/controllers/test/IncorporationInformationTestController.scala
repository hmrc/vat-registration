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

package controllers.test

import javax.inject.Inject

import auth.Authenticated
import cats.instances.FutureInstances
import common.TransactionId
import connectors.AuthConnector
import connectors.test.IncorporationInformationTestConnector
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.microservice.controller.BaseController
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class IncorporationInformationTestController @Inject()(val auth: AuthConnector,
                                                       iiTestConnector: IncorporationInformationTestConnector)
  extends BaseController with Authenticated with FutureInstances {

  // $COVERAGE-OFF$

  def incorpCompany(transactionId: TransactionId): Action[AnyContent] = Action.async { implicit request =>
    authenticated { user =>
      iiTestConnector.incorpCompany(transactionId).map(_ => Ok)
    }
  }

  // $COVERAGE-ON$


}