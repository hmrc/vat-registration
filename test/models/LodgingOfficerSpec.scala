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

package models

import java.time.LocalDate

import fixtures.VatRegistrationFixture
import models.api._
import play.api.data.validation.ValidationError
import play.api.libs.json.{Format, JsPath, JsSuccess, Json}

class LodgingOfficerSpec extends JsonFormatValidation with VatRegistrationFixture {

  private def writeAndRead[T](t: T)(implicit fmt: Format[T]) = fmt.reads(Json.toJson(fmt.writes(t)))

  implicit val format = LodgingOfficer.format

  private def buildLodgingOfficerDetails(email: String = "test@t.com", tel: Option[String] = None, mobile: Option[String] = None): LodgingOfficerDetails =
    LodgingOfficerDetails(
      currentAddress = scrsAddress,
      changeOfName = None,
      previousAddress = None,
      contact = VatDigitalContact(
        email = email,
        tel = tel,
        mobile = mobile
      )
    )

  val vatLodgingOfficer = LodgingOfficer(
    currentAddress           = Some(scrsAddress),
    dob                      = LocalDate.of(1990, 1, 1),
    nino                     = "NB686868C",
    role                     = "director",
    name                     = name,
    changeOfName             = Some(changeOfName),
    currentOrPreviousAddress = Some(currentOrPreviousAddress),
    contact                  = Some(contact),
    details                  = None
  )

  "LodgingOfficerDetails model" should {
    "successfully read from valid json" in {
      val json = Json.parse(
        s"""
           |{
           |  "currentAddress": {
           |    "line1": "line1",
           |    "line2": "line2",
           |    "postcode": "XX XX",
           |    "country": "UK"
           |  },
           |  "contact": {
           |    "email": "test@t.com"
           |  }
           |}
         """.stripMargin)

      val expectedResult = LodgingOfficerDetails(
        currentAddress = scrsAddress,
        changeOfName = None,
        previousAddress = None,
        contact = VatDigitalContact(
          email = "test@t.com",
          tel = None,
          mobile = None
        )
      )

      Json.fromJson[LodgingOfficerDetails](json)(LodgingOfficerDetails.format) shouldBe JsSuccess(expectedResult)
    }

    "successfully read from full valid json" in {
      val json = Json.parse(
        s"""
           |{
           |  "currentAddress": {
           |    "line1": "line1",
           |    "line2": "line2",
           |    "postcode": "XX XX",
           |    "country": "UK"
           |  },
           |  "changeOfName": {
           |    "formerName": "Bob Smith",
           |    "dateOfNameChange": "2017-01-01",
           |    "name": {
           |      "first": "Bob Smith"
           |    },
           |    "change": "2017-01-01"
           |  },
           |  "previousAddress": {
           |    "line1": "line11",
           |    "line2": "line22",
           |    "postcode": "YY ZZ",
           |    "country": "UK"
           |  },
           |  "contact": {
           |    "email": "test@t.com"
           |  }
           |}
         """.stripMargin)

      val previousAddress = Address("line11", "line22", None, None, Some("YY ZZ"), Some("UK"))
      val expectedResult = LodgingOfficerDetails(
        currentAddress = scrsAddress,
        changeOfName = Some(formerName),
        previousAddress = Some(previousAddress),
        contact = VatDigitalContact(
          email = "test@t.com",
          tel = None,
          mobile = None
        )
      )

      Json.fromJson[LodgingOfficerDetails](json)(LodgingOfficerDetails.format) shouldBe JsSuccess(expectedResult)
    }

    "fail read from json if currentAddress is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "changeOfName": {
           |    "formerName": "Bob Smith",
           |    "dateOfNameChange": "2017-01-01",
           |    "name": {
           |      "first": "Bob Smith"
           |    },
           |    "change": "2017-01-01"
           |  },
           |  "previousAddress": {
           |    "line1": "line11",
           |    "line2": "line22",
           |    "postcode": "YY ZZ",
           |    "country": "UK"
           |  },
           |  "contact": {
           |    "email": "test@t.com"
           |  }
           |}
         """.stripMargin)

      val result = Json.fromJson[LodgingOfficerDetails](json)(LodgingOfficerDetails.format)
      result shouldHaveErrors (JsPath() \ "currentAddress" -> ValidationError("error.path.missing"))
    }

    "fail read from json if contact is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "currentAddress": {
           |    "line1": "line1",
           |    "line2": "line2",
           |    "postcode": "XX XX",
           |    "country": "UK"
           |  },
           |  "changeOfName": {
           |    "formerName": "Bob Smith",
           |    "dateOfNameChange": "2017-01-01",
           |    "name": {
           |      "first": "Bob Smith"
           |    },
           |    "change": "2017-01-01"
           |  },
           |  "previousAddress": {
           |    "line1": "line11",
           |    "line2": "line22",
           |    "postcode": "YY ZZ",
           |    "country": "UK"
           |  }
           |}
         """.stripMargin)

      val result = Json.fromJson[LodgingOfficerDetails](json)(LodgingOfficerDetails.format)
      result shouldHaveErrors (JsPath() \ "contact" -> ValidationError("error.path.missing"))
    }
  }

  "LodgingOfficer model" should {
    "successfully read from valid json" in {
      val expectedResult = LodgingOfficer(
        currentAddress           = None,
        dob                      = LocalDate.of(1990, 1, 1),
        nino                     = "NB686868C",
        role                     = "director",
        name                     = name,
        changeOfName             = None,
        currentOrPreviousAddress = None,
        contact                  = None,
        details                  = None
      )

      val json = Json.parse(
        s"""
           |{
           |  "dob": "1990-01-01",
           |  "nino": "NB686868C",
           |  "role": "director",
           |  "name": {
           |    "first": "Forename",
           |    "last": "Surname",
           |    "forename": "Forename",
           |    "surname": "Surname",
           |    "title": "Title"
           |  },
           |  "ivPassed": false
           |}
         """.stripMargin)

      Json.fromJson[LodgingOfficer](json)(LodgingOfficer.format) shouldBe JsSuccess(expectedResult)
    }

    "successfully read from full valid json" in {
      val json = Json.parse(
        s"""
           |{
           |  "dob": "1990-01-01",
           |  "nino": "NB686868C",
           |  "role": "director",
           |  "name": {
           |    "first": "Forename",
           |    "last": "Surname",
           |    "forename": "Forename",
           |    "surname": "Surname",
           |    "title": "Title"
           |  },
           |  "details" : {
           |    "currentAddress": {
           |      "line1": "line1",
           |      "line2": "line2",
           |      "postcode": "XX XX",
           |      "country": "UK"
           |    },
           |    "changeOfName": {
           |      "formerName": "Bob Smith",
           |      "dateOfNameChange": "2017-01-01",
           |      "name": {
           |        "first": "Bob Smith"
           |      },
           |      "change": "2017-01-01"
           |    },
           |    "previousAddress": {
           |      "line1": "line11",
           |      "line2": "line22",
           |      "postcode": "YY ZZ",
           |      "country": "UK"
           |    },
           |    "contact": {
           |      "email": "test@t.com"
           |    }
           |  },
           |  "ivPassed": false
           |}
         """.stripMargin)

      val previousAddress = Address("line11", "line22", None, None, Some("YY ZZ"), Some("UK"))
      val details = LodgingOfficerDetails(
        currentAddress = scrsAddress,
        changeOfName = Some(formerName),
        previousAddress = Some(previousAddress),
        contact = VatDigitalContact(
          email = "test@t.com",
          tel = None,
          mobile = None
        )
      )
      val expectedResult = LodgingOfficer(
        currentAddress           = None,
        dob                      = LocalDate.of(1990, 1, 1),
        nino                     = "NB686868C",
        role                     = "director",
        name                     = name,
        changeOfName             = None,
        currentOrPreviousAddress = None,
        contact                  = None,
        details                  = Some(details)
      )

      Json.fromJson[LodgingOfficer](json)(LodgingOfficer.format) shouldBe JsSuccess(expectedResult)
    }

    "fail read from json if dob is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "nino": "NB686868C",
           |  "role": "director",
           |  "name": {
           |    "first": "Forename",
           |    "last": "Surname",
           |    "forename": "Forename",
           |    "surname": "Surname",
           |    "title": "Title"
           |  },
           |  "ivPassed": false
           |}
         """.stripMargin)

      val result = Json.fromJson[LodgingOfficer](json)(LodgingOfficer.format)
      result shouldHaveErrors (JsPath() \ "dob" -> ValidationError("error.path.missing"))
    }

    "fail read from json if nino is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "dob": "1990-01-01",
           |  "role": "director",
           |  "name": {
           |    "first": "Forename",
           |    "last": "Surname",
           |    "forename": "Forename",
           |    "surname": "Surname",
           |    "title": "Title"
           |  },
           |  "ivPassed": false
           |}
         """.stripMargin)

      val result = Json.fromJson[LodgingOfficer](json)(LodgingOfficer.format)
      result shouldHaveErrors (JsPath() \ "nino" -> ValidationError("error.path.missing"))
    }

    "fail read from json if role is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "dob": "1990-01-01",
           |  "nino": "NB686868C",
           |  "name": {
           |    "first": "Forename",
           |    "last": "Surname",
           |    "forename": "Forename",
           |    "surname": "Surname",
           |    "title": "Title"
           |  },
           |  "ivPassed": false
           |}
         """.stripMargin)

      val result = Json.fromJson[LodgingOfficer](json)(LodgingOfficer.format)
      result shouldHaveErrors (JsPath() \ "role" -> ValidationError("error.path.missing"))
    }

    "fail read from json if name is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "dob": "1990-01-01",
           |  "nino": "NB686868C",
           |  "role": "director",
           |  "ivPassed": false
           |}
         """.stripMargin)

      val result = Json.fromJson[LodgingOfficer](json)(LodgingOfficer.format)
      result shouldHaveErrors (JsPath() \ "name" -> ValidationError("error.path.missing"))
    }

    "fail read from json if ivPassed is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "dob": "1990-01-01",
           |  "nino": "NB686868C",
           |  "role": "director",
           |  "name": {
           |    "first": "Forename",
           |    "last": "Surname",
           |    "forename": "Forename",
           |    "surname": "Surname",
           |    "title": "Title"
           |  }
           |}
         """.stripMargin)

      val result = Json.fromJson[LodgingOfficer](json)(LodgingOfficer.format)
      result shouldHaveErrors (JsPath() \ "ivPassed" -> ValidationError("error.path.missing"))
    }
  }

  "Creating a Json from a valid LodgingOfficer model" should {
    "complete successfully" in {
      writeAndRead(vatLodgingOfficer) resultsIn vatLodgingOfficer
    }
  }

  "Creating a Json from an invalid LodgingOfficer model" should {
    "fail with a ValidationError" when {
      "NINO is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(nino = "NB888")
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "nino" -> ValidationError("error.pattern"))
      }

      "Role is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(role = "magician")
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "role" -> ValidationError("error.pattern"))
      }

      "Name is invalid" in {
        val name = Name(first = "$%@$%^@#%@$^@$^$%@#$%@#$", middle = None, last = None, forename = Some("$%@$%^@#%@$^@$^$%@#$%@#$"))
        val lodgingOfficer = vatLodgingOfficer.copy(name = name)
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "name" \ "forename" -> ValidationError("error.pattern"))
      }

      "Contact email is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(contact = Some(OfficerContactDetails(Some("£$%^&&*"), None, None)))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "contact" \ "email" -> ValidationError("error.pattern"))
      }

      "Contact tel is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(contact = Some(OfficerContactDetails(None, Some("£$%^&&*"), None)))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "contact" \ "tel" -> ValidationError("error.pattern"))
      }

      "Contact mob is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(contact = Some(OfficerContactDetails(None, None, Some("£$%^&&*"))))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "contact" \ "mobile" -> ValidationError("error.pattern"))
      }

      "LodgingOfficerDetails Contact email is too long" in {
        val emailTooLong = "eeeeeeeeeeeeemmmmmmmmaaaaaaaaaillllllll@ttttttttttt.gggg.vvv.uuu.tellll"
        val lodgingOfficer = vatLodgingOfficer.copy(details = Some(buildLodgingOfficerDetails(emailTooLong)))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "details" \ "contact" \ "email" -> ValidationError("error.pattern"))
      }

      "LodgingOfficerDetails Contact email is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(details = Some(buildLodgingOfficerDetails("£$%^&&*")))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "details" \ "contact" \ "email" -> ValidationError("error.pattern"))
      }

      "LodgingOfficerDetails Contact tel is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(details = Some(buildLodgingOfficerDetails(tel = Some("£$%^&&*"))))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "details" \ "contact" \ "tel" -> ValidationError("error.pattern"))
      }

      "LodgingOfficerDetails Contact mob is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(details = Some(buildLodgingOfficerDetails(mobile = Some("£$%^&&*"))))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "details" \ "contact" \ "mobile" -> ValidationError("error.pattern"))
      }
    }
  }
}
