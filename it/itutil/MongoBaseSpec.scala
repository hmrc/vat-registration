/*
 * Copyright 2016 HM Revenue & Customs
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

package itutil

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.DefaultApplicationLifecycle
import play.api.libs.json._
import play.api.{Configuration, Environment}
import play.modules.reactivemongo.ReactiveMongoComponentImpl
import uk.gov.hmrc.mongo.MongoSpecSupport

trait MongoBaseSpec extends PlaySpec with MongoSpecSupport with BeforeAndAfterEach with ScalaFutures with Eventually with GuiceOneAppPerSuite {

  lazy val applicationLifeCycle = new DefaultApplicationLifecycle
  lazy val reactiveMongoComponent = new ReactiveMongoComponentImpl(fakeApplication.injector.instanceOf[Configuration],
                                                                   fakeApplication.injector.instanceOf[Environment], applicationLifeCycle)

  implicit val jsObjWrites: OWrites[JsObject] = new OWrites[JsObject]{
    override def writes(o: JsObject): JsObject = o
  }

  val _id = "_id"

  implicit class RichOptJsObj(o: Option[JsObject]) {
    def without(key: String): Option[JsObject] = o.map(_ - key)
  }
}
