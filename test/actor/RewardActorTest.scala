package actor

import java.util.UUID

import actors.RewardActor
import akka.actor.{ActorRef, ActorSystem, Props}
import models.RewardRepository
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar.mock
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.scalatest.{BeforeAndAfterAll, MustMatchers, WordSpecLike}

import scala.concurrent.Future

class RewardActorTest(_system: ActorSystem) extends TestKit(_system: ActorSystem) with WordSpecLike with
  MustMatchers with BeforeAndAfterAll with ImplicitSender {

  def this() = this(ActorSystem("RewardTestSystem"))

  override def afterAll() {
    TestKit.shutdownActorSystem(system)
  }

  "get reward code" in {
    val codes = Set("code1", "code2", "code3", "code4")

    val (rewardInstance, mockedRewardRepository, _) = rewardActorTestObjects(codes)

    when(mockedRewardRepository.markedCodeUsed("code1", 1, Some(1))).thenReturn(Future.successful(true))

    val code = rewardInstance.getToken(1, Some(1))

    assert(code == "code1")
  }

  "update unused token" in {
    val codes = Set("code1", "code2", "code3", "code4")

    val (rewardInstance, _, _) = rewardActorTestObjects(codes)

    rewardInstance.updateUnusedCodes("code1")

    assert(!rewardInstance.rewardCodes.contains("code1"))

  }

  "get unused code from database" in {
    val codes = Set("code1", "code2", "code3", "code4")

    val (rewardInstance, _, _) = rewardActorTestObjects(codes)

    val unusedCodes = rewardInstance.getAllUnusedCodes

    assert(unusedCodes == codes)
  }


  private def rewardActorTestObjects(codes: Set[String]): (RewardActor, RewardRepository, ActorRef) = {
    val codes = Set("code1", "code2", "code3", "code4")

    val mockedRewardRepository = mock[RewardRepository]

    when(mockedRewardRepository.getUnusedCode).thenReturn(codes)

    val rewardActor = system.actorOf(Props(classOf[RewardActor], mockedRewardRepository), s"test-reward-actor-${UUID.randomUUID}")

    val rewardInstance = TestActorRef(new RewardActor(mockedRewardRepository)).underlyingActor

    (rewardInstance, mockedRewardRepository, rewardActor)
  }
}
