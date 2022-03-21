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

package models.registration

import play.api.mvc.PathBindable

sealed trait CollectionSectionId extends RegistrationSectionId {
  val minIndex: Int = 1
  val maxIndex: Int
}

case object OtherBusinessInvolvementsSectionId extends CollectionSectionId {
  val key = "other-business-involvements"
  val repoKey = "otherBusinessInvolvements"
  val maxIndex = 10
}

object CollectionSectionId {
  // scalastyle:off
  implicit def urlBinder(implicit stringBinder: PathBindable[String]): PathBindable[CollectionSectionId] =
    new PathBindable[CollectionSectionId] {
      override def bind(key: String, value: String): Either[String, CollectionSectionId] = {
        for {
          id <- stringBinder.bind(key, value).right
          section <- (key, id) match {
            case ("section", OtherBusinessInvolvementsSectionId.key) => Right(OtherBusinessInvolvementsSectionId)
            case _ => Left("Invalid registration section")
          }
        } yield section
      }

      override def unbind(key: String, value: CollectionSectionId): String =
        stringBinder.unbind(key, value.key)
    }
}
