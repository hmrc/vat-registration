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

package utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class StringNormaliserSpec extends AnyWordSpec with Matchers {

  "normaliseString" should {
    Map(
      "àáâãäåāăą"  -> "a",
      "çćĉċč"      -> "c",
      "þďð"        -> "d",
      "èéêëēĕėęě"  -> "e",
      "ĝģğġ"       -> "g",
      "ĥħ"         -> "h",
      "ìíîïĩīĭį"   -> "i",
      "ĵ"          -> "j",
      "ķ"          -> "k",
      "ĺļľŀł"      -> "l",
      "ñńņňŋ"      -> "n",
      "òóôõöøōŏőǿ" -> "o",
      "ŕŗř"        -> "r",
      "śŝşš"       -> "s",
      "ţťŧ"        -> "t",
      "ùúûüũūŭůűų" -> "u",
      "ŵẁẃẅ"       -> "w",
      "ỳýŷÿ"       -> "y",
      "źżž"        -> "z",
      "ÀÁÂÃÄÅĀĂĄǺ" -> "A",
      "ÇĆĈĊČ"      -> "C",
      "ÞĎÐ"        -> "D",
      "ÈÉÊËĒĔĖĘĚ"  -> "E",
      "ĜĞĠĢ"       -> "G",
      "ĤĦ"         -> "H",
      "ÌÍÎÏĨĪĬĮİ"  -> "I",
      "Ĵ"          -> "J",
      "Ķ"          -> "K",
      "ĹĻĽĿŁ"      -> "L",
      "ÑŃŅŇŊ"      -> "N",
      "ÒÓÔÕÖØŌŎŐǾ" -> "O",
      "ŔŖŘ"        -> "R",
      "ŚŜŞŠ"       -> "S",
      "ŢŤŦ"        -> "T",
      "ÙÚÛÜŨŪŬŮŰŲ" -> "U",
      "ŴẀẂẄ"       -> "W",
      "ỲÝŶŸ"       -> "Y",
      "ŹŻŽ"        -> "Z",
      "æǽ"         -> "ae",
      "œ"          -> "oe",
      "ÆǼ"         -> "AE",
      "Œ"          -> "OE"
    ).foreach { case (string, result) =>
      s"correctly parse each character in '$string' to '$result'" when {
        string.foreach { character =>
          s"char is '$character'" in {
            StringNormaliser.normaliseString(character.toString) shouldBe result
          }
        }
      }
    }

    "leave anything matching the company name regex unchanged" in {
      val numbers   = "1234567890"
      val lowerCase = "qwertyuiopasdfghjklzxcvbnm"
      val upperCase = "QWERTYUIOPASDFGHJKLZXCVBNM"
      val other     = " '’‘()[]{}<>!«»\"ʺ˝ˮ?/\\+=%#*&$€£_-@¥.,:;"

      StringNormaliser.normaliseString(numbers)   shouldBe numbers
      StringNormaliser.normaliseString(lowerCase) shouldBe lowerCase
      StringNormaliser.normaliseString(upperCase) shouldBe upperCase
      StringNormaliser.normaliseString(other)     shouldBe other
    }
  }
}
