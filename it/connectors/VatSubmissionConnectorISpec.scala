
package connectors

import itutil.IntegrationStubbing
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.InternalServerException

class VatSubmissionConnectorISpec extends IntegrationStubbing {

  val connector = app.injector.instanceOf[VatSubmissionConnector]
  val testCorrelationId = "testCorrelationId"

  "submit" should {
    "return successfully when the subscription API responds with OK" in new SetupHelper {
      given
        .user.isAuthorised
        .subscriptionApi.respondsWith(OK)

      val res = await(connector.submit(Json.obj(), testCorrelationId, testInternalid))

      res.status mustBe OK
    }
    "throw InternalServerException when the subscription API response contains the INVALID_PAYLOAD code" in new SetupHelper {
      given.user.isAuthorised
        .subscriptionApi.respondsWithJson(BAD_REQUEST, Json.obj("code" -> "INVALID_PAYLOAD"))

      intercept[InternalServerException] {
        await(connector.submit(Json.obj(), testCorrelationId, testInternalid))
      }
    }
    "throw InternalServerException when the subscription API response contains the INVALID_SESSIONID code" in new SetupHelper {
      given.user.isAuthorised
        .subscriptionApi.respondsWithJson(BAD_REQUEST, Json.obj("code" -> "INVALID_SESSIONID"))

      intercept[InternalServerException] {
        await(connector.submit(Json.obj(), testCorrelationId, testInternalid))
      }
    }
    "throw InternalServerException when the subscription API response contains the INVALID_CREDENTIALID code" in new SetupHelper {
      given.user.isAuthorised
        .subscriptionApi.respondsWithJson(BAD_REQUEST, Json.obj("code" -> "INVALID_CREDENTIALID"))

      intercept[InternalServerException] {
        await(connector.submit(Json.obj(), testCorrelationId, testInternalid))
      }
    }
    "throw InternalServerException when the subscription API returns CONFLICT" in new SetupHelper {
      given.user.isAuthorised
        .subscriptionApi.respondsWith(CONFLICT)

      intercept[InternalServerException] {
        await(connector.submit(Json.obj(), testCorrelationId, testInternalid))
      }
    }
    "throw InternalServerException when the subscription API returns any other status" in new SetupHelper {
      given.user.isAuthorised
        .subscriptionApi.respondsWith(IM_A_TEAPOT)

      intercept[InternalServerException] {
        await(connector.submit(Json.obj(), testCorrelationId, testInternalid))
      }
    }
  }

}
