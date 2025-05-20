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
  def apply(): Seq[ModuleID] = CompileDependencies() ++ UnitTestDependencies()

  val it: Seq[ModuleID] = Seq()

}

object CompileDependencies {
  val domainVersion = "9.0.0"
  val bootstrapVersion = "8.6.0"
  val hmrcMongoVersion = "2.6.0"
  val catsVersion = "2.8.0"
  val flexmarkVersion = "0.36.8"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30" % hmrcMongoVersion,
    "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapVersion,
    "uk.gov.hmrc" %% "domain-play-30" % domainVersion,
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.openapi4j" % "openapi-operation-validator" % "1.0.7",
    "org.openapi4j" % "openapi-parser" % "1.0.7"
  )

  def apply(): Seq[ModuleID] = compile
}

object UnitTestDependencies extends CommonTestDependencies {
  override val scope: Configuration = Test

  override val test: Seq[ModuleID] = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
    "org.scoverage" %% "scalac-scoverage-runtime" % scoverageVersion % scope,
    "org.mockito" % "mockito-core" % mockitoVersion % scope,
    "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % "test",
    "com.github.pjfanning" %% "pekko-mock-scheduler" % "0.6.0" % scope,
    "com.vladsch.flexmark" % "flexmark-all" % flexmarkVersion % scope,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.14.0" % scope,
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % "8.6.0" % scope
  )

  def apply(): Seq[ModuleID] = test
}

trait CommonTestDependencies {
  val scalaTestPlusVersion = "7.0.1"
  val scoverageVersion = "2.1.0"
  val flexmarkVersion = "0.36.8"
  val mockitoVersion = "4.8.1"
  val scope: Configuration
  val test: Seq[ModuleID]
}
