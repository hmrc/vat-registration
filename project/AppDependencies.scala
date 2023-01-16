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

import sbt._

object AppDependencies {
  def apply(): Seq[ModuleID] = CompileDependencies() ++ UnitTestDependencies() ++ IntegrationTestDependencies()
}

object CompileDependencies {
  val domainVersion = "8.1.0-play-28"
  val bootstrapVersion = "7.12.0"
  val hmrcMongoVersion = "0.74.0"
  val catsVersion = "2.8.0"

  val flexmarkVersion = "0.36.8"

  val compile = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % hmrcMongoVersion,
    "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrapVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.openapi4j" % "openapi-operation-validator" % "1.0.7",
    "org.openapi4j" % "openapi-parser" % "1.0.7"
  )

  def apply(): Seq[ModuleID] = compile
}

object UnitTestDependencies extends CommonTestDependencies {
  override val scope: Configuration = Test

  val mockitoVersion = "4.8.1"

  override val testDependencies: Seq[ModuleID] = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
    "org.scoverage" %% "scalac-scoverage-runtime" % scoverageVersion % scope,
    "org.mockito" % "mockito-core" % mockitoVersion % scope,
    "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % "test",
    "com.miguno.akka" %% "akka-mock-scheduler" % "0.5.5" % scope,
    "com.vladsch.flexmark" % "flexmark-all" % flexmarkVersion % scope,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.0" % scope
  )

  def apply(): Seq[ModuleID] = testDependencies
}

object IntegrationTestDependencies extends CommonTestDependencies {
  override val scope: Configuration = IntegrationTest

  val wireMockVersion = "2.35.0"

  override val testDependencies: Seq[ModuleID] = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
    "org.scoverage" %% "scalac-scoverage-runtime" % scoverageVersion % scope,
    "com.github.tomakehurst" % "wiremock-jre8" % wireMockVersion % scope,
    "com.vladsch.flexmark" % "flexmark-all" % flexmarkVersion % scope, // Scalatest doesn't currently work with the latest Flexmark
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.0" % scope
  )

  def apply(): Seq[ModuleID] = testDependencies
}

trait CommonTestDependencies {
  val scalaTestPlusVersion = "5.1.0"
  val scoverageVersion = "1.4.1"
  val flexmarkVersion = "0.36.8"
  val scope: Configuration
  val testDependencies: Seq[ModuleID]
}
