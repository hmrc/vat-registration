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

package featureswitch.core.config

import featureswitch.core.models.FeatureSwitch
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

import javax.inject.Singleton

@Singleton
class FeatureSwitchingModule extends Module with FeatureSwitchRegistry {

  val switches = Seq(StubSubmission, PostSubmissionDecoupling, PostSubmissionNonDecoupling, PostSubmissionDecouplingConnector)

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] =
    Seq(
      bind[FeatureSwitchRegistry].to(this).eagerly()
    )
}

case object StubSubmission extends FeatureSwitch {
  override val configName: String  = "feature-switch.submission-stub"
  override val displayName: String = "Use stub for Submission to DES"
}

case object PostSubmissionDecoupling extends FeatureSwitch {
  override val configName: String = "feature-switch.post-submission-decoupling"
  override val displayName: String = "Decouple SDES and NRS integrations"
}

case object PostSubmissionNonDecoupling extends FeatureSwitch {
  override val configName: String = "feature-switch.post-submission-non-decoupling"
  override val displayName: String = "Enable non-decoupled (old) SDES and NRS integrations"
}

case object PostSubmissionDecouplingConnector extends FeatureSwitch {
  override val configName: String = "feature-switch.post-submission-decoupling-connector"
  override val displayName: String = "Enable connector calls for decoupled SDES and NRS integrations"
}
