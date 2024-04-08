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

package services

import com.fasterxml.jackson.databind.ObjectMapper
import models.api.schemas.ApiSchema
import org.openapi4j.schema.validator.v3.SchemaValidator
import org.openapi4j.schema.validator.{ValidationContext, ValidationData}
import uk.gov.hmrc.http.InternalServerException

import javax.inject.Singleton
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

@Singleton
class SchemaValidationService {

  def validate(api: ApiSchema, body: String): Map[String, List[String]] = {
    val output = new ValidationData
    val json   = new ObjectMapper().readTree(body)

    // Setting the schemaNode as 'null' to avoid confusing prefix on jspaths. "Because Java"
    Try {
      new SchemaValidator(new ValidationContext(api.fullSchema.getContext), null, api.defaultSchema)
        .validate(json, output)
    } match {
      case Failure(_) =>
        throw new InternalServerException("Failed to validate request against schema for unknown reason")
      case Success(_) =>
        output
          .results()
          .items()
          .asScala
          .map(x => x.dataJsonPointer())
          .toList
          .map {
            case e if api.suppressedErrors.exists(regex => e.matches(regex)) => "suppressedErrors" -> e
            case e                                                           => "unknownErrors"    -> e
          }
          .groupBy(_._1)
          .view
          .mapValues(_.map(_._2).toList)
          .toMap
    }

  }

}
