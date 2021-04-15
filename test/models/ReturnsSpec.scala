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

package models

import helpers.BaseSpec
import models.api.{Returns, StartDate, TurnoverEstimates}
import play.api.libs.json._
import utils.EligibilityDataJsonUtils

import java.time.LocalDate

class ReturnsSpec extends BaseSpec with JsonFormatValidation {

  val dateValue: LocalDate = LocalDate.of(2017, 1, 1)
  val date: StartDate = StartDate(Some(dateValue))

  val fullJson: JsObject = Json.parse(
    s"""
       |{
       |  "reclaimVatOnMostReturns" : true,
       |  "frequency" : "quarterly",
       |  "staggerStart" : "jan",
       |  "start" : {
       |    "date" : "$dateValue"
       |  },
       |  "zeroRatedSupplies": 12.99
       |}
        """.stripMargin).as[JsObject]

  val invalidValidationJson: JsObject = Json.parse(
    s"""
       |{
       |  "reclaimVatOnMostReturns" : true,
       |  "frequency" : "whatever",
       |  "staggerStart" : "month",
       |  "start" : {
       |    "date" : "$dateValue"
       |  }
       |}
        """.stripMargin).as[JsObject]

  val invalidStaggerStartReturns: Returns = Returns(
    reclaimVatOnMostReturns = true,
    "quarterly",
    Some("month"),
    date,
    None
  )

  val invalidFrequencyReturns: Returns = Returns(
    reclaimVatOnMostReturns = true,
    "whatever",
    Some("jan"),
    date,
    None
  )

  val fullReturns: Returns = Returns(
    reclaimVatOnMostReturns = true,
    "quarterly",
    Some("jan"),
    date,
    Some(12.99)
  )

  val missingOptionsReturns: Returns = Returns(
    reclaimVatOnMostReturns = true,
    "quarterly",
    None,
    date,
    Some(12.99)
  )

  val invalidValidationReturns: Returns = Returns(
    reclaimVatOnMostReturns = true,
    "whatever",
    Some("month"),
    date,
    None
  )

  val fullSubmissionJson = Json.obj(
    "subscription" -> Json.obj(
      "reasonForSubscription" -> Json.obj(
        "voluntaryOrEarlierDate" -> "2017-01-01"
      ),
      "yourTurnover" -> Json.obj(
        "VATRepaymentExpected" -> true,
        "zeroRatedSupplies" -> 12.99
      )
    ),
    "periods" -> Json.obj(
      "customerPreferredPeriodicity" -> "MA"
    )
  )

  "Parsing Returns" should {
    "succeed" when {
      "full json is present" in {
        Json.fromJson[Returns](fullJson) mustBe JsSuccess(fullReturns)
      }
      "frequency and staggerStart are present but invalid" in {
        Json.fromJson[Returns](invalidValidationJson) mustBe JsSuccess(invalidValidationReturns)
      }
      "staggeredStart is missing" in {
        val json = Json.parse(
          s"""
             |{
             |  "reclaimVatOnMostReturns" : true,
             |  "frequency" : "quarterly",
             |  "start" : {
             |    "date" : "$dateValue"
             |  },
             |  "zeroRatedSupplies": 12.99
             |}
        """.stripMargin)
        Json.fromJson[Returns](json) mustBe JsSuccess(fullReturns.copy(staggerStart = None))
      }
    }
    "fails" when {
      "reclaimVatOnMostReturns is missing" in {
        val json = Json.parse(
          s"""
             |{
             |  "frequency" : "quarterly",
             |  "staggerStart" : "jan",
             |  "start" : {
             |    "date" : "$dateValue"
             |  }
             |}
        """.stripMargin)
        val result = Json.fromJson[Returns](json)
        result shouldHaveErrors (__ \ "reclaimVatOnMostReturns" -> JsonValidationError("error.path.missing"))
      }

      "frequency is missing" in {
        val json = Json.parse(
          s"""
             |{
             |  "reclaimVatOnMostReturns" : true,
             |  "staggerStart" : "jan",
             |  "start" : {
             |    "date" : "$dateValue"
             |  }
             |}
        """.stripMargin)
        val result = Json.fromJson[Returns](json)
        result shouldHaveErrors (__ \ "frequency" -> JsonValidationError("error.path.missing"))
      }

      "start is missing" in {
        val json = Json.parse(
          s"""
             |{
             |  "reclaimVatOnMostReturns" : true,
             |  "frequency" : "quarterly",
             |  "staggerStart" : "jan"
             |}
        """.stripMargin)
        val result = Json.fromJson[Returns](json)
        result shouldHaveErrors (__ \ "start" -> JsonValidationError("error.path.missing"))
      }
    }
  }

  "Returns model to json" should {
    "succeed" when {
      "everything is present" in {
        Json.toJson[Returns](fullReturns) mustBe fullJson
      }
      "staggerStart is missing" in {
        Json.toJson[Returns](missingOptionsReturns) mustBe (fullJson - "staggerStart")
      }
    }
  }

  "TurnoverEstimates mongoReads" must {
    "return model successfully when turnoverEstimate-value exists" in {
      val json = Json.parse(
        s"""{
           |  "sections": [
           |   {
           |     "title": "Foo bar",
           |     "data": [
           |       {"questionId":"turnoverEstimate-value","question": "VAT start date", "answer": "£123456" , "answerValue": 123456}
           |     ]
           |   }
           | ]
           | }""".stripMargin)
      val expected = TurnoverEstimates(turnoverEstimate = 123456)

      val result = Json.fromJson[TurnoverEstimates](EligibilityDataJsonUtils.toJsObject(json))(TurnoverEstimates.eligibilityDataJsonReads)
      result mustBe JsSuccess(expected)
    }

    "return empty model successfully" in {
      val json = Json.parse(
        s"""
           |[
           |   {
           |     "title": "Foo bar",
           |     "data": [
           |       {"questionId":"wrongId","question": "VAT start date", "answer": "The date the company is registered with Companies House" , "answerValue": 10000}
           |     ]
           |   },
           |   {
           |     "title": "Director details",
           |     "data": [
           |       {"questionId":"fooDirectorDetails2","question": "Former name", "answer": "Dan Swales", "answerValue": true},
           |       {"questionId":"fooDirectorDetails3","question": "Date of birth", "answer": "1 January 2000", "answerValue": true}
           |     ]
           |   }
           |]
        """.stripMargin)

      val result = Json.fromJson[TurnoverEstimates](EligibilityDataJsonUtils.toJsObject(json))(TurnoverEstimates.eligibilityDataJsonReads)
      result.isError mustBe true
    }
  }

  "readStaggerStart" should {
    "convert the stagger to the correct values" in {
      val values = List(
        Some("MM"),
        Some("MA"),
        Some("MB"),
        Some("MC"),
        None
      )

      val result = values.map(Returns.readStaggerStart)

      result mustBe List(
        None,
        Some("jan"),
        Some("feb"),
        Some("mar"),
        None
      )
    }
  }

  "readPeriod" should {
    "convert the periods to the correct values" in {
      val values = List(
        "MM",
        "MA",
        "MB",
        "MC"
      )

      val result = values.map(Returns.readPeriod)

      result mustBe List(
        "monthly",
        "quarterly",
        "quarterly",
        "quarterly"
      )
    }
  }

  "writePeriod" should {
    "convert the periods to the correct values" in {
      val values = List(
        ("monthly", Some("invalid")),
        ("monthly", Some("jan")),
        ("monthly", None),
        ("quarterly", Some("jan")),
        ("quarterly", Some("feb")),
        ("quarterly", Some("mar")),
        ("quarterly", Some("apr")),
        ("quarterly", None),
        ("invalid", Some("invalid"))
      )

      val result = values.map {
        case (freq, period) => Returns.writePeriod(freq, period)
      }

      result mustBe List(
        Some("MM"),
        Some("MM"),
        Some("MM"),
        Some("MA"),
        Some("MB"),
        Some("MC"),
        None,
        None,
        None
      )
    }
  }
}