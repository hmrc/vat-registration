
package itutil

import utils.IdGenerator

import javax.inject.Singleton

@Singleton
class FakeIdGenerator extends IdGenerator with ITFixtures {
  override def createId: String = testCorrelationid
}


