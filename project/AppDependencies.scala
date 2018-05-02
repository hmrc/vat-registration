/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the Licens  e at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import play.core.PlayVersion
import sbt._

object AppDependencies {
  def apply(): Seq[ModuleID] = CompileDependencies() ++ UnitTestDependencies() ++ IntegrationTestDependencies()
}

object CompileDependencies {
  val domainVersion                       = "5.1.0"
  val bootstrapVersion                    = "6.15.0"
  val reactiveMongoVersion                = "5.2.0"
  val urlBindersVersion                   = "2.1.0"
  val catsVersion                         = "0.9.0"
  private val authClientVersion           = "2.5.0"

  val compile = Seq(
    "uk.gov.hmrc"   %% "play-reactivemongo"     % reactiveMongoVersion,
    "uk.gov.hmrc"   %% "microservice-bootstrap" % bootstrapVersion,
    "uk.gov.hmrc"   %% "play-url-binders"       % urlBindersVersion,
    "uk.gov.hmrc"   %% "domain"                 % domainVersion,
    "org.typelevel" %% "cats"                   % catsVersion,
    "uk.gov.hmrc" %% "auth-client"              % authClientVersion

  )

  def apply(): Seq[ModuleID] = compile
}

object UnitTestDependencies extends CommonTestDependencies {
  override val scope: Configuration = Test

  val mockitoVersion = "2.13.0"

  override val testDependencies: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "hmrctest"                       % hmrcTestVersion           % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play"             % scalaTestPlusVersion      % scope,
    "org.scoverage"           %  "scalac-scoverage-runtime_2.11"  % scoverageVersion          % scope,
    "uk.gov.hmrc"             %% "reactivemongo-test"             % reactiveMongoTestVersion  % scope,
    "org.mockito"             %  "mockito-core"                   % mockitoVersion            % scope
  )

  def apply(): Seq[ModuleID] = testDependencies
}

object IntegrationTestDependencies extends CommonTestDependencies {
  override val scope: Configuration = IntegrationTest

  val wireMockVersion = "2.6.0"

  override val testDependencies: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "hmrctest"                       % hmrcTestVersion           % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play"             % scalaTestPlusVersion      % scope,
    "org.scoverage"           %  "scalac-scoverage-runtime_2.11"  % scoverageVersion          % scope,
    "uk.gov.hmrc"             %% "reactivemongo-test"             % reactiveMongoTestVersion  % scope,
    "com.github.tomakehurst"  %  "wiremock"                       % wireMockVersion           % scope
  )

  def apply(): Seq[ModuleID] = testDependencies
}

trait CommonTestDependencies {
  val hmrcTestVersion          = "3.0.0"
  val scalaTestPlusVersion     = "2.0.1"
  val scoverageVersion         = "1.3.1"
  val reactiveMongoTestVersion = "2.0.0"

  val scope: Configuration
  val testDependencies: Seq[ModuleID]
}
