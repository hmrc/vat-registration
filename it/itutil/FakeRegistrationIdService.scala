
package itutil

import services.RegistrationIdService

class FakeRegistrationIdService extends RegistrationIdService {

  override def generateRegistrationId(): String = FakeRegistrationIdService.id

}

object FakeRegistrationIdService extends ITVatSubmissionFixture {

  var id: String = testRegId

}
