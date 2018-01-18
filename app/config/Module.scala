/*
 * Copyright 2018 HM Revenue & Customs
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

package config

import com.google.inject.AbstractModule
import controllers._
import services.{TradingDetailsService, TradingDetailsSrv}

class Module extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ProcessIncorporationsController]).to(classOf[ProcessIncorporationsControllerImpl]).asEagerSingleton()
    bind(classOf[connectors.AuthConnector]).to(classOf[connectors.VatRegAuthConnector]).asEagerSingleton()
    bind(classOf[connectors.BusinessRegistrationConnector]).to(classOf[connectors.VatRegBusinessRegistrationConnector]).asEagerSingleton()
    bind(classOf[connectors.IncorporationInformationConnector]).to(classOf[connectors.VatRegIncorporationInformationConnector ]).asEagerSingleton()
    bind(classOf[services.RegistrationService]).to(classOf[services.VatRegistrationService]).asEagerSingleton()
    bind(classOf[EligibilityController]).to(classOf[EligibilityControllerImpl]).asEagerSingleton()
    bind(classOf[ThresholdController]).to(classOf[ThresholdControllerImpl]).asEagerSingleton()
    bind(classOf[LodgingOfficerController]).to(classOf[LodgingOfficerControllerImpl]).asEagerSingleton()
    bind(classOf[SicAndComplianceController]).to(classOf[SicAndComplianceControllerImpl]).asEagerSingleton()
    bind(classOf[TradingDetailsController]).to(classOf[TradingDetailsControllerImpl]).asEagerSingleton()
    bind(classOf[TradingDetailsSrv]).to(classOf[TradingDetailsService]).asEagerSingleton()
    //TODO: Should all services be done this way or is it just the controller.
//    bind(classOf[services.LodgingOfficerSrv]).to(classOf[services.LodgingOfficerService]).asEagerSingleton()
//    bind(classOf[services.EligibilitySrv]).to(classOf[services.EligibilityService]).asEagerSingleton()
//    bind(classOf[services.ThresholdSrv]).to(classOf[services.ThresholdService]).asEagerSingleton()
  }
}
