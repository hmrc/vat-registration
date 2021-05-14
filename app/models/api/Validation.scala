/*
 * Copyright 2021 HM Revenue & Customs
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

package models.api

import java.util.regex.Pattern

import play.api.libs.json.Reads.{email, pattern}
import play.api.libs.json._

trait Validation {
  protected def readToFmt(rds: Reads[String])(implicit wts: Writes[String]): Format[String] = Format(rds, wts)

  protected def acceptOnly(validStrings: String*)(implicit wts: Writes[String]): Format[String] =
    readToFmt(pattern(validStrings.map(Pattern.quote).mkString("^(?:", "|", ")$").r))
}

trait VatBankAccountValidator extends Validation {
  protected val accountNumberValidator: Format[String]    = readToFmt(pattern("^(\\d){8}$".r))
  protected val accountSortCodeValidator: Format[String]  = readToFmt(pattern("^(\\d){2}-(\\d){2}-(\\d){2}$".r))
}

trait VatChoiceValidator extends Validation {
  val necessityValidator: Format[String] = acceptOnly("voluntary", "obligatory")
  val reasonValidator: Format[String]    = acceptOnly("COMPANY_ALREADY_SELLS_TAXABLE_GOODS_OR_SERVICES", "COMPANY_INTENDS_TO_SELLS_TAXABLE_GOODS_OR_SERVICES_IN_THE_FUTURE", "NEITHER")
}

trait VatApplicantDetailsValidator extends Validation {
  val ninoValidator: Format[String]   = readToFmt(pattern("[[A-Z]&&[^DFIQUV]][[A-Z]&&[^DFIQUVO]] ?\\d{2} ?\\d{2} ?\\d{2} ?[A-D]{1}".r))
  val roleValidator: Format[String]   = acceptOnly("director", "secretary")
  val nameRegex                       = """^[A-Za-z 0-9\-';]{1,100}$""".r
  val titleRegex                      = """^[A-Za-z]{1,20}$""".r
  val titleValidator: Format[String]  = readToFmt(pattern(titleRegex))
  val nameValidator: Format[String]   = readToFmt(pattern(nameRegex))
  val emailValidator: Format[String]  = readToFmt(email)
  val telValidator: Format[String]    = readToFmt(pattern("^(\\d){1,20}$".r))
  val mobileValidator: Format[String] = readToFmt(pattern("^(\\d){1,20}$".r))
}

trait SicCodeValidator extends Validation {
  val idValidator: Format[String] = readToFmt(pattern("""^(\d){5}""".r))
}
