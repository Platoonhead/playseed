package models

import org.joda.time.DateTime
import org.specs2.mutable.Specification

class BlockListRepositoryTest extends Specification {

  private val blockRepository = new ModelsTest[BlockListRepository]

  "BlockList Repository" should {

    "store a block user" in {
      val blockUser = BlockList(0, "test@example.com", new DateTime(System.currentTimeMillis))
      val storeResult = blockRepository.result(blockRepository.repository.store(blockUser))

      storeResult must equalTo(1)
    }

    "check blocked user by email" in {
      val statusOfBlockedUser = blockRepository.result(blockRepository.repository.shouldEnter("test@example.com"))

      statusOfBlockedUser must equalTo(false)
    }

    "check blocked user if user email is not blocked." in {
      val statusOfBlockedUser = blockRepository.result(blockRepository.repository.shouldEnter("test@test.com"))

      statusOfBlockedUser must equalTo(true)
    }
  }
}

