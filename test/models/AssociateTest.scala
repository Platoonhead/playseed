package models

import org.specs2.mutable.Specification

class AssociateTest extends Specification {

  val associateRepository = new ModelsTest[AssociateRepository]
  val associateDetail = Associate(1, 1, "p3UserId", None, None, Some("userpass"))

  "Associate Repository" should {

    "store associate detail of a user" in {
      val storeResult = associateRepository.result(associateRepository.repository.store(associateDetail))

      storeResult must equalTo(true)
    }
  }
}
