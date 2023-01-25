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

import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "vat-registration"
val testThreads = 12

lazy val scoverageSettings = Seq(
  ScoverageKeys.coverageExcludedPackages  := "<empty>;Reverse.*;config.*;.*(AuthService|BuildInfo|Routes).*",
  ScoverageKeys.coverageMinimum           := 100,
  ScoverageKeys.coverageFailOnMinimum     := false,
  ScoverageKeys.coverageHighlighting      := true
)

lazy val aliases: Seq[Def.Setting[_]] = Seq(
  addCommandAlias("testTime", "testOnly * -- -oD")
).flatten

lazy val testSettings = Seq(
  Test / fork                            := true,
  Test / testForkedParallel              := false,
  Test / parallelExecution               := true,
  Test / logBuffered                     := false,
  IntegrationTest / fork                 := false,
  IntegrationTest / testForkedParallel   := false,
  IntegrationTest / parallelExecution    := false,
  IntegrationTest / logBuffered          := false,
  IntegrationTest / unmanagedSourceDirectories := (IntegrationTest / baseDirectory)(base => Seq(base / "it")).value,
  addTestReportOption(IntegrationTest, "int-test-reports")
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(PlayScala, SbtGitVersioning, SbtDistributablesPlugin): _*)
  .settings(routesImport := Seq("models.registration.RegistrationSectionId.urlBinder"))
  .settings(PlayKeys.playDefaultPort := 9896)
  .settings(scalaSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(testSettings: _*)
  .settings(aliases: _*)
  .settings(
    scalaVersion := "2.12.12",
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    Global / cancelable := true
  )
  .settings(majorVersion := 1)

  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
