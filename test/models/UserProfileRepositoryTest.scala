package models

import org.joda.time.DateTime
import org.specs2.mutable.Specification

class UserProfileRepositoryTest extends Specification {
  val userProfileRepo = new ModelsTest[UserProfileRepository]

  val dob = "%s-%s-%s" format(1991, 9, 8)

  private val userProfile = Profile(2, "firstName", "lastName", "test@example.com", DateTime.parse(dob), "address1",
    None, "province",  "V1V1 V1", "1245678903", new DateTime(), suspended = false)

  "UserProfile Repository" should {

    "store user profile" in {
      val storeResult = userProfileRepo.result(userProfileRepo.repository.store(userProfile))

      storeResult must equalTo(Some(2))
    }

    "find profile by email address" in {
      val optionalProfile = userProfileRepo.result(userProfileRepo.repository.findByEmail("test@example.com"))

      optionalProfile must equalTo(Some(userProfile.copy(created = optionalProfile.get.created)))
    }
  }
}
