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
  fork                       in IntegrationTest := false,
  testForkedParallel         in IntegrationTest := false,
  parallelExecution          in IntegrationTest := false,
  logBuffered                in IntegrationTest := false,
  unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest) (base => Seq(base / "it")).value,
  fork                       in Test            := true,
  testForkedParallel         in Test            := true,
  parallelExecution          in Test            := true,
  logBuffered                in Test            := false,
  addTestReportOption(IntegrationTest, "int-test-reports")
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory): _*)
  .settings(PlayKeys.playDefaultPort := 9896)
  .settings(scalaSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(testSettings: _*)
  .settings(aliases: _*)
  .settings(majorVersion := 0)
  .settings(
    scalaVersion                     := "2.12.12",
    libraryDependencies              ++= AppDependencies(),
    retrieveManaged                  := true,
    cancelable             in Global := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)
  )
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

