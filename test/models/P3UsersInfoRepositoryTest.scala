package models

import org.joda.time.DateTime
import org.specs2.mutable.Specification

class P3UsersInfoRepositoryTest extends Specification {
  private val p3UserInfoRepository = new ModelsTest[P3UsersInfoRepository]

  private val dateTime = new DateTime(1470633591)
  private val p3UserInfo = P3UserInfo(1, "p3UserInfo", "test@examaple.com", dateTime)

  "P3UserInfo Repository" should {

    "store p3 user information" in {
      val storeResult = p3UserInfoRepository.result(p3UserInfoRepository.repository.store(p3UserInfo))

      storeResult must equalTo(true)
    }

    "fetch user by email address" in {
      val user = p3UserInfoRepository.result(p3UserInfoRepository.repository.fetchByEmail("test@examaple.com"))

      user.get.email must contain("test@examaple.com")
    }
  }
}
