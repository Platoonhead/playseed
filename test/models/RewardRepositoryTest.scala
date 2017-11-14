package models

import org.specs2.mutable.Specification

class RewardRepositoryTest extends Specification {

  val rewardRepo = new ModelsTest[RewardRepository]
  val reward = RewardCode(1, "code1233", None, None, false, None)

  "Token Repository" should {

    "marked code used " in {
      val storeResult = rewardRepo.result(rewardRepo.repository.markedCodeUsed("code1233", 1, Some(1)))

      storeResult must equalTo(true)
    }

    "check all rewards have used" in {
      val storeResult = rewardRepo.result(rewardRepo.repository.checkUsedRewards)

      storeResult must equalTo(true)
    }
  }
}
