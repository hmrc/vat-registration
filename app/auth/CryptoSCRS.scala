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

package auth

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.{JsString, Reads, Writes}
import uk.gov.hmrc.crypto.{ApplicationCrypto, CompositeSymmetricCrypto, Crypted, PlainText}

@Singleton
class CryptoSCRS @Inject()(config: Configuration) {

  lazy val crypto: CompositeSymmetricCrypto = new ApplicationCrypto(config.underlying).JsonCrypto

  val rds: Reads[String] = Reads[String](js =>
    js.validate[String].map { encryptedUtr =>
      crypto.decrypt(Crypted.fromBase64(encryptedUtr)).value
    }
  )

  val wts: Writes[String] = Writes[String] { utr =>
    val encryptedUtr = new String(crypto.encrypt(PlainText(utr)).toBase64)
    JsString(encryptedUtr)
  }
}
