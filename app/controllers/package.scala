/*
 * Copyright 2022 HM Revenue & Customs
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

import auth._
import common.exceptions.MissingRegDocument
import play.api.Logging
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Results._
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

package object controllers extends Logging {

  trait SendResult {
    def sendResult(method:String, regId:String): Future[Result]
    def missingDoc(method:String,regId:String):Results.Status = {
      logger.warn(s"[$method] User has attempted to perform an action, after being authorised, but the document is now missing for regId: $regId")
      NotFound
    }
    def unexpectedException(method:String,regId:String, e:String):Results.Status = {
      logger.warn(s"[$method] An unexpected exception has occurred for regId: $regId in [sendResult] with error: ${e}")
      InternalServerError
    }
  }

  implicit class HandleResultToSendJson[T](f: Future[T])(implicit writes: Writes[T], ec: ExecutionContext) extends SendResult {
    override def sendResult(method:String,regId:String):Future[Result] = {
      f.map {
        data => {
          Ok(Json.toJson(data))
        }
      } recover {
        case _: MissingRegDocument => missingDoc(method,regId)
        case e:Exception => unexpectedException(method,regId,e.getMessage)
      }
    }
  }

  implicit class HandleResultToSendOptionJson[T](f: Future[Option[T]])(implicit writes: Writes[T], ec: ExecutionContext) extends SendResult {
   override def sendResult(method: String, regId: String): Future[Result] = {
      f map {
        _.fold(NoContent)(data => Ok(Json.toJson(data)))
      } recover {
        case _: MissingRegDocument => missingDoc(method, regId)
        case e: Exception => unexpectedException(method, regId, e.getMessage)
      }
    }
  }

  implicit class HandleAuthResult(authResult: AuthorisationResult)(implicit hc: HeaderCarrier) {
    def ifAuthorised(regID: String, controller: String, method: String)(f: => Future[Result]): Future[Result] = authResult match {
      case Authorised(_) =>
        f
      case NotLoggedInOrAuthorised =>
        logger.info(s"[$controller] [$method] [ifAuthorised] User not logged in")
        Future.successful(Forbidden)
      case NotAuthorised(_) =>
        logger.info(s"[$controller] [$method] [ifAuthorised] User logged in but not authorised for resource $regID")
        Future.successful(Forbidden)
      case AuthResourceNotFound(_) =>
        logger.info(s"[$controller] [$method] [ifAuthorised] User logged in but no resource found for regId $regID")
        Future.successful(NotFound)
    }
  }

}
