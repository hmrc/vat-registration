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

import sbt.Keys.scalacOptions
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

val appName = "vat-registration"
val testThreads = 12

lazy val scoverageSettings = Seq(
  ScoverageKeys.coverageExcludedPackages  := "<empty>;Reverse.*;config.*;.*(AuthService|BuildInfo|Routes).*;featureswitch.*;controllers.test.*",
  ScoverageKeys.coverageMinimumStmtTotal  := 90,
  ScoverageKeys.coverageFailOnMinimum     := true,
  ScoverageKeys.coverageHighlighting      := true
)

lazy val aliases: Seq[Def.Setting[_]] = Seq(
  addCommandAlias("testTime", "testOnly * -- -oD")
).flatten

lazy val testSettings = Seq(
  Test / fork                            := true,
  Test / testForkedParallel              := false,
  Test / parallelExecution               := true,
  Test / logBuffered                     := false
)

ThisBuild / scalaVersion := "2.13.12"
ThisBuild / majorVersion := 1

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(PlayScala, SbtGitVersioning, SbtDistributablesPlugin): _*)
  .settings(routesImport := Seq("models.registration.RegistrationSectionId.urlBinder"))
  .settings(PlayKeys.playDefaultPort := 9896)
  .settings(scalaSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(testSettings: _*)
  .settings(aliases: _*)
  .settings(
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    Global / cancelable := true,
    scalacOptions += "-Wconf:cat=unused-imports&src=routes/.*:s",
    libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
)

.disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

lazy val it = project.in(file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)
