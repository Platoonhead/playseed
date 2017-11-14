package models

import org.joda.time.DateTime
import org.specs2.mutable.Specification

class SupportRepositoryTest extends Specification {

  private val supportRepository = new ModelsTest[SupportRepository]

  "Support Repository" should {

    "store a support user" in {
      val supportForm = Support(0, "test", "test@example.com", "message", new DateTime(0L))
      val storeResult = supportRepository.result(supportRepository.repository.store(supportForm))

      storeResult must equalTo(1)
    }
  }
}
