package actors

import javax.inject.Inject

import actors.RewardActor.{GetSizeOfUnusedCodes, GetUnusedCode}
import akka.actor.Actor
import models.RewardRepository
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global

object RewardActor {

  case class GetUnusedCode(profileId: Long, receiptId: Option[Long])

  case object GetSizeOfUnusedCodes

}

class RewardActor @Inject()(rewardRepository: RewardRepository) extends Actor {
  var rewardCodes: Set[String] = Set.empty

  override def preStart(): Unit = {
    Logger.info("Initializing Reward codes in Reward Actor")

    rewardCodes = rewardCodes ++ getAllUnusedCodes
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    Logger.info(s"Restart reward actor restarted for $message because of exception $reason")

    Logger.info(s"Total unused codes :: ${rewardCodes.size}")
  }

  def receive: Receive = {
    case GetUnusedCode(profileId: Long, receiptId: Option[Long]) =>
      Logger.info(s"Got a request to get reward code for receipt id $receiptId and user profile id $profileId")

      sender ! getToken(profileId: Long, receiptId: Option[Long])

    case GetSizeOfUnusedCodes =>
      val unusedCodeSize = rewardCodes.size
      Logger.info(s"Size of unused codes, $unusedCodeSize")

      sender ! unusedCodeSize
  }

  def getToken(profileId: Long, receiptId: Option[Long]): String = {
    val unusedCode = rewardCodes.head

    rewardRepository.markedCodeUsed(unusedCode, profileId, receiptId) foreach { isUpdated =>
      Logger.info(s"Reward code $unusedCode marked used for user profile id $profileId")
    }

    updateUnusedCodes(unusedCode)

    unusedCode
  }

  def updateUnusedCodes(code: String): Set[String] = {
    rewardCodes = rewardCodes.filterNot(_.equals(code))

    rewardCodes
  }

  def getAllUnusedCodes: Set[String] = {
    val totalUnusedCodes = rewardRepository.getUnusedCode

    Logger.info(s"Total unused codes :: ${totalUnusedCodes.size}")
    totalUnusedCodes
  }
}
