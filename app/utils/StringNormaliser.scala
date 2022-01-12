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

package utils

import java.text.Normalizer
import java.text.Normalizer.Form

object StringNormaliser {

  val characterConversions: Map[Char, String] = Map(
    'æ' -> "ae",
    'Æ' -> "AE",
    'œ' -> "oe",
    'Œ' -> "OE",
    'Þ' -> "D",
    'þ' -> "d",
    'ŋ' -> "n",
    'Ŋ' -> "N",
    'ð' -> "d",
    'Ð' -> "D",
    'ø' -> "o",
    'Ø' -> "O",
    'ł' -> "l",
    'Ł' -> "L",
    'ŧ' -> "t",
    'Ŧ' -> "T",
    'ð' -> "d",
    'Ð' -> "D",
    'ħ' -> "h",
    'Ħ' -> "H",
    'Ŀ' -> "L",
    'ŀ' -> "l"
  )

  def normaliseString(string: String): String = Normalizer.normalize(string, Form.NFD)
    .map(char => characterConversions.getOrElse(char, char))
    .mkString
    .replaceAll("\\p{M}", "")

}
